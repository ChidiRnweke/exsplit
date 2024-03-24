import exsplit.auth._
import munit._
import cats.effect._
import cats._
import cats.syntax.all._
import cats.data._
import exsplit.config._
import exsplit.spec._
import cats.effect.std._
import java.util.UUID

class TokenDecoderEncoderSuite extends CatsEffectSuite:
  val authConfig = new AuthConfig[IO]:
    val secretKey = "test".pure[IO]

  val encoderDecoder = TokenEncoderDecoder(authConfig)

  test("Smithy4s prevents non-emails from being parsed without a type error"):
    val expected = IO.fromEither(Email("test@test.com").validate)
    assertIOBoolean(expected.attempt.map(_.isRight))

  test("TokenEncoderDecoder should encode and decode a token without an error"):
    val email = "foo@bar.com"
    val tokenLifespan = TokenLifespan.ShortLived
    val IONow = Clock[IO].realTimeInstant
    val result = for
      now <- IONow
      token = encoderDecoder.makeClaim(tokenLifespan, Email(email), now)
      encoded <- encoderDecoder.encodeClaim(token)
      decoded <- encoderDecoder.decodeClaim(encoded)
    yield decoded.isRight
    assertIOBoolean(result)

  test("You should receive the same email after encoding and decoding"):
    val email = "foo@bar.com"
    val tokenLifespan = TokenLifespan.ShortLived
    val IONow = Clock[IO].realTimeInstant
    val result = for
      now <- IONow
      token = encoderDecoder.makeClaim(tokenLifespan, Email(email), now)
      encoded <- encoderDecoder.encodeClaim(token)
      decoded <- encoderDecoder.decodeClaim(encoded).rethrow
      subject = decoded.subject.getOrElse("Failed to get subject")
    yield assertEquals(subject, email)

  test(
    "You should receive the same token lifespan after encoding and decoding"
  ):
    val email = "foo@bar.com"
    val tokenLifespan = TokenLifespan.ShortLived
    val IONow = Clock[IO].realTimeInstant
    val result = for
      now <- IONow
      token = encoderDecoder.makeClaim(tokenLifespan, Email(email), now)
      encoded <- encoderDecoder.encodeClaim(token)
      decoded <- encoderDecoder.decodeClaim(encoded).rethrow
      expiration = decoded.expiration.get
    yield assertEquals(expiration, token.expiration.get)

  test("A claim with an expiration in the future should be validated"):
    val email = "foo@bar.com"
    val tokenLifespan = TokenLifespan.ShortLived
    val IONow = Clock[IO].realTimeInstant
    val claimValidator = ClaimValidator[IO]()
    val result = for
      now <- IONow
      claim = encoderDecoder.makeClaim(tokenLifespan, Email(email), now)
      potentialError <- claimValidator
        .validateClaimExpiration(claim, now)
        .attempt
    yield assert(potentialError.isRight)

  test("A claim with an expiration in the past should be invalidated"):
    val email = "foo@bar.com"
    val tokenLifespan = TokenLifespan.ShortLived
    val IONow = Clock[IO].realTimeInstant
    val claimValidator = ClaimValidator[IO]()
    val result = for
      now <- IONow
      yesterday = now.minusSeconds(1000)
      claim = encoderDecoder.makeClaim(tokenLifespan, Email(email), yesterday)
      potentialError <- claimValidator
        .validateClaimExpiration(claim, now)
        .attempt
    yield assert(potentialError.isLeft)

  test("hashing a password and checking it should return true"):
    val validator = BCrypt.createValidator[IO]
    val password = "password"
    for
      hashed <- validator.hashPassword(password)
      check = validator.checkPassword(hashed, password)
    yield assert(check)

  test("Checking a password with a different hash should return false"):
    val validator = BCrypt.createValidator[IO]
    val password = "password"
    for
      hashed <- validator.hashPassword(password)
      check = validator.checkPassword(hashed, "wrong")
    yield assert(!check)

  test("Hashing the same password should return different hashes"):
    val validator = BCrypt.createValidator[IO]
    val password = "password"
    for
      hashed1 <- validator.hashPassword(password)
      hashed2 <- validator.hashPassword(password)
    yield assertNotEquals(hashed1, hashed2)

  test("User authenticator should authenticate a user that exists"):
    val userRepository = MockUserRepository()
    val validator = MockValidator()
    val uuid = UUIDGen[IO]
    val authenticator = UserAuthenticator(userRepository, validator, uuid)
    val mail = Email("mail@mail.com")
    val password = Password("password")
    val auth = authenticator.authenticateUser(mail, password)
    assertIOBoolean(auth)

  test("User authenticator fails to authenticate a user that doesn't exist"):
    val userRepository = MockUserRepository()
    val validator = MockValidator()
    val uuid = UUIDGen[IO]
    val authenticator = UserAuthenticator(userRepository, validator, uuid)
    val mail = Email("not-found@mail.com") // This email doesn't exist
    val password = Password("password")
    val failed = authenticator.authenticateUser(mail, password)
    assertIO(failed, false)

  test(
    "User authenticator fails to authenticate a user with the wrong password"
  ):
    val userRepository = MockUserRepository()
    val validator = MockValidator()
    val uuid = UUIDGen[IO]
    val authenticator = UserAuthenticator(userRepository, validator, uuid)
    val mail = Email("mail@mail.com")
    val password = Password("wrong") // This password is wrong
    val failed = authenticator.authenticateUser(mail, password)
    assertIO(failed, false)

  test("Should not create a user that already exists"):
    val userRepository = MockUserRepository()
    val validator = MockValidator()
    val uuid = UUIDGen[IO]
    val authenticator = UserAuthenticator(userRepository, validator, uuid)
    val mail = Email("mail@mail.com") // This email already exists
    val password = Password("password")
    val created = authenticator.registerUser(mail, password)
    assertIOBoolean(created.attempt.map(_.isLeft))

  test("Should create a user that doesn't exist"):
    val userRepository = MockUserRepository()
    val validator = MockValidator()
    val uuid = UUIDGen[IO]
    val authenticator = UserAuthenticator(userRepository, validator, uuid)
    val mail = Email("not-found@mail.com") // This email doesn't exist
    val password = Password("password")
    val created = authenticator.registerUser(mail, password)
    assertIOBoolean(created.attempt.map(_.isRight))

