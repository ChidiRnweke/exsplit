ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "be.chidinweke"
ThisBuild / organizationName := "chidi nweke"

lazy val root = (project in file("."))
  .aggregate(core, shared)

lazy val core = project
  .in(file("modules/core"))
  .dependsOn(shared)
  .settings(
    name := "Exsplit",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.26",
      "com.github.jwt-scala" %% "jwt-upickle" % "10.0.0",
      "org.springframework.security" % "spring-security-crypto" % "6.2.3",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.6",
      "org.flywaydb" % "flyway-database-postgresql" % "10.10.0",
      "commons-logging" % "commons-logging" % "1.2",
      "com.github.geirolz" %% "fly4s" % "1.0.0",
      "org.postgresql" % "postgresql" % "42.7.2",
      "io.chrisdavenport" %% "fiberlocal" % "0.1.2",
      "org.tpolecat" %% "skunk-core" % "0.6.3",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test
    ),
    Compile / run / fork := true,
    Compile / run / connectInput := true,
    Test / fork := true,
    assembly / mainClass := Some("exsplit.Main"),
    assembly / assemblyJarName := "exsplit.jar"
  )

lazy val shared = project
  .in(file("modules/shared"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value
    ),
    assembly / assemblyJarName := "shared.jar"
  )

lazy val integration = project
  .in(file("modules/integration"))
  .dependsOn(core)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.github.geirolz" %% "fly4s" % "1.0.0",
      "org.flywaydb" % "flyway-database-postgresql" % "10.10.0",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.6",
      "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.8" % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.8" % Test,
      "org.postgresql" % "postgresql" % "42.7.2",
      "org.tpolecat" %% "skunk-core" % "0.6.3",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test
    ),
    Compile / run / fork := true,
    Compile / run / connectInput := true,
    Test / fork := true
  )

ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "application.conf"            => MergeStrategy.concat
  case _                             => MergeStrategy.first
}
