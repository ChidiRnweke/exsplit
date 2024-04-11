package exsplit

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import exsplit.config.*
import exsplit.migration.*
import pureconfig._
import pureconfig.generic.derivation.default._
import skunk.Session
import exsplit.auth.AuthEntryPoint
import exsplit.expenseList.ExpenseListEntryPoint
import exsplit.circles.CirclesEntryPoint
import natchez.Trace.Implicits.noop
import cats.effect.IO.asyncForIO
import exsplit.migration.migrateDb
import exsplit.authorization.Middleware
import io.chrisdavenport.fiberlocal.FiberLocal
import exsplit.authorization._
object Routes:
  def fromSession(
      config: AuthConfig,
      local: FiberLocal[IO, Either[InvalidTokenError, Email]],
      session: Session[IO]
  ) =
    for
      emailEither <- local.get
      email = IO.fromEither(emailEither)
      userService <- AuthEntryPoint.fromSession[IO](session, config)
      expenseService <- ExpenseServiceWithAuth.fromSession(email, session)
      expenseListService <- ExpenseListEntryPoint.fromSession(email, session)
      circlesService <- CirclesWithAuthEntryPoint.fromSession(email, session)
      routes = servicesToRoutes(
        userService,
        expenseService,
        expenseListService,
        circlesService
      )
      routesWithLocal = routes.map(Middleware.withRequestInfo(_, config, local))
    yield routesWithLocal

  private def servicesToRoutes(
      userService: UserService[IO],
      expenseService: ExpenseService[IO],
      expenseListService: ExpenseListService[IO],
      circlesService: CirclesService[IO]
  ): Resource[IO, HttpRoutes[IO]] =
    for
      userRoute <- SimpleRestJsonBuilder.routes(userService).resource
      expenseRoute <- SimpleRestJsonBuilder.routes(expenseService).resource
      expListRoute <- SimpleRestJsonBuilder.routes(expenseListService).resource
      circlesRoute <- SimpleRestJsonBuilder.routes(circlesService).resource
    yield userRoute <+> expenseRoute <+> expListRoute <+> circlesRoute <+> docs

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger
      .docs[IO](UserService, ExpenseService, ExpenseListService, CirclesService)

  def makeServer(routes: HttpRoutes[IO]) =
    EmberServerBuilder
      .default[IO]
      .withPort(port"9000")
      .withHost(ipv4"0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .build

object Main extends IOApp.Simple:
  val appConfig = readConfig[AppConfig]("application.conf")
  val authConfig = appConfig.auth
  val repoConfig = appConfig.postgres
  val migrationConfig = appConfig.migrations

  private val local: IO[IOLocal[Either[InvalidTokenError, Email]]] =
    IOLocal(Left(InvalidTokenError("No token found")))

  val run =
    migrateDb(migrationConfig) >> SessionPool
      .makePool(repoConfig)
      .use: sessionPool =>
        sessionPool.use: session =>
          local.flatMap: local =>
            val fiberLocal = FiberLocal.fromIOLocal(local)
            Routes
              .fromSession(authConfig, fiberLocal, session)
              .flatMap: routes =>
                routes
                  .flatMap(Routes.makeServer)
                  .use(_ => IO.never)
                  .as(ExitCode.Success)
