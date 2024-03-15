package exsplit.circles

import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth.User

case class CirclesServiceImpl[F[_]: Functor](repo: CirclesRepository[F])
    extends CirclesService[F]:
  def getCircles(userId: UserId): F[GetCirclesOutput] =
    repo.getCirclesForUser(userId).map(GetCirclesOutput(_))
  def createCircle(
      userId: UserId,
      name: String,
      description: Option[String]
  ): F[Unit] =
    repo.createCircle(userId, name, description)
  def addUserToCircle(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit] =
    repo.addUserToCircle(userId, displayName, circleId)

  def deleteCircle(userId: UserId, circleId: CircleId): F[Unit] =
    repo.deleteCircle(userId, circleId)
  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    repo.listCircleMembers(circleId).map(ListCircleMembersOutput(_))
  def updateCircle(
      userId: UserId,
      circleId: CircleId,
      name: String,
      description: Option[String]
  ): F[Unit] =
    repo.updateCircle(userId, circleId, name, description)

trait CirclesRepository[F[_]]:
  def getCirclesForUser(userId: UserId): F[List[Circle]]

  def createCircle(
      userId: UserId,
      name: String,
      description: Option[String]
  ): F[Unit]

  def addUserToCircle(
      userId: UserId,
      displayName: String,
      circleId: CircleId
  ): F[Unit]

  def deleteCircle(userId: UserId, circleId: CircleId): F[Unit]

  def listCircleMembers(circleId: CircleId): F[List[CircleMember]]

  def updateCircle(
      userId: UserId,
      circleId: CircleId,
      name: String,
      description: Option[String]
  ): F[Unit]
