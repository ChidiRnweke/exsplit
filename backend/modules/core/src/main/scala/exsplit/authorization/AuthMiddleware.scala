package exsplit.authorization

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
import java.time.Instant

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

  def withAuthentication[F[_]: Clock: MonadThrow](
      authChecker: TokenEncoderDecoder
  ): ServerEndpointMiddleware[F] =
    new ServerEndpointMiddleware.Simple[F]:
      def prepareWithHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[F] => HttpApp[F] =
        serviceHints.get[smithy.api.HttpBearerAuth] match
          case Some(_) =>
            endpointHints.get[smithy.api.Auth] match
              case Some(auths) if auths.value.isEmpty => identity
              case _ => expirationCheck(authChecker)

          case None => identity

  private def expirationCheck[F[_]: MonadThrow](decoder: TokenEncoderDecoder)(
      using C: Clock[F]
  ): HttpApp[F] => HttpApp[F] =
    val errMsg = "Your access token has expired. Please log in again."

    inputApp =>
      HttpApp[F]: request =>
        val maybeClaim = claimFromRequest(decoder, request)
        val isAuthorized = for
          now <- C.realTimeInstant
          claim <- maybeClaim.liftTo[F]
          notExpired <- decoder.claimNotExpired(claim, now).liftTo[F]
        yield notExpired

        isAuthorized.ifM(
          ifTrue = inputApp(request),
          ifFalse = AuthError(errMsg).raiseError[F, Response[F]]
        )

  private def extractClaim[F[_]](
      decoder: TokenEncoderDecoder,
      request: Request[F]
  ): Either[InvalidTokenError, Authorization] =
    request.headers
      .get[`Authorization`]
      .toRight(InvalidTokenError("Authorization header not found"))

  private def claimFromRequest[F[_]](
      decoder: TokenEncoderDecoder,
      request: Request[F]
  ): Either[InvalidTokenError, JwtClaim] =
    extractClaim(decoder, request).flatMap:
      case Authorization(Credentials.Token(AuthScheme.Bearer, token)) =>
        decoder.decodeClaim(token)
      case _ => Left(InvalidTokenError("Invalid token"))

  private def tokenFromRequest[F[_]](
      decoder: TokenEncoderDecoder,
      request: Request[F]
  ): Either[InvalidTokenError, Email] =
    extractClaim(decoder, request).flatMap:
      case Authorization(Credentials.Token(AuthScheme.Bearer, token)) =>
        decoder.bearerTokenToEmail(token)
      case _ => Left(InvalidTokenError("Invalid token"))
