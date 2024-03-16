ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    name := "Exsplit",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.26",
      "com.github.jwt-scala" %% "jwt-upickle" % "10.0.0",
      "com.outr" %% "scalapass" % "1.2.8",
      "com.github.pureconfig" %% "pureconfig" % "0.17.6",
      "com.github.geirolz" %% "fly4s" % "1.0.0"
    ),
    Compile / run / fork := true,
    Compile / run / connectInput := true
  )
