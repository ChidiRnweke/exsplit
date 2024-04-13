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
import exsplit.auth.AuthEntryPoint
import skunk.Session
import exsplit.circles.CirclesEntryPoint
import cats.effect.std.Random
import exsplit.database.AppSessionPool

class serviceSuite extends DatabaseSuite:
  val authConfig = readConfig[AppConfig]("application.conf").auth
  val name = "Test Circle"
  val displayName = "Test Display Name"

  def makeUser(session: AppSessionPool[IO], number: Int): IO[UserId] =
    val password = Password("password")
    val email = Email(s"test$number@mail.com")
    val userService = AuthEntryPoint.fromSession(session, authConfig)

    for user <- userService.register(email, password)
    yield UserId(user.userId)

  test("You should be able to create a circle."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)

    for
      userId <- makeUser(session, 1)
      circle <- service.createCircle(userId, displayName, name)
    yield assertEquals(circle.circle.circleName, "Test Circle")

  test("You should be able to get a circle."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 2)
      expected <- service.createCircle(userId, name, displayName)
      obtained <- service.getCircle(CircleId(expected.circle.circleId))
    yield assertEquals(obtained.circle, expected.circle)

  test("You should be able to list circles for a user."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 3)
      _ <- service.createCircle(userId, name, displayName)
      _ <- service.createCircle(userId, name, displayName)
      _ <- service.createCircle(userId, name, displayName)
      obtained <- service.listCirclesForUser(userId)
    yield assertEquals(obtained.circles.length, 3)

  test("You should be able to add a member to a circle."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 4)
      userId2 <- makeUser(session, 5)
      circle <- service.createCircle(userId, name, displayName)
      circleId = CircleId(circle.circle.circleId)
      _ <- service.addUserToCircle(userId2, "test2", circleId)
      members <- service.listCircleMembers(circleId)
    yield assertEquals(members.members.length, 2)

  test("You should be able to remove a member from a circle."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)

    for
      userId <- makeUser(session, 6)
      userId2 <- makeUser(session, 7)
      circle <- service.createCircle(userId, name, displayName)
      circleId = CircleId(circle.circle.circleId)
      _ <- service.addUserToCircle(userId2, "test2", circleId)
      member <- service.listCircleMembers(circleId).map(_.members.head)
      memberId = CircleMemberId(member.circleMemberId)
      _ <- service.removeMemberFromCircle(circleId, memberId)
      members <- service.listCircleMembers(circleId)
    yield assertEquals(members.members.length, 1)

  test("When you remove the last member the circle should disappear."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 8)
      circle <- service.createCircle(userId, name, displayName)
      circleId = CircleId(circle.circle.circleId)
      members <- service.listCircleMembers(circleId)
      memberId = CircleMemberId(members.members.head.circleMemberId)
      _ <- service.removeMemberFromCircle(circleId, memberId)
      obtained <- service.getCircle(circleId).attempt
    yield assertEquals(obtained.isLeft, true)

  test("You should be able to change the display name of a member."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 9)
      circle <- service.createCircle(userId, name, displayName)
      circleId = CircleId(circle.circle.circleId)
      member <- service.listCircleMembers(circleId).map(_.members.head)
      memberId = CircleMemberId(member.circleMemberId)
      _ <- service.changeDisplayName(circleId, memberId, "new name")
      members <- service.listCircleMembers(circleId)
    yield assertEquals(members.members.head.displayName, "new name")

  test("You should be able to delete a circle."):
    val session = sessionPool()
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 11)
      circle <- service.createCircle(userId, name, displayName)
      circleId = CircleId(circle.circle.circleId)
      obtained <- service.deleteCircle(circleId).attempt
    yield assertEquals(obtained.isRight, true)

  test("You should be able to change the display name of a circle."):
    val session = sessionPool()
    val newName = "new name"
    val newDesc = "new description"
    val expected = (newName, newDesc)
    val service = CirclesEntryPoint.fromSession(session)
    for
      userId <- makeUser(session, 12)
      circle <- service.createCircle(userId, name, displayName)
      circleId = CircleId(circle.circle.circleId)
      _ <- service.updateCircle(circleId, Some(newName), Some(newDesc))
      circle <- service.getCircle(circleId)
      obtained = (circle.circle.circleName, circle.circle.description)
    yield assertEquals(obtained, expected)
