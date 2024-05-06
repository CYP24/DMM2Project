ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "Project"
  )
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.8.2",
  "io.circe" %% "circe-core" % "0.15.0-M1",
  "io.circe" %% "circe-parser" % "0.15.0-M1",
  "io.circe" %% "circe-generic" % "0.15.0-M1"
)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % "3.6.11",
  "org.json4s" %% "json4s-jackson" % "3.6.11"
)
