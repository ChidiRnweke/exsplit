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
import cats.effect.std.UUIDGen
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/** This file contains the implementation of the AuthEntryPoint object and
  * related functions. The AuthEntryPoint object provides methods for creating a
  * UserService instance and performing actions related to user authentication
  * such as login, registration and token generation.
  */
object AuthEntryPoint:
  /** Creates a UserServiceImpl instance with the provided dependencies.
    *
    * @param authConfig
    *   The authentication configuration. The configuration for JWT
    *   authentication. Contains the secret key.
    * @param repo
    *   The user repository. The repository for access to user instances.
    * @param clock
    *   The clock for time-related operations.
    * @param validator
    *   The password validator. A trait representing a password validator.
    *   Provides methods for hashing and checking passwords.
    * @param uuid
    *   The UUID generator. Provides methods for generating UUIDs for user IDs
    *   in an effectful context.
    * @return
    *   A UserService instance.
    */
  def createService[F[_]: MonadThrow](
      authConfig: AuthConfig[F],
      repo: UserRepository[F],
      clock: Clock[F],
      validator: PasswordValidator[F],
      uuid: UUIDGen[F]
  ): UserService[F] =
    val claimValidator = ClaimValidator()
    val encoderDecoder = TokenEncoderDecoder(authConfig)
    val tokenCreator = AuthTokenCreator(encoderDecoder, clock, claimValidator)
    val userAuthenticator =
      UserAuthenticator(repo, validator, uuid)
    UserServiceImpl(tokenCreator, userAuthenticator)

  /** Creates a UserService instance with the provided dependencies for the IO
    * effect.
    *
    * @param authConfig
    *   The authentication configuration.
    * @param repo
    *   The user repository.
    * @return
    *   A UserService instance for the IO effect.
    */
  def createIOService(
      authConfig: AuthConfig[IO],
      repo: UserRepository[IO]
  ): UserService[IO] =
    val clock = Clock[IO]
    val bcrypt = BCrypt.createValidator[IO]
    val uuid = UUIDGen[IO]
    createService(authConfig, repo, clock, bcrypt, uuid)

/** Executes the specified action with a valid user.
  *
  * @param userId
  *   The ID of the user.
  * @param userRepo
  *   The user repository.
  * @param action
  *   The action to be performed with the user.
  * @return
  *   The result of the action.
  */
def withValidUser[F[_]: MonadThrow, A](
    userId: UserId,
    userRepo: UserRepository[F]
)(action: User => F[A]): F[A] =
  for
    user <- userRepo.findUserById(userId).rethrow
    result <- action(user)
  yield result

/** Implementation of the UserService trait.
  *
  * @param authTokenCreator
  *   An instance of the AuthTokenCreator trait used for creating authentication
  *   tokens.
  * @param auth
  *   An instance of the UserAuthenticator trait used for user authentication.
  *   This trait provides methods for user login, registration, and token
  *   generation. The methods are implemented using the provided instances of
  *   `AuthTokenCreator` and `UserAuthenticator`. The former holds an instance
  *   of `TokenEncoderDecoder` for encoding and decoding JWT tokens, while the
  *   latter holds an instance of `PasswordValidator` for hashing and checking
  *   passwords.
  *
  * @param F
  *   An instance of the MonadThrow typeclass representing the effect type.
  */
case class UserServiceImpl[F[_]](
    authTokenCreator: AuthTokenCreator[F],
    auth: UserAuthenticator[F]
)(using F: MonadThrow[F])
    extends UserService[F]:

  /** Logs in a user with the provided email and password.
    *
    * @param email
    *   The user's email.
    * @param password
    *   The user's password.
    * @return
    *   A LoginOutput wrapped in the effect type F. The output contains the
    *   access token and refresh token.
    */
  def login(email: Email, password: Password): F[LoginOutput] =
    for
      validatedEmail <- F.fromEither(email.validate)
      authSuccess <- auth.authenticateUser(validatedEmail, password)
      _ <- F.raiseWhen(!authSuccess)(AuthError("Invalid credentials"))
      refresh <- authTokenCreator.generateRefreshToken(validatedEmail)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield (LoginOutput(access, refresh))

  /** Registers a new user with the provided email and password.
    *
    * @param email
    *   The user's email.
    * @param password
    *   The user's password.
    * @return
    *   A RegisterOutput wrapped in the effect type F. The output contains the
    *   user ID, refresh token, and access token.
    */
  def register(email: Email, password: Password): F[RegisterOutput] =
    for
      validatedEmail <- F.fromEither(email.validate)
      userId <- auth.registerUser(validatedEmail, password)
      refresh <- authTokenCreator.generateRefreshToken(validatedEmail)
      access <- authTokenCreator.generateAccessToken(refresh)
    yield RegisterOutput(userId, refresh, access)

  /** Generates a new access token using the provided refresh token.
    *
    * @param refresh
    *   The refresh token.
    * @return
    *   A RefreshOutput wrapped in the effect type F. The output contains the
    *   new access token.
    */
  def refresh(refresh: RefreshToken): F[RefreshOutput] =
    authTokenCreator.generateAccessToken(refresh).map(RefreshOutput(_))

