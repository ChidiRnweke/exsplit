package exsplit.circles

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth._
import exsplit.circles._

object CirclesEntryPoint:
  def createService[F[_]: MonadThrow](
      circleRepository: CirclesRepository[F],
      userRepository: UserRepository[F]
  ): CirclesServiceImpl[F] =
    CirclesServiceImpl(circleRepository, userRepository)

def withValidCircle[F[_]: MonadThrow, A](
    circleId: CircleId,
    circleRepository: CirclesRepository[F]
)(action: CircleOut => F[A]): F[A] =
  for
    circle <- circleRepository.findCircleById(circleId).rethrow
    result <- action(circle)
  yield result

case class CirclesServiceImpl[F[_]: MonadThrow](
    circleRepository: CirclesRepository[F],
    userRepo: UserRepository[F]
) extends CirclesService[F]:

  def listCirclesForUser(userId: UserId): F[ListCirclesForUserOutput] =
    withValidUser(userId, userRepo): user =>
      for circlesOut <-
          circleRepository.getCirclesForUser(user).map(CirclesOut(_))
      yield ListCirclesForUserOutput(circlesOut)

  def removeUserFromCircle(circleId: CircleId, userId: UserId): F[Unit] =
    withValidUser(userId, userRepo): user =>
      withValidCircle(circleId, circleRepository): circle =>
        for
          _ <- circleRepository.removeUserFromCircle(circle, user)
          // TODO: the user is not allowed to have outstanding debts in the circle
          members <- circleRepository.listCircleMembers(circle)
          _ <- handleEmptyCircle(circleId, members)
        yield ()

  def getCircle(circleId: CircleId): F[GetCircleOutput] =
    circleRepository.findCircleById(circleId).rethrow.map(GetCircleOutput(_))

  def changeDisplayName(
      circleId: CircleId,
      userId: UserId,
      displayName: String
  ): F[Unit] =
    withValidUser(userId, userRepo): user =>
      withValidCircle(circleId, circleRepository): circle =>
        val member = CircleMember(userId, displayName)
        circleRepository.changeDisplayName(member, circle)

  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[Unit] =
    withValidUser(userId, userRepo): user =>
      val member = CircleMember(userId, displayName)
      circleRepository.createCircle(member, circleName, description)

  def addUserToCircle(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    withValidUser(userId, userRepo): user =>
      withValidCircle(circleId, circleRepository): circle =>
        val member = CircleMember(userId, displayName)
        circleRepository.addUserToCircle(member, circle)

  def deleteCircle(circleId: CircleId): F[Unit] =
    // TODO: the circle is not allowed to have outstanding debts
    circleRepository.deleteCircle(circleId)

  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    withValidCircle(circleId, circleRepository): circle =>
      for
        members <- circleRepository.listCircleMembers(circle)
        membersListOut = MembersListOut(members)
      yield ListCircleMembersOutput(membersListOut)

  def updateCircle(
      circleId: CircleId,
      name: Option[String],
      description: Option[String]
  ): F[Unit] =
    withValidCircle(circleId, circleRepository): circle =>
      circleRepository.updateCircle(circle, name, description)

  private def handleEmptyCircle(
      circleId: CircleId,
      members: List[CircleMemberOut]
  ): F[Unit] =
    members match
      case Nil => circleRepository.deleteCircle(circleId)
      case _   => ().pure[F]
