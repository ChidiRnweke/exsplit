import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import munit.CatsEffectSuite
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import exsplit.migration._
import scala.sys.process._
import scala.concurrent.duration.*
import com.dimafeng.testcontainers.DockerComposeContainer
import java.io.File
import com.dimafeng.testcontainers.ExposedService
import org.testcontainers.containers.wait.strategy.Wait

class ConfigSuite extends munit.FunSuite:
  test("Config files are correctly read with pureConfig"):
    val obtained =
      readConfig[MigrationsConfig]("database_test.conf")
    val expected = MigrationsConfig(
      "jdbc:postgresql://localhost:5432/exsplit",
      "exsplit",
      "exsplit",
      "migrations",
      List("db/migrations")
    )
    assertEquals(obtained, expected)

class MigrationsSuite extends CatsEffectSuite with TestContainerForAll:

  private val waitStrategy = Wait.forLogMessage(
    ".*database system is ready to accept connections.*",
    2
  )

  private val pgService = ExposedService("postgres_1", 5432, waitStrategy)
  override val containerDef = DockerComposeContainer.Def(
    File("src/test/resources/docker-compose-test.yml"),
    tailChildContainers = true,
    exposedServices = Seq(pgService)
  )

  test("Migrations are correctly applied"):
    val config =
      readConfig[MigrationsConfig]("database_test.conf")
    migrateDb(config)
      .map(result => assertEquals(result.success, true))
