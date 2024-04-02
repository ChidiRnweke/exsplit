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
import exsplit.expenses.ExpensesEntryPoint
import exsplit.expenseList.ExpenseListEntryPoint
import exsplit.circles.CirclesEntryPoint
import natchez.Trace.Implicits.noop
import cats.effect.IO.asyncForIO
import exsplit.migration.migrateDb

object Routes:
  def fromSession(
      config: AuthConfig,
      session: Session[IO]
  ) =
    for
      userService <- AuthEntryPoint.fromSession[IO](session, config)
      expenseService <- ExpensesEntryPoint.fromSession(session)
      expenseListService <- ExpenseListEntryPoint.fromSession(session)
      circlesService <- CirclesEntryPoint.fromSession(session)
      userRoute = SimpleRestJsonBuilder.routes(userService).resource
      expenseRoute = SimpleRestJsonBuilder.routes(expenseService).resource
      expListRoute = SimpleRestJsonBuilder.routes(expenseListService).resource
      circlesRoute = SimpleRestJsonBuilder.routes(circlesService).resource
      routes = userRoute <+> expenseRoute <+> expListRoute <+> circlesRoute
    yield routes.map(_ <+> docs)

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
  val authConfig = readConfig[AuthConfig]("auth.conf")
  val repoConfig = readConfig[PostgresConfig]("database.conf")
  val migrationConfig = readConfig[MigrationsConfig]("database.conf")

  val run =
    migrateDb(migrationConfig) >> SessionPool
      .makePool(repoConfig)
      .use: sessionPool =>
        sessionPool.use: session =>
          Routes
            .fromSession(authConfig, session)
            .flatMap: routes =>
              routes
                .flatMap(Routes.makeServer)
                .use(_ => IO.never)
