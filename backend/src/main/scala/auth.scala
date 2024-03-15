import exsplit.spec._
import cats.effect._
import java.time.Instant
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}
import concurrent.duration._
import Environment.AuthConfig
import cats.syntax.all._
import cats._
import java.util.UUID
import com.outr.scalapass.Argon2PasswordFactory

object AuthEntryPoint:
  def createService(
      authConfig: AuthConfig[IO],
      repo: UserRepository[IO]
  ): UserServiceImpl =
    val encoderDecoder = TokenEncoderDecoder(authConfig)
    val authTokenCreator = AuthTokenCreator(encoderDecoder)
    val userAuthenticator = UserAuthenticator(repo)
    UserServiceImpl(authTokenCreator, userAuthenticator)

case class UserServiceImpl(
    authTokenCreator: AuthTokenCreator[IO],
    auth: UserAuthenticator[IO]
) extends UserService[IO]:

  def login(email: Email, password: Password): IO[LoginOutput] =
    for
      authSuccess <- auth.authenticateUser(email, password)
      _ <- IO.raiseWhen(!authSuccess)(AuthError("Invalid credentials"))
      refresh <- authTokenCreator.generateRefreshToken(email)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield (LoginOutput(access, refresh))

  def register(email: Email, password: Password): IO[Unit] =
    for
      userId <- auth.registerUser(email, password)
      refresh <- authTokenCreator.generateRefreshToken(email)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield LoginOutput(access, refresh)

  def refresh(refresh: RefreshToken): IO[RefreshOutput] =
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

case class UserAuthenticator[F[_]](repo: UserRepository[F])(using F: Sync[F]):

  private val factory = Argon2PasswordFactory()

  def authenticateUser(email: Email, password: Password): F[Boolean] =
    for
      userOpt <- repo.findCredentials(email)
      result = userOpt match
        case Some(user) => checkPassword(user.password, password.value)
        case None       => false
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
          hashedPassword = hashPassword(password.value)
          _ <- repo.createUser(userId, hashedPassword)
        yield ()

  def checkPassword(user: String, password: String): Boolean =
    factory.verify(user, password)

  def hashPassword(password: String): String =
    factory.hash(password)

  def createUserId: F[UUID] = F.delay(UUID.randomUUID())

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

case class AuthTokenCreator[F[_]](tokenService: TokenEncoderDecoder[F])(using
    F: Sync[F]
):

  def generateAccessToken(refresh: RefreshToken): F[AccessToken] =
    for
      claimEither <- tokenService.decodeClaim(refresh)
      claim <- Sync[F].fromEither(claimEither)
      email <- Sync[F].fromEither(validateSubject(claim.subject))
      token <- generateToken(TokenLifespan.ShortLived, Email(email))
    yield AccessToken(token)

  def generateRefreshToken(email: Email): F[RefreshToken] =
    generateToken(TokenLifespan.LongLived, email).map(RefreshToken(_))

  private def generateToken(lifeSpan: TokenLifespan, email: Email): F[String] =
    for
      now <- F.realTimeInstant
      claim = tokenService.makeClaim(lifeSpan, email, now)
      token <- tokenService.encodeClaim(claim)
    yield token

  private def validateSubject(
      subjectOpt: Option[String]
  ): Either[InvalidTokenError, String] =
    subjectOpt.toRight(InvalidTokenError("Subject wasn't found in JWT claim."))
