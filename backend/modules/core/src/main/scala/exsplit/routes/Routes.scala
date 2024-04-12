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

object Routes:
  def fromSession[F[_]: Async: Parallel](
      config: AuthConfig,
      local: FiberLocal[F, Either[InvalidTokenError, Email]],
      session: Session[F]
  ): F[Resource[F, HttpRoutes[F]]] =
    val getEmail = local.get.flatMap(_.liftTo[F])
    (
      AuthEntryPoint.fromSession(session, config),
      ExpenseServiceWithAuth.fromSession(getEmail, session),
      ExpenseListServiceWithAuth.fromSession(getEmail, session),
      CirclesWithAuthEntryPoint.fromSession(getEmail, session)
    ).mapN:
      servicesToRoutes(config, _, _, _, _)
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

  def makeServer[F[_]: Async](routes: HttpRoutes[F]) =
    EmberServerBuilder
      .default[F]
      .withPort(port"9000")
      .withHost(ipv4"0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .build
