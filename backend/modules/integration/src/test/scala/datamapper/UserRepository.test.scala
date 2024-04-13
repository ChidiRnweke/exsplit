import cats.effect._
import cats.syntax.all._
import cats.effect.std.UUIDGen
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._
import exsplit.spec.UserId
import exsplit.datamapper.user._

class UserRepositorySuite extends DatabaseSuite:
  test("You should be able to create a user in the database"):
    val session = sessionPool()
    val userRepo = UserMapper.fromSession(session)

    for
      uuid <- UUIDGen[IO].randomUUID
      result <- userRepo
        .createUser(uuid, Email("user@foo.com"), "password")
        .attempt
    yield assertEquals(result.isRight, true)

  test("You should be able to read a user from the database"):
    val session = sessionPool()
    val userRepo = UserMapper.fromSession(session)
    for
      uuid <- UUIDGen[IO].randomUUID
      _ <- userRepo.createUser(uuid, Email("user@foo.com"), "password")
      obtained <- userRepo.get(uuid.toString()).rethrow
      expected = UserReadMapper(uuid.toString(), "user@foo.com", "password")
    yield assertEquals(obtained, expected)

  test("You should be able to delete a user from the database"):
    val session = sessionPool()
    val userRepo = UserMapper.fromSession(session)
    for
      uuid <- UUIDGen[IO].randomUUID
      _ <- userRepo.createUser(uuid, Email("foo@foo.com"), "password")
      _ <- userRepo.delete(uuid.toString())
      obtained <- userRepo.get(uuid.toString()) // The user should not exist
    yield assertEquals(obtained.isLeft, true)

  test("You should be able to update a user in the database"):
    val session = sessionPool()
    val userRepo = UserMapper.fromSession(session)
    for
      uuid <- UUIDGen[IO].randomUUID
      _ <- userRepo.createUser(uuid, Email("foo@foo.com"), "password")
      userWrite = UserWriteMapper(
        uuid.toString(),
        Some("foo@bar.com"),
        Some("newpassword")
      )
      _ <- userRepo.update(userWrite)
      obtained <- userRepo.get(uuid.toString()).rethrow
      expected = UserReadMapper(uuid.toString(), "foo@bar.com", "newpassword")
    yield assertEquals(obtained, expected)
