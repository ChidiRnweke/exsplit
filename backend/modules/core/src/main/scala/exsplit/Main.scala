package exsplit

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config.*
import exsplit.migration.*
import pureconfig._
import pureconfig.generic.derivation.default._
import skunk.Session
import natchez.Trace.Implicits.noop
import cats.effect.IO.asyncForIO
import exsplit.migration.migrateDb
import io.chrisdavenport.fiberlocal.FiberLocal
import exsplit.routes._
import exsplit.database._

object Main extends IOApp.Simple:
  val appConfig = readConfig[AppConfig]("application.conf")
  val authConfig = appConfig.auth
  val repoConfig = appConfig.postgres
  val migrationConfig = appConfig.migrations

  private val local: IO[IOLocal[Either[InvalidTokenError, Email]]] =
    IOLocal(Left(InvalidTokenError("No token found")))

  val run =
    migrateDb(migrationConfig) >> SessionPool
      .makePool[IO](repoConfig)
      .use: sessionPool =>
        local.flatMap: local =>
          val fiberLocal = FiberLocal.fromIOLocal(local)
          Routes
            .fromSession(authConfig, fiberLocal, sessionPool)
            .flatMap(Routes.makeServer)
            .use(_ => IO.never)
            .as(ExitCode.Success)
