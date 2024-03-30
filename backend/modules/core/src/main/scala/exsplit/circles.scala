package exsplit.circles

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth._
import exsplit.circles._
import exsplit.datamapper.user._
import exsplit.datamapper.circles._
import skunk.Session

object CirclesEntryPoint:
  def createService[F[_]: MonadThrow](
      circleRepository: CirclesMapper[F],
      circleMemberRepository: CircleMemberMapper[F],
      circleToMembersRepository: CircleToMembersMapper[F],
      userCirclesMapper: UserCirclesMapper[F],
      userRepository: UserMapper[F]
  ): CirclesServiceImpl[F] =
    CirclesServiceImpl(
      circleRepository,
      circleMemberRepository,
      circleToMembersRepository,
      userCirclesMapper,
      userRepository
    )

  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[CirclesService[F]] =
    for
      circlesMapper <- CirclesMapper.fromSession(session)
      circleMemberMapper <- CircleMemberMapper.fromSession(session)
      circleToMembersMapper <- CircleToMembersMapper.fromSession(session)
      userCirclesMapper <- UserCirclesMapper.fromSession(session)
      userMapper <- UserMapper.fromSession(session)
    yield CirclesServiceImpl(
      circlesMapper,
      circleMemberMapper,
      circleToMembersMapper,
      userCirclesMapper,
      userMapper
    )

def withValidCircle[F[_]: MonadThrow, A](
    circleId: CircleId,
    circleRepository: CirclesMapper[F]
)(action: CircleOut => F[A]): F[A] =
  for
    circleRead <- circleRepository.get(circleId.value).rethrow
    circle = circleRead.toCircleOut
    result <- action(circle)
  yield result

def withValidCircleMember[F[_]: MonadThrow, A](
    memberId: CircleMemberId,
    circleMemberRepository: CircleMemberMapper[F]
)(action: CircleMemberOut => F[A]): F[A] =
  for
    memberRead <- circleMemberRepository.get(memberId.value).rethrow
    member = memberRead.toCircleMemberOut
    result <- action(member)
  yield result

case class CirclesServiceImpl[F[_]: MonadThrow](
    circleRepository: CirclesMapper[F],
    circleMemberRepository: CircleMemberMapper[F],
    circleToMembersRepository: CircleToMembersMapper[F],
    userCirclesMapper: UserCirclesMapper[F],
    userRepo: UserMapper[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[ListCirclesForUserOutput] =
    withValidUser(userId, userRepo): user =>
      for
        circlesRead <- userCirclesMapper.listPrimaries(userId)
        circles = CirclesOut(circlesRead.map(_.toCircleOut))
      yield ListCirclesForUserOutput(circles)

  def removeMemberFromCircle(
      circleId: CircleId,
      member: CircleMemberId
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): _ =>
      withValidCircleMember(member, circleMemberRepository): _ =>
        for
          _ <- circleMemberRepository.delete(member.value)
          // TODO: the user is not allowed to have outstanding debts in the circle
          membersRead <- circleToMembersRepository.listChildren(circleId.value)
          members = membersRead.map(_.toCircleMemberOut)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circleRepository
      .get(circleId.value)
      .rethrow
      .map(out => GetCircleOutput(out.toCircleOut))

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    withValidCircleMember(memberId, circleMemberRepository): member =>
      withValidCircle(circleId, circleRepository): circle =>
        val write = CircleMemberWriteMapper(memberId.value, displayName)
        circleMemberRepository.update(write)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    withValidUser(userId, userRepo): user =>
      val input =
        CreateCircleInput(userId, displayName, circleName, description)
      circleRepository
        .create(input)
        .map(_.toCircleOut)
        .map(CreateCircleOutput(_))

  def addUserToCircle(
      user: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(user, userRepo): validUser =>
      withValidCircle(circleId, circleRepository): circle =>
        val input = AddUserToCircleInput(user, displayName, circleId)
        circleMemberRepository.create(input).void

  def deleteCircle(circleId: CircleId): F[Unit] =
    // TODO: the circle is not allowed to have outstanding debts
    circleRepository.delete(circleId.value)

  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    withValidCircle(circleId, circleRepository): circle =>
      for
        membersWrite <- circleToMembersRepository.listChildren(circleId.value)
        members = membersWrite.map(_.toCircleMemberOut)
        membersListOut = MembersListOut(members)
      yield ListCircleMembersOutput(membersListOut)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): circle =>
      val write = CircleWriteMapper(circleId.value, name, description)
      circleRepository.update(write)

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circleRepository.delete(circleId.value)
      case _   => ().pure[F]

extension (circle: CircleReadMapper)
  def toCircleOut: CircleOut = CircleOut(
    circle.id,
    circle.name,
    circle.description
  )

extension (member: CircleMemberReadMapper)
  def toCircleMemberOut: CircleMemberOut = CircleMemberOut(
    member.id,
    member.displayName
  )
