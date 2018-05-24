package org.paul

import java.text.SimpleDateFormat
import com.typesafe.scalalogging._

import org.slf4j.LoggerFactory
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values._
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.{Pipeline, PipelineResult}
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import com.spotify.scio._
import com.spotify.scio.bigquery.TableRow
import com.spotify.scio.values._
import com.spotify.scio.bigquery.CREATE_NEVER
import com.spotify.scio.bigquery.WRITE_APPEND
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.paul.protos.PersonOuterClass.Person


object BigQueryDump {


  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  // A Beam native source `PTransform` where the input type is `PBegin`
  def pubsubIn(topic: String): PTransform[PBegin, PCollection[Person]] =
    PubsubIO.readProtos(classOf[Person]).fromTopic(topic)

  def kafkaIn(broker:String, topic: String): PTransform[PBegin, PCollection[KV[Array[Byte], Array[Byte]]]] = {

    KafkaIO.read()
      .withBootstrapServers(broker)
      .withTopic(topic)
      .withKeyDeserializer(classOf[ByteArrayDeserializer])
      .withValueDeserializer(classOf[ByteArrayDeserializer])
      .withReadCommitted()
      .withProcessingTime()
      .withoutMetadata()
  }


  def tsToDate(ts: Long): String = {
    val df:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    df.format(ts)
  }

  def bigqueryOut(tableName: String): PTransform[PCollection[TableRow], PDone] =
    BigQueryIO.writeTableRows()
      .withCreateDisposition(CREATE_NEVER)
      .withWriteDisposition(WRITE_APPEND)
      .withoutValidation()
      .to(tableName)
      .asInstanceOf[PTransform[PCollection[TableRow], PDone]]


  def main(cmdlineArgs: Array[String]): Unit = {
    // Parse command line arguments and create Beam specific options plus application specific
    // arguments

    val (opts, args) = ScioContext.parseArguments[PipelineOptions](cmdlineArgs)

    // Create a new `ScioContext` with the given `PipelineOptions`
    val sc = ScioContext(opts)

    val pipeline: Pipeline = sc.pipeline

//    val persons: SCollection[Person] = sc.customInput("Input", pubsubIn(args("inputTopic")))

    val personKafka: SCollection[KV[Array[Byte], Array[Byte]]] =
      sc.customInput("Input", kafkaIn(args("inputBroker"), args("inputTopic")))

    def convertToTableRow(p: Person): TableRow = {
      val newRow = TableRow()
      newRow.set("id", p.getId)
      newRow.set("gender", p.getGender)
      newRow.set("name", p.getName)
      newRow.set("ts", tsToDate(p.getTs))
    }

    def decodeToPerson(kv: KV[Array[Byte], Array[Byte]]): Person = {
      val bytes = kv.getValue
      val p = Person.parseFrom(bytes)
      logger.info(s"Here is a new person $p.")
      p
    }

    personKafka
        .map(decodeToPerson)
        .map(convertToTableRow)
        .saveAsCustomOutput("Output", bigqueryOut(args("tableName")))

//    persons
//      // Beam `PTransform`
//      .map(convertToTableRow)
//      // Custom output with a Beam sink `PTransform`
//      .saveAsCustomOutput("Output", bigqueryOut(args("tableName")))

    // This calls sc.pipeline.run() under the hood
    val result = sc.close()

    // Underlying Beam pipeline result
    val pipelineResult: PipelineResult = result.internal
  }
}