class UserServiceSuite extends CatsEffectSuite:
  val userService: UserService[IO] =
    val authConfig = new AuthConfig[IO]:
      val secretKey = "test".pure[IO]
    val repository = MockUserRepository()
    AuthEntryPoint.createIOService(authConfig, repository)

  test("Should not create a user that already exists"):
    val mail = Email("mail@mail.com") // This email exists
    val password = Password("password")
    val auth = userService.register(mail, password)
    assertIOBoolean(auth.attempt.map(_.isLeft))

  test("Should create a user that does not already exists"):
    val mail = Email("not-found@mail.com") // This email exists
    val password = Password("password")
    val auth = userService.register(mail, password)
    assertIOBoolean(auth.attempt.map(_.isRight))

case class MockValidator() extends PasswordValidator[IO]:
  def hashPassword(password: String): IO[String] = IO.pure(password)
  def checkPassword(password: String, hash: String): Boolean =
    password == hash

case class MockUserRepository() extends UserRepository[IO]:
  def createUser(id: UUID, email: Email, password: String): IO[Unit] = IO.unit

  def findUserByEmail(email: Email): IO[Either[NotFoundError, User]] =
    email.value match
      case "not-found@mail.com" =>
        IO.pure(Left(NotFoundError("User not found")))
      case mail => IO.pure(Right(User("id", mail, "password")))

  def findUserById(userId: UserId): IO[Either[NotFoundError, User]] =
    userId.value match
      case "not-found" => IO.pure(Left(NotFoundError("User not found")))
      case id          => IO.pure(Right(User(id, "email@mail.com", "password")))

  def updateUser(user: User): IO[User] = IO.pure(user)
  def deleteUser(user: User): IO[Unit] = IO.unit
