import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._
import exsplit.spec.UserId
import exsplit.datamapper.user._
import exsplit.auth.AuthEntryPoint

class UserServiceSuite extends DatabaseSuite:
  val authConfig = readConfig[AppConfig]("application.conf").auth

  test("You should be able to create an account."):
    val session = sessionPool()
    val email = Email("test@test.com")
    val password = Password("password")
    val userService = AuthEntryPoint.fromSession(session, authConfig)

    for user <- userService.register(email, password).attempt
    yield assertEquals(user.isRight, true)

  test("You should not be able to create an account with the same email."):
    val session = sessionPool()
    val email = Email("test2@test.com")
    val password = Password("password")
    val userService = AuthEntryPoint.fromSession(session, authConfig)

    for
      user <- userService.register(email, password)
      user2 <- userService.register(email, password).attempt
    yield assertEquals(user2.isLeft, true)

  test("You should be able to login with the correct credentials."):
    val session = sessionPool()
    val email = Email("test3@test.com")
    val password = Password("password")
    val userService = AuthEntryPoint.fromSession(session, authConfig)

    for
      user <- userService.register(email, password)
      login <- userService.login(email, password).attempt
    yield assertEquals(login.isRight, true)

  test("You should not be able to login with the wrong credentials."):
    val session = sessionPool()
    val email = Email("test4@test.com")
    val password = Password("password")
    val wrongPassword = Password("wrongPassword")
    val userService = AuthEntryPoint.fromSession(session, authConfig)

    for
      user <- userService.register(email, password)
      login <- userService.login(email, wrongPassword).attempt
    yield assertEquals(login.isLeft, true)

  test("You should not be able to create an account with an invalid email."):
    val session = sessionPool()
    val email = Email("test5")
    val password = Password("password")
    val userService = AuthEntryPoint.fromSession(session, authConfig)

    for user <- userService.register(email, password).attempt
    yield assertEquals(user.isLeft, true)
