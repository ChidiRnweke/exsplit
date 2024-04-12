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
import exsplit.authorization.extractors._
import exsplit.authorization._

object CirclesEntryPoint:
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[CirclesService[F]] =
    (
      CirclesRepository.fromSession(session),
      CircleMembersRepository.fromSession(session),
      UserMapper.fromSession(session)
    ).mapN(CirclesServiceImpl(_, _, _))

case class CirclesServiceImpl[F[_]: MonadThrow](
    circlesRepo: CirclesRepository[F],
    circleMembersRepo: CircleMembersRepository[F],
    userRepo: UserMapper[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[CirclesOut] =
    withValidUser(userId, userRepo): user =>
      circlesRepo.getCirclesOut(userId)

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
      circleMembersRepo.getCircleMembersOuts(circleId).map(MembersListOut(_))

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    circlesRepo.withValidCircle(circleId): circle =>
      circlesRepo.update(CircleWriteMapper(circleId.value, name, description))

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circlesRepo.delete(circleId)
      case _   => ().pure[F]
