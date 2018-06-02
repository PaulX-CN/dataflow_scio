import sbt._
import Keys._

val scioVersion = "0.5.5"
val beamVersion = "2.4.0"
val kafkaVersion = "1.1.0"
val scalaMacrosVersion = "2.1.1"
val protobufVersion = "3.5.1"

lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  // Semantic versioning http://semver.org/
  version               := "0.1.0-SNAPSHOT",
  scalaVersion          := "2.11.12",
  scalacOptions         ++= Seq("-target:jvm-1.8",
                                "-deprecation",
                                "-feature",
                                "-unchecked"),
  javacOptions          ++= Seq("-source", "1.8",
                                "-target", "1.8")
)

lazy val paradiseDependency =
  "org.scalamacros" % "paradise" % scalaMacrosVersion cross CrossVersion.full
lazy val macroSettings = Seq(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  addCompilerPlugin(paradiseDependency)
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root: Project = Project(
  "dataflow_scio",
  file(".")
).settings(
  commonSettings ++ macroSettings ++ noPublishSettings,
  description := "dataflow_scio",
  libraryDependencies ++= Seq(
    "com.spotify" %% "scio-core" % scioVersion,
    "com.spotify" %% "scio-test" % scioVersion % "test",
    "org.apache.beam" % "beam-runners-direct-java" % beamVersion,
    "org.apache.beam" % "beam-sdks-java-io-kafka" % beamVersion,
    "org.apache.kafka" % "kafka-clients" % kafkaVersion,
    // optional dataflow runner
     "org.apache.beam" % "beam-runners-google-cloud-dataflow-java" % beamVersion,
    "org.slf4j" % "slf4j-simple" % "1.7.25",
    "com.google.protobuf" % "protobuf-java" % protobufVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
  )
).enablePlugins(PackPlugin)

lazy val repl: Project = Project(
  "repl",
  file(".repl")
).settings(
  commonSettings ++ macroSettings ++ noPublishSettings,
  description := "Scio REPL for dataflow_scio",
  libraryDependencies ++= Seq(
    "com.spotify" %% "scio-repl" % scioVersion
  ),
  mainClass in Compile := Some("com.spotify.scio.repl.ScioShell")
).dependsOn(
  root
)




