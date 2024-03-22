import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth.User

object CirclesEntryPoint:
  def createService[F[_]: Functor](
      repo: CirclesRepository[F]
  ): CirclesServiceImpl[F] =
    CirclesServiceImpl(repo)

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

  def deleteCircle(circleId: CircleId): F[Unit] =
    repo.deleteCircle(circleId)
  def listCircleMembers(circleId: CircleId): F[ListCircleMembersOutput] =
    repo.listCircleMembers(circleId).map(ListCircleMembersOutput(_))
  def updateCircle(
      circleId: CircleId,
      name: String,
      description: Option[String]
  ): F[Unit] =
    repo.updateCircle(circleId, name, description)

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

  def deleteCircle(circleId: CircleId): F[Unit]

  def listCircleMembers(circleId: CircleId): F[List[CircleMember]]

  def updateCircle(
      circleId: CircleId,
      name: String,
      description: Option[String]
  ): F[Unit]

object CirclesRepository:
  import exsplit.db._
  import skunk._
  import skunk.implicits._
  import skunk.codec.all._
  import natchez.Trace.Implicits.noop

  def fromSession[F[_]: Async](
      session: Resource[F, Session[F]]
  ): CirclesRepository[F] =
    new CirclesRepository[F] with SkunkRepository[F](session):
      def getCirclesForUser(userId: UserId): F[List[Circle]] =

        val query = sql"""
          SELECT c.id, c.name, c.description
          FROM circles c
          JOIN circle_members cm ON c.id = cm.circle_id
          WHERE cm.user_id = $text
        """.query(varchar *: varchar *: varchar).to[Circle]
        ???

      ???
  ???
