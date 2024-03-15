import exsplit.spec._
import cats.effect._
import java.time.Instant
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}
import concurrent.duration._
import Environment.AuthConfig
import cats.syntax.all._
import cats._
import java.util.UUID

enum TokenLifespan(val duration: Long):
  case LongLived extends TokenLifespan(90.days.toSeconds)
  case ShortLived extends TokenLifespan(1.day.toSeconds)

trait UserRepository[F[_]]:
  def findUser(email: Email): F[Option[User]]
  def createUser(email: Email, password: Password): F[Unit]

trait TokenEncoderDecoder[F[_]: Functor](authConfig: AuthConfig[F]):

  def decodeClaim(token: String): F[Either[Throwable, JwtClaim]] =
    authConfig.secretKey.map(key =>
      JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256)).toEither
    )

  def encodeClaim(claim: JwtClaim): F[String] =
    authConfig.secretKey.map(key =>
      JwtUpickle.encode(claim, key, JwtAlgorithm.HS256)
    )

  def makeClaim(
      lifeSpan: TokenLifespan,
      userId: UserId,
      now: Instant
  ): JwtClaim =
    JwtClaim(
      expiration = Some(now.plusSeconds(lifeSpan.duration).getEpochSecond),
      issuedAt = Some(now.getEpochSecond),
      subject = Some(userId.value.toString())
    )

trait TokenCreator[F[_]](tokenService: TokenEncoderDecoder[F])(using
    F: Sync[F],
    clock: Clock[F]
):

  def generateAccessToken(refresh: RefreshToken): F[AccessToken] =
    for
      claimEither <- tokenService.decodeClaim(refresh.value)
      claim <- Sync[F].fromEither(claimEither)
      subject <- Sync[F].fromEither(validateSubject(claim.subject))
      userId <- Sync[F].fromEither(parseUserId(subject))
      token <- generateToken(TokenLifespan.ShortLived, UserId(userId))
    yield AccessToken(token)

  def generateRefreshToken(userId: UserId): F[RefreshToken] =
    generateToken(TokenLifespan.LongLived, userId).map(RefreshToken(_))

  private def validateSubject(
      subjectOpt: Option[String]
  ): Either[ValidationError, String] =
    subjectOpt.toRight(ValidationError("Subject wasn't found in JWT claim."))

  private def parseUserId(subject: String): Either[ValidationError, UUID] =
    Either
      .catchOnly[IllegalArgumentException](UUID.fromString(subject))
      .leftMap(_ => ValidationError("Subject in JWT claim isn't a valid UUID."))

  private def generateToken(
      lifeSpan: TokenLifespan,
      userId: UserId
  ): F[String] =
    for
      now <- F.realTimeInstant
      claim = tokenService.makeClaim(lifeSpan, userId, now)
      token <- tokenService.encodeClaim(claim)
    yield token

case class AuthTokenCreator(tokenService: TokenEncoderDecoder[IO])
    extends TokenCreator[IO](tokenService)

case class UserServiceImpl(authTokenCreator: AuthTokenCreator)
    extends UserService[IO]:
  def login(email: Email, password: Password): IO[LoginOutput] =
    val email = ???
    for
      refresh <- authTokenCreator.generateRefreshToken(email)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield LoginOutput(access, refresh)
  def register(email: Email, password: Password): IO[Unit] =
    val email = ???
    for
      refresh <- authTokenCreator.generateRefreshToken(email)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield LoginOutput(access, refresh)

  def refresh(refreshToken: RefreshToken): IO[RefreshOutput] = ???