/** Represents an `AuthTokenCreator` that is responsible for creating
  * authentication tokens.
  *
  * @param tokenParser
  *   The token parser used for encoding and decoding tokens.
  *
  * @param clock
  *   The typeclass instance for the clock used for retrieving the current time.
  *
  * @param claimValidator
  *   The validator used for validating token claims. Used to validate the
  *   expiration of the refresh token.
  *
  * @param F
  *   The effect type that provides the necessary capabilities for token
  *   creation. It must have a `MonadThrow` instance because it is used for
  *   error handling.
  */
case class AuthTokenCreator[F[_]](
    tokenParser: TokenEncoderDecoder[F],
    clock: Clock[F],
    claimValidator: ClaimValidator[F]
)(using F: MonadThrow[F]):

  /** Generates an access token based on the provided refresh token.
    *
    * @param refresh
    *   The refresh token used to generate the access token.
    * @return
    *   The generated access token.
    */
  def generateAccessToken(refresh: RefreshToken): F[AccessToken] =
    for
      claimEither <- tokenParser.decodeClaim(refresh.value)
      claim <- F.fromEither(claimEither)
      now <- clock.realTimeInstant
      _ <- claimValidator.validateClaimExpiration(claim, now)
      email <- F.fromEither(validateSubject(claim.subject))
      token <- generateToken(TokenLifespan.ShortLived, Email(email))
    yield AccessToken(token)

  /** Generates a refresh token based on the provided email.
    *
    * @param email
    *   The email associated with the refresh token.
    * @return
    *   The generated refresh token.
    */
  def generateRefreshToken(email: Email): F[RefreshToken] =
    generateToken(TokenLifespan.LongLived, email).map(RefreshToken(_))

  private def generateToken(lifeSpan: TokenLifespan, email: Email): F[String] =
    for
      now <- clock.realTimeInstant
      claim = tokenParser.makeClaim(lifeSpan, email, now)
      token <- tokenParser.encodeClaim(claim)
    yield token

  private def validateSubject(
      subjectOpt: Option[String]
  ): Either[InvalidTokenError, String] =
    subjectOpt.toRight(InvalidTokenError("Subject wasn't found in JWT claim."))

case class User(id: String, email: String, password: String)

/** Enum representing the lifespan of a token. The lifespan can be either
  * long-lived or short-lived. Short-lived tokens are used for access tokens,
  * while long-lived tokens are used for refresh tokens.
  *
  * @param duration
  *   The duration of the token lifespan in seconds.
  * @see
  *   TokenEncoderDecoder
  */
enum TokenLifespan(val duration: Long):
  case LongLived extends TokenLifespan(90.days.toSeconds)
  case ShortLived extends TokenLifespan(1.day.toSeconds)

/** A trait representing a user repository. This trait provides methods for
  * finding and creating users.
  *
  * @tparam F
  *   the effect type, representing the context in which the repository operates
  */
trait UserRepository[F[_]]:
  /** Finds the user credentials based on the email.
    *
    * @param email
    *   the email of the user
    * @return
    *   an effect that yields either a `NotFoundError` or the user with the
    *   given email
    */

  def findUserById(userId: UserId): F[Either[NotFoundError, User]]

  /** Finds the user based on the email.
    *
    * @param email
    *   the email of the user
    * @return
    *   an effect that yields either a `NotFoundError` or the user with the
    *   given email
    */
  def findUserByEmail(email: Email): F[Either[NotFoundError, User]]

  /** Creates a new user with the specified ID, email, and password.
    *
    * @param id
    *   the ID of the user
    * @param email
    *   the email of the user
    * @param password
    *   the password of the user
    * @return
    *   an effect that yields `Unit` when the user is successfully created
    */
  def createUser(id: UUID, email: Email, password: String): F[Unit]

