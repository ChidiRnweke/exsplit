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
import exsplit.domainmapper._

object CirclesEntryPoint:
  def fromSession[F[_]: Concurrent](
      userInfo: F[Email],
      session: Session[F]
  ): F[CirclesService[F]] =
    for
      circlesRepo <- CirclesRepository.fromSession(session)
      circleMembersRepo <- CircleMembersRepository.fromSession(session)
      userRepo <- UserMapper.fromSession(session)
    yield CirclesServiceImpl(userInfo, circlesRepo, circleMembersRepo, userRepo)

case class CirclesServiceImpl[F[_]: MonadThrow](
    userInfo: F[Email],
    circlesRepo: CirclesRepository[F],
    circleMembersRepo: CircleMembersRepository[F],
    userRepo: UserMapper[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[CirclesOut] =
    withValidUser(userId, userRepo): user =>
      for circles <- circlesRepo.getCirclesOut(userId)
      yield circles

  def removeMemberFromCircle(
      circleId: CircleId,
      member: CircleMemberId
  ): F[Unit] =
    circlesRepo.withValidCircle(circleId): _ =>
      circleMembersRepo.withValidCircleMember(member): _ =>
        for
          _ <- circleMembersRepo.delete(member)
          // TODO: the user is not allowed to have outstanding debts in the circle
          members <- circleMembersRepo.getCircleMembersOuts(circleId)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circlesRepo.getCircleOut(circleId).map(GetCircleOutput(_))

  def changeDisplayName(
      circleId: CircleId,
      memberId: CircleMemberId,
      displayName: String
  ): F[Unit] =
    circleMembersRepo.withValidCircleMember(memberId): member =>
      circlesRepo.withValidCircle(circleId): circle =>
        val write = CircleMemberWriteMapper(memberId.value, displayName)
        circleMembersRepo.update(write)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CreateCircleOutput] =
    withValidUser(userId, userRepo): user =>
      circlesRepo
        .createCircle(userId, displayName, circleName, description)
        .map(CreateCircleOutput(_))

  def addUserToCircle(
      user: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(user, userRepo): validUser =>
      circlesRepo.withValidCircle(circleId): circle =>
        circleMembersRepo.addCircleMember(user, displayName, circleId).void

  def deleteCircle(circleId: CircleId): F[Unit] =
    // TODO: the circle is not allowed to have outstanding debts
    circlesRepo.delete(circleId)

  def listCircleMembers(circleId: CircleId): F[MembersListOut] =
    circlesRepo.withValidCircle(circleId): circle =>
      for members <- circleMembersRepo.getCircleMembersOuts(circleId)
      yield MembersListOut(members)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    circlesRepo.withValidCircle(circleId): circle =>
      val write = CircleWriteMapper(circleId.value, name, description)
      circlesRepo.update(write)

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circlesRepo.delete(circleId)
      case _   => ().pure[F]
