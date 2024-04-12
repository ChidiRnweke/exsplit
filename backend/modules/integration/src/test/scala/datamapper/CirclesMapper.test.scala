import cats.effect._
import cats.syntax.all._
import cats.data._
import cats._
import exsplit.config._
import exsplit.integration._
import exsplit.spec._
import exsplit.datamapper.circles._
import exsplit.datamapper.user._
import cats.effect.std.UUIDGen

class CirclesRepositorySuite extends DatabaseSuite:
  test("It is possible to write a circle to the database"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("1"), "User", "Test Circle")
    val result = session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        result <- circlesRepo.create(input)
      yield result match
        case CircleReadMapper(_, "Test Circle", "") => true
        case _                                      => false
    )
    result.assert

  test("It is possible to write and then read a circle from the database"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("2"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.create(input)
        circleRead <- circlesRepo.get(CircleId(createdCircle.id))
      yield assert(circleRead.isRight)
    )

  test("If you write and then read a circle, they should be the same"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.create(input)
        circleRead <- circlesRepo.get(CircleId(createdCircle.id)).rethrow
      yield assertEquals(createdCircle, circleRead)
    )

  test("You should be able to write and then delete a circle"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.create(input)
        _ <- circlesRepo.delete(CircleId(createdCircle.id))
        circleRead <- circlesRepo.get(CircleId(createdCircle.id))
      yield assert(circleRead.isLeft)
    )

  test("You should be to update a circle's name"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.create(input)
        circleWrite = CircleWriteMapper(
          createdCircle.id,
          Some("Updated Circle"),
          None
        )
        _ <- circlesRepo.update(circleWrite)
        circleRead <- circlesRepo.get(CircleId(createdCircle.id)).rethrow
      yield assertEquals(circleRead.name, "Updated Circle")
    )

  test("You should be able to update a circle's description"):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.create(input)
        circleWrite = CircleWriteMapper(
          createdCircle.id,
          None,
          Some("Updated Description")
        )
        _ <- circlesRepo.update(circleWrite)
        circleRead <- circlesRepo.get(CircleId(createdCircle.id)).rethrow
      yield assertEquals(circleRead.description, "Updated Description")
    )

  test(
    "You should be able to update a circle's name and description at the same time"
  ):
    val session = sessionPool()
    val input = CreateCircleInput(UserId("3"), "User", "Test Circle")
    val result = session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        createdCircle <- circlesRepo.create(input)
        circleWrite = CircleWriteMapper(
          createdCircle.id,
          Some("Updated Circle"),
          Some("Updated Description")
        )
        _ <- circlesRepo.update(circleWrite)
        circleRead <- circlesRepo.get(CircleId(createdCircle.id)).rethrow
      yield circleRead
    )
    assertIO(result.map(_.name), "Updated Circle") >> assertIO(
      result.map(_.description),
      "Updated Description"
    )

  test("You should be able to list all from a user"):
    val session = sessionPool()
    val input1 = CreateCircleInput(UserId("4"), "User", "Test Circle")
    val input2 = CreateCircleInput(UserId("4"), "User", "Test Circle 2")
    val result = session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        _ <- circlesRepo.create(input1)
        _ <- circlesRepo.create(input2)
        circles <- circlesRepo.listPrimaries(UserId("4"))
      yield assertEquals(circles.length, 2)
    )

class CircleMembersRepositorySuite extends DatabaseSuite:
  test("You should be able to add a member to a circle"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("5"), "User", "Test Circle")
    val testUser = (UserId("5"), "User", "User")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        createdMember <- circleMembersRepo.create(createUser).attempt
      yield assertEquals(createdMember.isRight, true)
    )

  test("You should be able to add a member to a circle and then read it"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("6"), "User", "Test Circle")
    val testUser = (UserId("6"), "User", "User")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        member <- circleMembersRepo.create(createUser)
        readMember <- circleMembersRepo
          .get(CircleMemberId(member.id))
          .rethrow
      yield assertEquals(readMember, member)
    )

  test("You should be able to add a member to a circle and then delete it"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("7"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        member <- circleMembersRepo.create(createUser)
        _ <- circleMembersRepo.delete(CircleMemberId(member.id))
        readMember <- circleMembersRepo
          .get(CircleMemberId(member.id))
      yield assert(readMember.isLeft)
    )

  test("You should be able to add a member to a circle and then update it"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("8"), "User", "Test Circle")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        member <- circleMembersRepo.create(createUser)
        updatedMember = CircleMemberWriteMapper(
          member.id,
          "Updated User"
        )
        _ <- circleMembersRepo.update(updatedMember)
        readMember <- circleMembersRepo
          .get(CircleMemberId(member.id))
          .rethrow
      yield assertEquals(readMember.displayName, "Updated User")
    )

  test("You should be able to list all members from a circle"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("9"), "User", "Test Circle")
    val testUser = (UserId("9"), "User", "User")
    session.use(session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        createdCircle <- circlesRepo.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        createUser = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(createdCircle.id)
        )
        _ <- circleMembersRepo.create(createUser)
        members <- circleMembersRepo.listChildren(
          CircleId(createdCircle.id)
        )
      yield assertEquals(members.length, 1)
    )

  test("You should be able to list all circles a user is in"):
    val session = sessionPool()
    val testCircle = CreateCircleInput(UserId("10"), "User", "Test Circle")
    val testUser = (UserId("10"), "User", "User")
    session.use: session =>
      for
        circlesRepo <- CirclesRepository.fromSession(session)
        circleMembersRepo <- CircleMembersRepository.fromSession(session)
        circle1 <- circlesRepo.create(testCircle)
        circle2 <- circlesRepo.create(testCircle)
        userRepo <- UserMapper.fromSession(session)
        uuid <- UUIDGen[IO].randomUUID
        _ <- userRepo.createUser(uuid, Email("foo@bar.com"), "password")
        member1 = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(circle1.id)
        )
        member2 = AddUserToCircleInput(
          UserId(uuid.toString()),
          "User",
          CircleId(circle2.id)
        )
        member1 <- circleMembersRepo.create(member1)
        member2 <- circleMembersRepo.create(member2)
        circles <- circleMembersRepo.byUserId(UserId(uuid.toString()))
        expected = List(member1.id, member2.id).sorted
        obtained = circles.map(_.id).sorted
      yield assertEquals(obtained, expected)
