import hello._
import exsplit.spec._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import exsplit.config.*
import exsplit.migration.*
import pureconfig._
import pureconfig.generic.derivation.default._

object HelloWorldImpl extends HelloWorldService[IO]:
  def hello(name: String, town: Option[String]): IO[Greeting] =
    IO.pure(
      town match
        case None    => Greeting(s"Hello $name!")
        case Some(t) => Greeting(s"Hello $name from $t!")
    )

object Routes:
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger
      .docs[IO](UserService, ExpenseService, ExpenseListService, CirclesService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)

object Main extends IOApp.Simple:

  val dbConfig =
    ConfigSource.resources("database.conf").loadOrThrow[MigrationsConfig]

  val run = IO.println(dbConfig) >> Routes.all
    .flatMap: routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(ipv4"0.0.0.0")
        .withHttpApp(routes.orNotFound)
        .build
    .use(_ => IO.never)
