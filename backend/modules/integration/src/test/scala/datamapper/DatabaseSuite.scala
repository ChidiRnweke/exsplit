package exsplit.integration
import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import munit.CatsEffectSuite
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import exsplit.migration._
import com.dimafeng.testcontainers.DockerComposeContainer
import com.dimafeng.testcontainers.ExposedService
import org.testcontainers.containers.wait.strategy.Wait
import skunk.Session
import natchez.Trace.Implicits.noop
import java.io.File
import exsplit.database.SessionPool

/** Abstract class representing a database suite for integration testing. This
  * is a `CatsEffectSuite` which gives it the ability to run effectful tests. It
  * also extends `TestContainerForAll` to provide the ability to run tests with
  * a postgres container.
  *
  * The `DatabaseSuite` provides a session pool and migration fixtures for the
  * tests.
  *
  * This means that any test suite that extends `DatabaseSuite` will have access
  * `sessionPool` that provides a session pool for the database. Migrations will
  * also be run before the tests.
  *
  * example:
  * ```scala
  * class CirclesMapperSuite extends DatabaseSuite:
  *   test("It is possible to write and then read a circle from the database"):
  *   val session = sessionPool()
  *   val input = CreateCircleInput(UserId("2"), "User", "Test Circle")
  *   session.use(session =>
  *     for
  *       circlesRepo <- CirclesRepository.fromSession(session)
  *       createdCircle <- circlesRepo.main.create(input)
  *       circleRead <- circlesRepo.main.get(CircleId(createdCircle.id))
  *     yield assert(circleRead.isRight)
  *   )
  * ```
  */
abstract class DatabaseSuite extends CatsEffectSuite with TestContainerForAll:

  private val appConfig = readConfig[AppConfig]("application.conf")

  /** Resource suite local fixture for the session pool. It creates a session
    * pool for the database using the `appConfig.postgres` configuration.
    */
  val sessionPool = ResourceSuiteLocalFixture(
    "sessionPool",
    SessionPool.makePool[IO](appConfig.postgres)
  )

  /** Resource suite local fixture for the database migration. It performs
    * database migration using the `appConfig.migrations` configuration.
    */
  private val migration = ResourceSuiteLocalFixture(
    "migration",
    Resource.make(migrateDb(appConfig.migrations).void)(_ => IO.unit)
  )

  /** Wait strategy for checking if the database system is ready to accept
    * connections. It wait s for the log message ".*database system is ready to
    * accept connections.*" to appear twice.
    */
  private val waitStrategy = Wait.forLogMessage(
    ".*database system is ready to accept connections.*",
    2
  )

  /** Overrides the `munitFixtures` method to provide the sessionPool and
    * migration fixtures.
    */
  override def munitFixtures = List(sessionPool, migration)

  /** Exposed service for the PostgreSQL container. It specifies the name, port,
    * and wait strategy for the container.
    */
  private val pgService = ExposedService("postgres_1", 5432, waitStrategy)

  /** Overrides the `containerDef` value to provide the DockerComposeContainer
    * definition. It specifies the Docker Compose file, tailChildContainers
    * option, and exposed services.
    */
  override val containerDef = DockerComposeContainer.Def(
    File("src/test/resources/docker-compose-test.yml"),
    tailChildContainers = true,
    exposedServices = Seq(pgService)
  )
