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
import exsplit.datamapper.user._
import skunk.Session

/** The entry point for the auth module. This object provides methods for
  * creating a UserService instance with the provided dependencies.
  *
  * The most important method is `fromSession`, which creates a UserService
  * instance using the provided session and authentication configuration. The
  * service is wrapped in the effect type F because when the session is created
  * prepared statements are created, which is an effectful operation.
  *
  * Some parts of the service can be tested without a database connection. For
  * example, the password hashing and validation can be tested without a
  * database connection. The `AuthEntryPoint` object provides a method for
  * creating a UserService instance where you manually provide the dependencies.
  * This is useful for testing the service without a database connection.
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
    *   The clock. Provides methods for retrieving the current time. Used for
    *   generating tokens.
    * @param validator
    *   The password validator. A trait representing a password validator.
    *   Provides methods for hashing and checking passwords.
    * @param uuid
    *   The UUID generator. Provides methods for generating UUIDs for user IDs
    *   in an effectful context. Used for generating user IDs.
    * @return
    *   A UserService instance. This is an implementation of the UserService
    *   trait generated by smithy4s.
    */
  def createService[F[_]: MonadThrow](
      authConfig: AuthConfig,
      repo: UserMapper[F],
      clock: Clock[F],
      validator: PasswordValidator[F],
      uuid: UUIDGen[F]
  ): UserService[F] =
    val encoderDecoder = TokenEncoderDecoder(authConfig)
    val tokenCreator = AuthTokenCreator(encoderDecoder, clock)
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
      authConfig: AuthConfig,
      repo: UserMapper[IO]
  ): UserService[IO] =
    val clock = Clock[IO]
    val bcrypt = BCrypt.createValidator[IO]
    val uuid = UUIDGen[IO]
    createService(authConfig, repo, clock, bcrypt, uuid)

  /** Creates a UserService instance using the provided session and
    * authentication configuration. The service is wrapped in the effect type F
    * because the user repository is created using the session, which is
    * effectful.
    * @param session
    *   The session to be used for database operations.
    * @param authConfig
    *   The authentication configuration. The configuration for JWT
    * @return
    *   A UserService instance. This is an implementation of the UserService
    *   trait generated by smithy4s.
    */
  def fromSession[F[_]: Async](
      session: Session[F],
      authConfig: AuthConfig
  ): F[UserService[F]] =
    for
      userRepo <- UserMapper.fromSession[F](session)
      uuid = UUIDGen[F]
      validator = BCrypt.createValidator[F]
      clock = Clock[F]
    yield createService(authConfig, userRepo, clock, validator, uuid)

/** Executes the specified action with a valid user. The action is performed
  * with the user read from the database. If the user is not found, an error is
  * raised. This method is used for performing actions with a valid user. The
  * user is retrieved from the database using the provided user ID.
  *
  * This is necessary because many operations have a foreign key constraint on
  * the user ID. The user ID must be valid for the operation to succeed. This
  * method ensures that the user ID is valid by checking if the user exists in
  * the database.
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
    userRepo: UserMapper[F]
)(action: User => F[A]): F[A] =
  for
    userRead <- userRepo.findUserById(userId).rethrow
    result <- action(userRead.toUser)
  yield result

/** Implementation of the UserService trait. This trait is automatically
  * generated by smithy4s from the user.smithy file. It provides methods for
  * user login, registration, and token generation.
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

  /** Registers a new user with the provided email and password. The password is
    * hashed before being stored in the database.
    *
    * @param email
    *   The user's email.
    * @param password
    *   The user's password.
    * @return
    *   A RegisterOutput wrapped in the effect type F. The output contains the
    *   user ID, refresh token, and access token. This is part of the eventual
    *   response to the client.
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
  * authentication tokens. It provides methods for generating access tokens and
  * refresh tokens based on the provided refresh token and email.
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
    tokenParser: TokenEncoderDecoder,
    clock: Clock[F]
)(using F: MonadThrow[F]):

  /** Generates an access token based on the provided refresh token.
    *
    * @param refresh
    *   The refresh token used to generate the access token.
    * @return
    *   The generated access token.
    */
  def generateAccessToken(refresh: RefreshToken): F[AccessToken] =
    val claimEither = tokenParser.decodeClaim(refresh.value)
    for
      claim <- F.fromEither(claimEither)
      now <- clock.realTimeInstant
      notExpiredEither = ClaimValidator.claimNotExpired(claim, now)
      notExpired <- F.fromEither(notExpiredEither)
      _ <- F.raiseWhen(!notExpired)(AuthError("Token is expired."))
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
      token = tokenParser.encodeClaim(claim)
    yield token

  private def validateSubject(
      subjectOpt: Option[String]
  ): Either[InvalidTokenError, String] =
    subjectOpt.toRight(InvalidTokenError("Subject wasn't found in JWT claim."))

