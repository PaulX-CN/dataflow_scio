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
}

