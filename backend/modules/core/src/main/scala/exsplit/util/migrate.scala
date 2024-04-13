package exsplit.migration

import fly4s.*
import fly4s.data.*
import fly4s.implicits.*
import cats._
import cats.effect._
import cats.syntax.all._
import cats.data._
import exsplit.config.MigrationsConfig

/** Migrates the database using Fly4s library. This function should be called
  * when the application starts to ensure that the database is up-to-date.
  *
  * @param dbConfig
  *   The configuration for the database migrations.
  * @return
  *   An IO that represents the result of the migration.
  */
def migrateDb(dbConfig: MigrationsConfig): IO[MigrateResult] =
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