/** Trait representing a password validator. Provides methods for hashing and
  * checking passwords.
  *
  * @tparam F
  *   the effect type for the password validation operations
  */
trait PasswordValidator[F[_]]:

  /** Hashes the given password.
    *
    * @param password
    *   the password to hash
    * @return
    *   the hashed password
    */
  def hashPassword(password: String): F[String]

  /** Checks if the given password matches the provided hash. Returns true if
    * the password matches the hash, false otherwise. Checking a password does
    * not require an effectful operation.
    *
    * @param hash
    *   the hashed password
    * @param password
    *   the password to check
    * @return
    *   true if the password matches the hash, false otherwise
    */
  def checkPassword(hash: String, password: String): Boolean

/** Provides functionality for working with BCrypt password hashing and
  * validation.
  */
object BCrypt:
  /** Creates a password validator using the BCrypt algorithm.
    *
    * @return
    *   a new instance of PasswordValidator that uses BCrypt for password
    *   hashing and validation
    * @tparam F
    *   the effect type for the password validation operations Creating a hash
    *   for a password is an effectful operation because it involves generating
    *   a random salt for the hash. The effect type must have a `Sync` instance
    *   to create the password validator.
    */
  def createValidator[F[_]](using F: Sync[F]): PasswordValidator[F] =
    new PasswordValidator[F]:
      private val bcrypt = BCryptPasswordEncoder()

      /** Hashes a password using BCrypt.
        *
        * @param password
        *   the password to hash
        * @return
        *   the hashed password
        *
        * @note
        *   This method is effectful because it generates a random salt for the
        *   hash. The effect type `F` must have a `Sync` instance to create the
        *   password validator.
        */
      def hashPassword(password: String): F[String] =
        F.delay(bcrypt.encode(password))

      /** Checks if a password matches a BCrypt hash.
        *
        * @param hash
        *   the BCrypt hash
        * @param password
        *   the password to check
        * @return
        *   true if the password matches the hash, false otherwise. This method
        *   is not effectful because it does not require any effectful
        *   operations.
        */
      def checkPassword(hash: String, password: String): Boolean =
        bcrypt.matches(password, hash)

/** UserAuthenticator is responsible for authenticating and registering users.
  *
  * @param repo
  *   The repository for access to user instances.
  * @param validator
  *   The password validator for checking and hashing passwords.
  * @param uuid
  *   The UUID generator for creating user IDs.
  * @param F
  *   The effect type constructor, providing the necessary type class instances.
  */
case class UserAuthenticator[F[_]](
    repo: UserRepository[F],
    validator: PasswordValidator[F],
    uuid: UUIDGen[F]
)(using
    F: MonadThrow[F]
):

  /** Authenticates a user by checking their email and password.
    *
    * @param email
    *   The user's email.
    * @param password
    *   The user's password.
    * @return
    *   A boolean indicating whether the authentication was successful.
    */
  def authenticateUser(email: Email, password: Password): F[Boolean] =
    for
      userEither <- repo.findUserByEmail(email)
      result = userEither match
        case Right(user) =>
          validator.checkPassword(user.password, password.value)
        case Left(_) => false
    yield result

  /** Registers a new user with the provided email and password.
    *
    * @param email
    *   The user's email.
    * @param password
    *   The user's password.
    * @return
    *   A string representing the newly created user's ID.
    * @throws RegistrationError
    *   if the user already exists.
    */
  def registerUser(email: Email, password: Password): F[String] =
    val userExists = repo.findUserByEmail(email).map(_.isRight)
    userExists.ifM(
      F.raiseError(RegistrationError("User already exists.")),
      makeUser(email, password)
    )

  private def createUserId: F[UUID] = uuid.randomUUID

  private def makeUser(email: Email, password: Password): F[String] =
    createUserId.flatMap: userId =>
      for
        hashedPassword <- validator.hashPassword(password.value)
        _ <- repo.createUser(userId, email, hashedPassword)
      yield userId.toString()

