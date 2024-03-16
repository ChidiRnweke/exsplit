ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    name := "Exsplit",
    javacOptions ++= Seq("-source", "17", "-target", "17"),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.26",
      "com.github.jwt-scala" %% "jwt-upickle" % "10.0.0",
      "com.outr" %% "scalapass" % "1.2.8",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.6",
      "org.flywaydb" % "flyway-database-postgresql" % "10.0.0",
      "com.github.geirolz" %% "fly4s" % "1.0.0",
      "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.8" % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.8" % Test,
      "org.postgresql" % "postgresql" % "42.5.1",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test
    ),
    Compile / run / fork := true,
    Compile / run / connectInput := true,
    Test / fork := true
  )
