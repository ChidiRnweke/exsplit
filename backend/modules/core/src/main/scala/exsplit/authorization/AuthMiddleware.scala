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

/** Middleware for adding request information to the request context.
  *
  * This application makes the distinction between authentication and
  * authorization. The former is carried out by the `withAuthentication`
  * middleware, which checks if the token is present and/or expired. The latter
  * is carried out by the `withRequestInfo` middleware, which adds the email of
  * the authenticated user to the request context
  */
object Middleware:
  /** Adds the email of the authenticated user to the request context. Each
    * route decides how to use this information.
    *
    * @param routes
    *   The routes to be wrapped.
    * @param authConfig
    *   The configuration for the authentication. Necessary because a decoder is
    *   needed to extract the email from the token. This contains the secret key
    *   used for encoding and decoding the token.
    * @param local
    *   The fiber local that stores the email of the authenticated user.
    * @return
    *   The wrapped routes, with the email of the authenticated user in the
    *   request context.
    */
  def withRequestInfo[F[_]: Monad](
      routes: HttpRoutes[F],
      authConfig: AuthConfig,
      local: FiberLocal[F, Either[InvalidTokenError, Email]]
  ): HttpRoutes[F] =
    val decoder = TokenEncoderDecoder(authConfig)
    HttpRoutes[F]: request =>
      val infoMaybe = tokenFromRequest(decoder, request)
      OptionT.liftF(local.set(infoMaybe)) *> routes(request)

  /** Middleware for adding authentication to the routes. It checks if the token
    * is expired and if it is, it returns an error. Unlike the `withRequestInfo`
    * middleware, this middleware is applied uniformly to all routes that have
    * hints for authentication in the same way.
    *
    * @param authChecker
    *   The decoder for the token.
    * @return
    *   The middleware that checks if the token is expired.
    */
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
