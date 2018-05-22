import sbt._
import Keys._

val scioVersion = "0.5.4"
val beamVersion = "2.4.0"
val scalaMacrosVersion = "2.1.1"

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
    // optional dataflow runner
    // "org.apache.beam" % "beam-runners-google-cloud-dataflow-java" % beamVersion,
    "org.slf4j" % "slf4j-simple" % "1.7.25"
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

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// (optional) If you need scalapb/scalapb.proto or anything from
// google/protobuf/*.proto
libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" %
  scalapb.compiler.Version.scalapbVersion % "protobuf"