/** TokenEncoderDecoder is responsible for encoding and decoding JWT tokens.
  *
  * @param authConfig
  *   The configuration for JWT authentication. Contains the secret key.
  * @tparam F
  *   The effect type for the encoding and decoding operations.
  */
case class TokenEncoderDecoder[F[_]: Functor](authConfig: AuthConfig[F]):

  /** Decodes the given refresh token and returns the decoded JWT claim.
    *
    * @param token
    *   The refresh token to decode.
    * @return
    *   Either a Throwable if decoding fails, or the decoded JwtClaim wrapped in
    *   the effect F.
    */
  def decodeClaim(token: String): F[Either[Throwable, JwtClaim]] =
    authConfig.secretKey.map(key =>
      JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256)).toEither
    )

  /** Encodes the given JwtClaim and returns the encoded JWT token as a string.
    *
    * @param claim
    *   The JwtClaim to encode.
    * @return
    *   The encoded JWT token wrapped in the effect F. This method uses the
    *   secret key from the `authConfig` to encode the claim that is why it is
    *   wrapped in the effect F.
    */
  def encodeClaim(claim: JwtClaim): F[String] =
    authConfig.secretKey.map(key =>
      JwtUpickle.encode(claim, key, JwtAlgorithm.HS256)
    )

  /** Creates a JwtClaim with the specified lifespan, email, and current time.
    *
    * @param lifeSpan
    *   The lifespan of the token. Short-lived for access tokens, long-lived for
    *   refresh tokens.
    * @param email
    *   The email associated with the token. Used as the subject in the claim.
    * @param now
    *   The current time.
    * @return
    *   The created JwtClaim.
    */
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

/** Represents an `AuthTokenCreator` that is responsible for generating access
  * tokens and refresh tokens.
  *
  * @param tokenService
  *   The service used for encoding and decoding tokens.
  * @param clock
  *   The clock instance used for retrieving the current time.
  * @param F
  *   The effect type constructor, which must have a `MonadThrow` instance.
  */

/** TokenValidator is responsible for validating JWT tokens. It provides methods
  * for validating the expiration of a claim and checking if a given epoch time
  * is expired based on the current time.
  *
  * It's a class of its own because it is also used by the middleware to
  * validate the access token.
  */
case class ClaimValidator[F[_]]()(using F: MonadThrow[F]):

  /** Validates the expiration of a JWT claim.
    *
    * @param claim
    *   The JWT claim to validate.
    * @param now
    *   The current instant.
    * @return
    *   A `Unit` in the effect `F` if the claim is not expired, otherwise raises
    *   an `AuthError`. This method can also throw a second exception if the
    *   expiration time is not found in the claim (an `InvalidTokenError`).
    */
  def validateClaimExpiration(claim: JwtClaim, now: Instant): F[Unit] =
    val err = AuthError("Token has expired log in again to receive a new one.")
    val notExpired =
      extractExpiration(claim).map(expiration => isNotExpired(expiration, now))
    F.ifM(notExpired)(F.unit, F.raiseError(err))

  /** Checks if a given epoch time is expired based on the current time.
    *
    * @param expiration
    *   The expiration time as an epoch time.
    * @param now
    *   The current time as an Instant object.
    * @return
    *   True if the expiration time is in the future, false otherwise.
    */
  private def isNotExpired(expiration: Long, now: Instant): Boolean =
    now.getEpochSecond < expiration

  /** Extracts the expiration time from a JwtClaim object.
    *
    * @param claim
    *   The JwtClaim object to extract the expiration time from.
    * @return
    *   The expiration time as a Long value wrapped in an F context. If the
    *   expiration time is not found in the claim, an `InvalidTokenError`. That
    *   is why the result is wrapped in the effect F.
    */
  private def extractExpiration(claim: JwtClaim): F[Long] =
    F.fromOption(
      claim.expiration,
      InvalidTokenError("No expiration found in JWT claim.")
    )

/** Extension method for validating an email address. This method checks if the
  * email address is in a valid format. Smithy4s does not do this by default, so
  * this extension method is provided.
  * @param email
  *   The email address to validate.
  * @return
  *   Either a `ValidationError` if the email is invalid, or the validated
  *   `Email` object.
  */
extension (email: Email)
  def validate: Either[ValidationError, Email] =
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    email.value.matches(emailRegex) match
      case true  => Right(email)
      case false => Left(ValidationError("Invalid email format."))