/** the domain model for a user. It has two counterparts: `UserReadMapper` and
  * `UserWriteMapper`. The `UserReadMapper` is used for reading user data from
  * the database, while the `UserWriteMapper` is used for writing user data to
  * the database. The `User` domain model is used for the business logic of the
  * application.
  *
  * @param id
  *   The unique identifier of the user.
  * @param email
  *   The email address of the user.
  * @param password
  *   The password of the user.
  */
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
    *
    * The underlying BCryptPasswordEncoder is from the Spring Security library.
    * The `Sync` instance is used to create the password validator because the
    * `hashPassword` method is effectful and must be delayed. It generates a
    * random salt for the hash. The `checkPassword` method is not effectful
    * because it does not require any effectful operations.
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
  *
  * @note
  *   The uuid parameter is a type class instance of UUIDGen. The auth package
  *   is the only place where the UUIDGen type class is used. The rest of the
  *   application lets the database handle the generation of random IDs. In the
  *   future, the UUIDGen type class could be removed and the UUID generation
  *   could be handled by the database as well.
  */
case class UserAuthenticator[F[_]](
    repo: UserMapper[F],
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
      userReadEither <- repo.findUserByEmail(email)
      result = userReadEither match
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
  */
case class TokenEncoderDecoder(authConfig: AuthConfig):

  /** Decodes the given refresh token and returns the decoded JWT claim.
    *
    * @param token
    *   The refresh token to decode.
    * @return
    *   The decoded JWT claim wrapped in an Either. If the token is invalid, an
    *   exception is raised.
    */
  def decodeClaim(token: String): Either[Throwable, JwtClaim] =
    JwtUpickle
      .decode(token, authConfig.secretKey, Seq(JwtAlgorithm.HS256))
      .toEither

  /** Encodes the given JwtClaim and returns the encoded JWT token as a string.
    *
    * @param claim
    *   The JwtClaim to encode.
    * @return
    *   The encoded JWT token as a string.
    */
  def encodeClaim(claim: JwtClaim): String =
    JwtUpickle.encode(claim, authConfig.secretKey, JwtAlgorithm.HS256)

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

/** TokenValidator is responsible for validating JWT tokens. It provides methods
  * for validating the expiration of a claim and checking if a given epoch time
  * is expired based on the current time.
  */
object ClaimValidator:

  /** Checks if the claim has not expired.
    *
    * @param claim
    *   The JWT claim to check.
    * @param now
    *   The current time as an Instant.
    * @return
    *   Either an InvalidTokenError or a Boolean indicating if the claim has not
    *   expired. True if the claim has not expired, false otherwise.
    */
  def claimNotExpired(
      claim: JwtClaim,
      now: Instant
  ): Either[InvalidTokenError, Boolean] =
    extractExpiration(claim).map(expiration => isNotExpired(expiration, now))

  /** Checks if the given expiration time is not expired.
    *
    * @param expiration
    *   The expiration time in seconds.
    * @param now
    *   The current time as an Instant.
    * @return
    *   A Boolean indicating if the expiration time is not expired. True if the
    *   expiration time is not expired, false otherwise.
    */
  private def isNotExpired(expiration: Long, now: Instant): Boolean =
    now.getEpochSecond < expiration

  /** Extracts the expiration time from the JWT claim.
    *
    * @param claim
    *   The JWT claim to extract the expiration time from.
    * @return
    *   Either an InvalidTokenError or the expiration time in seconds.
    */
  private def extractExpiration(
      claim: JwtClaim
  ): Either[InvalidTokenError, Long] =
    claim.expiration.toRight(
      InvalidTokenError("Expiration time not found in claim.")
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

extension (user: UserReadMapper)

  /** Converts a UserReadMapper object to a User object. This method is used for
    * converting the UserReadMapper object to the User domain model. The User
    * domain model is used for the business logic of the application.
    *
    * @return
    *   The converted User object. More or less an isomorphic mapping.
    */
  def toUser: User = User(user.id, user.email, user.password)

  /** Updates the user with the specified email and password. This method is
    * used for updating the user data. The email and password are optional, so
    * they can be updated independently or together.
    *
    * @param email
    *   The new email for the user (optional).
    * @param password
    *   The new password for the user (optional).
    * @return
    *   The updated UserWriteMapper object.
    */
  def updateUser(
      email: Option[String],
      password: Option[String]
  ): UserWriteMapper =
    UserWriteMapper(user.id, email, password)
