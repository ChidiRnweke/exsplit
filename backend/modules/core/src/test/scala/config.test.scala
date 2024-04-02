import munit._
import cats.effect._
import cats._
import cats.syntax.all._
import cats.data._
import exsplit.config._
import pureconfig._
import pureconfig.generic.derivation.default._

class ConfigSuite extends FunSuite:
  test("You should be able to correctly read the migration config"):
    val expected = MigrationsConfig(
      "jdbc:postgresql://localhost:5432/exsplit",
      "exsplit",
      "exsplit",
      "migrations",
      List("db/migrations")
    )
    val appConfig = readConfig[AppConfig]("application.conf")
    assertEquals(appConfig.migrations, expected)

  test("You should be able to correctly read the auth config"):
    val expected = AuthConfig("secret")
    val appConfig = readConfig[AppConfig]("application.conf")
    assertEquals(appConfig.auth, expected)

  test("You should be able to correctly read the postgres config"):
    val expected = PostgresConfig(
      "localhost",
      "exsplit",
      "exsplit",
      "exsplit",
      10
    )
    val appConfig = readConfig[AppConfig]("application.conf")
    assertEquals(appConfig.postgres, expected)
