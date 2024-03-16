/** This file contains the implementation of the authentication module. It
  * provides functionality for user login, registration, and token generation.
  * The main components of this module are:
  *   - `AuthEntryPoint`: An object that creates the main service for
  *     authentication.
  *   - `UserServiceImpl`: The implementation of the `UserService` trait, which
  *     provides methods for login, registration, and token generation.
  *   - `User`: A case class representing a user with an ID, email, and
  *     password.
  *   - `TokenLifespan`: An enumeration representing the lifespan of a token
  *     (long-lived or short-lived).
  *   - `UserRepository`: A trait defining methods for accessing user data from
  *     a repository.
  *   - `UserAuthenticator`: A class that handles user authentication and
  *     registration.
  *   - `TokenEncoderDecoder`: A class that encodes and decodes JWT claims.
  *   - `AuthTokenCreator`: A class that generates access and refresh tokens.
  */
package exsplit.auth

import exsplit.spec._
import cats.effect._
import java.time.Instant
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}
import concurrent.duration._
import exsplit.config.AuthConfig
import cats.syntax.all._
import cats._
import java.util.UUID
import com.outr.scalapass.Argon2PasswordFactory
import cats.effect.std.UUIDGen

object AuthEntryPoint:
  def createService[F[_]: MonadThrow](
      authConfig: AuthConfig[F],
      repo: UserRepository[F],
      clock: Clock[F],
      validator: PasswordValidator[F],
      uuid: UUIDGen[F]
  ): UserServiceImpl[F] =
    val encoderDecoder = TokenEncoderDecoder(authConfig)
    val authTokenCreator = AuthTokenCreator(encoderDecoder, clock)
    val userAuthenticator =
      UserAuthenticator(repo, validator, uuid)
    UserServiceImpl(authTokenCreator, userAuthenticator)

  def createIOService(
      authConfig: AuthConfig[IO],
      repo: UserRepository[IO]
  ): UserServiceImpl[IO] =
    val clock = Clock[IO]
    val argon = Argon.createValidator[IO]
    val uuid = UUIDGen[IO]
    createService(authConfig, repo, clock, argon, uuid)

case class UserServiceImpl[F[_]](
    authTokenCreator: AuthTokenCreator[F],
    auth: UserAuthenticator[F]
)(using F: MonadThrow[F])
    extends UserService[F]:

  def login(email: Email, password: Password): F[LoginOutput] =
    for
      authSuccess <- auth.authenticateUser(email, password)
      _ <- F.raiseWhen(!authSuccess)(AuthError("Invalid credentials"))
      refresh <- authTokenCreator.generateRefreshToken(email)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield (LoginOutput(access, refresh))

  def register(email: Email, password: Password): F[Unit] =
    for
      userId <- auth.registerUser(email, password)
      refresh <- authTokenCreator.generateRefreshToken(email)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield LoginOutput(access, refresh)

  def refresh(refresh: RefreshToken): F[RefreshOutput] =
    authTokenCreator.generateAccessToken(refresh).map(RefreshOutput(_))

case class User(id: String, email: String, password: String)

enum TokenLifespan(val duration: Long):
  case LongLived extends TokenLifespan(90.days.toSeconds)
  case ShortLived extends TokenLifespan(1.day.toSeconds)

trait UserRepository[F[_]]:
  def findCredentials(email: Email): F[Option[User]]
  def findUserById(userId: UserId): F[Option[User]]
  def findUserByEmail(email: Email): F[Option[User]]
  def createUser(id: UUID, password: String): F[Unit]

trait PasswordValidator[F[_]]:

  def hashPassword(password: String): F[String]
  def checkPassword(hash: String, password: String): Boolean
object Argon:
  private val factory = Argon2PasswordFactory()

  def createValidator[F[_]](using F: Sync[F]): PasswordValidator[F] =
    new PasswordValidator[F]:
      def hashPassword(password: String): F[String] =
        F.delay(factory.hash(password))
      def checkPassword(hash: String, password: String): Boolean =
        factory.verify(hash, password)

case class UserAuthenticator[F[_]](
    repo: UserRepository[F],
    validator: PasswordValidator[F],
    uuid: UUIDGen[F]
)(using
    F: MonadThrow[F]
):

  def authenticateUser(email: Email, password: Password): F[Boolean] =
    for
      userOpt <- repo.findCredentials(email)
      result = userOpt match
        case Some(user) =>
          validator.checkPassword(user.password, password.value)
        case None => false
    yield result

  def registerUser(email: Email, password: Password): F[Unit] =
    repo
      .findUserByEmail(email)
      .flatMap:
      case Some(_) =>
        F.raiseError(RegistrationError("User already exists."))
      case None =>
        for
          userId <- createUserId
          hashedPassword <- validator.hashPassword(password.value)
          _ <- repo.createUser(userId, hashedPassword)
        yield ()

  def createUserId: F[UUID] = uuid.randomUUID

case class TokenEncoderDecoder[F[_]: Functor](authConfig: AuthConfig[F]):

  def decodeClaim(token: RefreshToken): F[Either[Throwable, JwtClaim]] =
    authConfig.secretKey.map(key =>
      JwtUpickle.decode(token.value, key, Seq(JwtAlgorithm.HS256)).toEither
    )

  def encodeClaim(claim: JwtClaim): F[String] =
    authConfig.secretKey.map(key =>
      JwtUpickle.encode(claim, key, JwtAlgorithm.HS256)
    )

  def makeClaim(
      lifeSpan: TokenLifespan,
      email: Email,
      now: Instant
  ): JwtClaim =
    JwtClaim(
      expiration = Some(now.plusSeconds(lifeSpan.duration).getEpochSecond),
      issuedAt = Some(now.getEpochSecond),
      subject = Some(email.value)
    )

case class AuthTokenCreator[F[_]](
    tokenService: TokenEncoderDecoder[F],
    clock: Clock[F]
)(using F: MonadThrow[F]):

  def generateAccessToken(refresh: RefreshToken): F[AccessToken] =
    for
      claimEither <- tokenService.decodeClaim(refresh)
      claim <- F.fromEither(claimEither)
      email <- F.fromEither(validateSubject(claim.subject))
      token <- generateToken(TokenLifespan.ShortLived, Email(email))
    yield AccessToken(token)

  def generateRefreshToken(email: Email): F[RefreshToken] =
    generateToken(TokenLifespan.LongLived, email).map(RefreshToken(_))

  private def generateToken(lifeSpan: TokenLifespan, email: Email): F[String] =
    for
      now <- clock.realTimeInstant
      claim = tokenService.makeClaim(lifeSpan, email, now)
      token <- tokenService.encodeClaim(claim)
    yield token

  private def validateSubject(
      subjectOpt: Option[String]
  ): Either[InvalidTokenError, String] =
    subjectOpt.toRight(InvalidTokenError("Subject wasn't found in JWT claim."))
