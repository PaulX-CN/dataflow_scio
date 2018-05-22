package org.paul

import com.google.api.services.bigquery.model.TableRow
import com.spotify.scio._
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values._
import org.paul.protos.person.Person
import org.apache.beam.sdk.transforms.windowing._
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.io.gcp.bigquery._
import com.spotify.scio.bigquery.BigQueryUtil
import com.spotify.scio.values._
import org.apache.beam.sdk.{Pipeline, PipelineResult}
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.joda.time.Duration

object BigQueryDump {

  // A Beam native source `PTransform` where the input type is `PBegin`
  def pubsubIn(topic: String): PTransform[PBegin, PCollection[Person]] =
    PubsubIO.readProtos(classOf[Person]).fromTopic(topic)

  // A Beam native windowing `PTransform`
  val window: PTransform[PCollection[Person], PCollection[Person]] =
    Window
      .into[Person](FixedWindows.of(Duration.standardSeconds(60)))
      .triggering(
        AfterWatermark
          .pastEndOfWindow()
          .withEarlyFirings(
            AfterProcessingTime
              .pastFirstElementInPane()
              .plusDelayOf(Duration.standardSeconds(5)))
          .withLateFirings(
            AfterProcessingTime
              .pastFirstElementInPane()
              .plusDelayOf(Duration.standardSeconds(10))))
      .accumulatingFiredPanes()

  def bigqueryOut(tableName: String): PTransform[PCollection[TableRow], PDone] =
    BigQueryIO.writeTableRows().to(tableName).asInstanceOf[PTransform[PCollection[TableRow], PDone]]


  def main(cmdlineArgs: Array[String]): Unit = {
    // Parse command line arguments and create Beam specific options plus application specific
    // arguments
    //
    // - opts: `PipelineOptions` or its subtype - Beam pipeline options, where field names and types
    // are defined as setters and getters in the Java interface
    // - args: `Args` - application specific arguments, anything not covered by `opts` ends up here
    val (opts, args) = ScioContext.parseArguments[PipelineOptions](cmdlineArgs)

    // Create a new `ScioContext` with the given `PipelineOptions`
    val sc = ScioContext(opts)

    // Underlying Beam `Pipeline`
    val pipeline: Pipeline = sc.pipeline

    // Custom input with a Beam source `PTransform`
    val accounts: SCollection[Person] = sc.customInput("Input", pubsubIn(args("inputTopic")))

    // Underlying Beam `PCollection`
    val p: PCollection[Person] = accounts.internal

    accounts
      // Beam `PTransform`
      .applyTransform(window)
      // Scio `map` transform
      .map(a => KV.of(a.name, a.id))
      // Scio `map` transform
      .map(kv => kv.getKey + "_" + kv.getValue)
      // Custom output with a Beam sink `PTransform`
      .saveAsCustomOutput("Output", bigqueryOut(args("outputTopic")))

    // This calls sc.pipeline.run() under the hood
    val result = sc.close()

    // Underlying Beam pipeline result
    val pipelineResult: PipelineResult = result.internal
  }
}

