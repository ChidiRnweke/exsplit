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

object Routes:
  def fromSession(
      config: AuthConfig[IO],
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

object Main extends IOApp.Simple:
  val authConfig = ??? // TODO: Implement AuthConfig
  val repoConfig = ??? // TODO: Implement RepositoryConfig

  val dbConfig =
    ConfigSource.resources("database.conf").loadOrThrow[MigrationsConfig]

  val run =
    SessionPool
      .makePool(repoConfig)
      .use: session =>
        session.use: session =>
          Routes
            .fromSession(authConfig, session)
            .flatMap: routes =>
              routes
                .flatMap: routes =>
                  EmberServerBuilder
                    .default[IO]
                    .withPort(port"9000")
                    .withHost(ipv4"0.0.0.0")
                    .withHttpApp(routes.orNotFound)
                    .build
                .use(_ => IO.never)
