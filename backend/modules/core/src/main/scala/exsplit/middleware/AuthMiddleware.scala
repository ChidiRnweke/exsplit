package exsplit.middleware

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data
import exsplit.auth._
import smithy4s._
import org.http4s._
import smithy4s.http4s._
import org.http4s.headers.Authorization
import exsplit.datamapper.user._
import pdi.jwt.JwtClaim
import exsplit.datamapper.circles._
import io.chrisdavenport.fiberlocal.FiberLocal
import cats.data._
import exsplit.config.AuthConfig

object Middleware:

  def withRequestInfo[F[_]: Monad](
      routes: HttpRoutes[F],
      authConfig: AuthConfig,
      local: FiberLocal[F, Either[InvalidTokenError, Email]]
  ): HttpRoutes[F] =
    val decoder = TokenEncoderDecoder(authConfig)
    HttpRoutes[F]: request =>
      val infoMaybe = tokenFromRequest(decoder, request)
      OptionT.liftF(local.set(infoMaybe)) *> routes(request)

  private def tokenFromRequest[F[_]](
      decoder: TokenEncoderDecoder,
      request: Request[F]
  ): Either[InvalidTokenError, Email] =
    request.headers
      .get[`Authorization`]
      .toRight(InvalidTokenError("Authorization header not found"))
      .flatMap:
        case Authorization(Credentials.Token(AuthScheme.Bearer, token)) =>
          decoder.bearerTokenToEmail(token)
        case _ => Left(InvalidTokenError("Invalid token"))
