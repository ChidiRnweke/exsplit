package exsplit.routes

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats.data._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import exsplit.config.*
import skunk.Session
import exsplit.authorization.Middleware
import io.chrisdavenport.fiberlocal.FiberLocal
import exsplit.authorization._
import cats._
import exsplit.auth._
import exsplit.database._

/** Represents the routes for the application. This object creates the routes
  * for the user service, expense service, expense list service, and circles
  * service.
  */
object Routes:
  /*
   * Creates a new instance of `HttpRoutes` using the provided configuration.
   * This is the main entry point for the application. It creates the routes for
   * the user service, expense service, expense list service, and circles service.
   * The routes are wrapped in a resource, which is a type that manages the
   * lifecycle of the routes. The resource is created using the provided
   * configuration, local fiber, and session pool.
   *
   * @param config
   *  The configuration for the application.
   *
   * @param local
   * This is a fiber local that stores the email of the authenticated user. It is
   * used to retrieve the email of the authenticated user by the middleware.
   *
   * @param pool
   * The session pool to be used for database operations.
   *
   * @return
   * A `Resource[F, HttpRoutes[F]]` representing the created routes instance.
   *
   *
   */
  def fromSession[F[_]: Async: Parallel](
      config: AuthConfig,
      local: FiberLocal[F, Either[InvalidTokenError, Email]],
      pool: AppSessionPool[F]
  ): Resource[F, HttpRoutes[F]] =
    val getEmail = local.get.flatMap(_.liftTo[F])

    val auth = AuthEntryPoint.fromSession(pool, config)
    val expense = ExpenseServiceWithAuth.fromSession(getEmail, pool)
    val expenseList = ExpenseListServiceWithAuth.fromSession(getEmail, pool)
    val circles = CirclesWithAuthEntryPoint.fromSession(getEmail, pool)
    servicesToRoutes(config, auth, expense, expenseList, circles)
      .map(Middleware.withRequestInfo(_, config, local))

  private def servicesToRoutes[F[_]: Async](
      config: AuthConfig,
      userService: UserService[F],
      expenseService: ExpenseService[F],
      expenseListService: ExpenseListService[F],
      circlesService: CirclesService[F]
  ): Resource[F, HttpRoutes[F]] =
    val decoder = TokenEncoderDecoder(config)
    (
      SimpleRestJsonBuilder
        .routes(userService)
        .middleware(Middleware.withAuthentication(decoder))
        .resource,
      SimpleRestJsonBuilder
        .routes(expenseService)
        .middleware(Middleware.withAuthentication(decoder))
        .resource,
      SimpleRestJsonBuilder
        .routes(expenseListService)
        .middleware(Middleware.withAuthentication(decoder))
        .resource,
      SimpleRestJsonBuilder
        .routes(circlesService)
        .middleware(Middleware.withAuthentication(decoder))
        .resource
    ).mapN(_ <+> _ <+> _ <+> _ <+> docs)

  private def docs[F[_]: Sync]: HttpRoutes[F] =
    smithy4s.http4s.swagger
      .docs[F](UserService, ExpenseService, ExpenseListService, CirclesService)

  /** Creates the HTTP server using the provided routes. The server listens on
    * port 9000 and binds to the IP address
    *
    * @param routes
    *   The routes to be served by the server.
    */
  def makeServer[F[_]: Async](routes: HttpRoutes[F]) =
    EmberServerBuilder
      .default[F]
      .withPort(port"9000")
      .withHost(ipv4"0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .build
