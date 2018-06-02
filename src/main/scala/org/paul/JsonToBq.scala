package org.paul

import com.google.api.services.bigquery.model.{TableFieldSchema, TableSchema}
import com.spotify.scio.ContextAndArgs
import com.spotify.scio.bigquery._
import com.typesafe.scalalogging.Logger
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values.{PCollection, PDone}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object JsonToBq {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def bigqueryOut(tableName: String): PTransform[PCollection[TableRow], PDone] =
    BigQueryIO.writeTableRows()
    .withCreateDisposition(CREATE_IF_NEEDED)
    .withWriteDisposition(WRITE_APPEND)
    .withoutValidation()
    .to(tableName)
    .asInstanceOf[PTransform[PCollection[TableRow], PDone]]

  def main(cmdlineArgs: Array[String]): Unit = {

    val (sc, args) = ContextAndArgs(cmdlineArgs)

    val schema: TableSchema = new TableSchema().setFields(List(
      new TableFieldSchema().setName("status").setType("RECORD"),
      new TableFieldSchema().setName("status.score").setType("INTEGER"),
      new TableFieldSchema().setName("status.ts").setType("TIMESTAMP"),
      new TableFieldSchema().setName("status.context").setType("STRING"),
      new TableFieldSchema().setName("gender").setType("STRING"),
      new TableFieldSchema().setName("id").setType("STRING"),
      new TableFieldSchema().setName("name").setType("STRING")
    ).asJava)

    val writeMode: WriteDisposition = {
      if (args("writeMode") == "overwrite") WRITE_TRUNCATE
      else if (args("writeMode") == "append") WRITE_APPEND
      else WRITE_EMPTY
    }

    val bucketName = args("bucketName")
    val fileName = args("fileName")
    val uri = s"gs://$bucketName/$fileName"

    // Open text files a `SCollection[TableRow]`
    sc.tableRowJsonFile(args.getOrElse("input", uri))
      .setName("testjson")
      .saveAsBigQuery(args("output"), schema, writeMode, CREATE_IF_NEEDED)

  }
}
