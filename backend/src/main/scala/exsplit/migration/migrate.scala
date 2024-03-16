package exsplit.migration

import fly4s.*
import fly4s.data.*
import fly4s.implicits.*
import cats._
import cats.effect._
import cats.syntax.all._
import cats.data._
import exsplit.config.DatabaseConfig

def migrateDb(dbConfig: DatabaseConfig): IO[MigrateResult] =
  Fly4s
    .make[IO](
      url = dbConfig.url,
      user = Some(dbConfig.user),
      password = Some(dbConfig.password.toCharArray()),
      config = Fly4sConfig(
        table = dbConfig.migrationsTable,
        locations = Locations(dbConfig.migrationsLocations)
      )
    )
    .use(_.migrate)
