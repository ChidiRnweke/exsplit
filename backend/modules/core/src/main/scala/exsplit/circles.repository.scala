package exsplit.circles
import exsplit.db._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import cats.data._
import exsplit.auth._

trait CirclesRepository[F[_]]:

  def findCircleById(circleId: CircleId): F[Either[NotFoundError, CircleOut]]

  def getCirclesForUser(user: User): F[List[CircleOut]]

  def listCircleMembers(circleId: CircleOut): F[List[CircleMemberOut]]

  def getCircleMemberById(
      memberId: CircleMemberId
  ): F[Either[NotFoundError, CircleMemberOut]]

  def createCircle(
      user: User,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CircleOut]

  def addUserToCircle(
      user: User,
      displayName: String,
      circle: CircleOut
  ): F[Unit]

  def changeDisplayName(
      member: CircleMemberOut,
      circle: CircleOut,
      newDisplayName: String
  ): F[Unit]

  def removeUserFromCircle(circle: CircleOut, member: CircleMemberOut): F[Unit]

  def deleteCircle(circleId: CircleId): F[Unit]

  def updateCircle(
      circle: CircleOut,
      name: Option[String],
      description: Option[String]
  ): F[Unit]

case class CircleQueryPreparer[F[_]](session: Session[F]):
  val findCircleByIdQuery: F[PreparedQuery[F, String, CircleOut]] =
    val query = sql"""
      SELECT c.id, c.name, c.description
      FROM circles c
      WHERE c.id = $text
    """.query(varchar *: varchar *: varchar).to[CircleOut]
    session.prepare(query)

  val getCircleMemberByIdQuery: F[PreparedQuery[F, String, CircleMemberOut]] =
    val query = sql"""
      SELECT cm.user_id, cm.display_name
      FROM circle_members cm
      WHERE cm.user_id = $text
    """.query(varchar *: varchar).to[CircleMemberOut]
    session.prepare(query)

  val getCirclesForUserQuery: F[PreparedQuery[F, String, CircleOut]] =
    val query = sql"""
      SELECT c.id, c.name, c.description
      FROM circles c
      INNER JOIN circle_members cm ON c.id = cm.circle_id
      WHERE cm.user_id = $text
    """.query(varchar *: varchar *: varchar).to[CircleOut]
    session.prepare(query)

  val listCircleMembersQuery: F[PreparedQuery[F, String, CircleMemberOut]] =
    val query = sql"""
        SELECT cm.user_id, cm.display_name
        FROM circle_members cm
        WHERE cm.circle_id = $text
      """.query(varchar *: varchar).to[CircleMemberOut]
    session.prepare(query)

  val createCircleCommand
      : F[PreparedQuery[F, (String, String, String), CircleOut]] =
    val command = sql"""
      INSERT INTO circles (id, name, description)
      VALUES ($text, $text, $text)
      RETURNING id, name, description
    """.query(varchar *: varchar *: varchar).to[CircleOut]
    session.prepare(command)

  val addUserToCircleCommand: F[PreparedCommand[F, (String, String, String)]] =
    val command = sql"""
      INSERT INTO circle_members (circle_id, user_id, display_name)
      VALUES ($text, $text, $text)
    """.command
    session.prepare(command)

  val changeDisplayNameCommand
      : F[PreparedCommand[F, (String, String, String)]] =
    val command = sql"""
      UPDATE circle_members
      SET display_name = $text
      WHERE circle_id = $text AND user_id = $text
    """.command
    session.prepare(command)

  val removeUserFromCircleCommand: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      DELETE FROM circle_members
      WHERE circle_id = $text AND id = $text
    """.command
    session.prepare(command)

  val deleteCircleCommand: F[PreparedCommand[F, String]] =
    val command = sql"""
      DELETE FROM circles
      WHERE id = $text
    """.command
    session.prepare(command)

  val updateCircleNameAndDescCommand
      : F[PreparedCommand[F, (String, String, String)]] =
    val command = sql"""
      UPDATE circles
      SET name = $text, description = $text
      WHERE id = $text
    """.command
    session.prepare(command)

  val updateCircleNameCommand: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE circles
      SET name = $text
      WHERE id = $text
    """.command
    session.prepare(command)

  val updateCircleDescCommand: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE circles
      SET description = $text
      WHERE id = $text
    """.command
    session.prepare(command)

object CirclesRepository:

  def fromSession[F[_]](
      session: Resource[F, Session[F]]
  )(using F: Concurrent[F]): F[CirclesRepository[F]] =
    session.use: session =>
      val preparer = CircleQueryPreparer(session)
      for
        findCircleByIdQuery <- preparer.findCircleByIdQuery
        getCirclesForUserQuery <- preparer.getCirclesForUserQuery
        listCircleMembersQuery <- preparer.listCircleMembersQuery
        createCircleCommand <- preparer.createCircleCommand
        addUserToCircleCommand <- preparer.addUserToCircleCommand
        changeDisplayNameCommand <- preparer.changeDisplayNameCommand
        removeUserFromCircleCommand <- preparer.removeUserFromCircleCommand
        deleteCircleCommand <- preparer.deleteCircleCommand
        updateCircleNameAndDescCommand <-
          preparer.updateCircleNameAndDescCommand
        updateCircleNameCommand <- preparer.updateCircleNameCommand
        updateCircleDescCommand <- preparer.updateCircleDescCommand
        getCircleMemberByIdQuery <- preparer.getCircleMemberByIdQuery

        circleRepository = new CirclesRepository[F]:
          def findCircleById(
              circleId: CircleId
          ): F[Either[NotFoundError, CircleOut]] =
            val err =
              NotFoundError(s"Circle with id ${circleId.value} not found")
            findCircleByIdQuery
              .option(circleId.value)
              .map(_.toRight(err))

          def getCircleMemberById(
              memberId: CircleMemberId
          ): F[Either[NotFoundError, CircleMemberOut]] =
            getCircleMemberByIdQuery
              .option(memberId.value)
              .map(
                _.toRight(
                  NotFoundError(
                    s"Circle member with id ${memberId.value} not found"
                  )
                )
              )

          def getCirclesForUser(user: User): F[List[CircleOut]] =
            getCirclesForUserQuery.stream(user.id, 1024).compile.toList

          def listCircleMembers(circle: CircleOut): F[List[CircleMemberOut]] =
            listCircleMembersQuery.stream(circle.circleId, 1024).compile.toList

          def createCircle(
              user: User,
              displayName: String,
              circleName: String,
              description: Option[String]
          ): F[CircleOut] =
            val desc = description.getOrElse("")
            createCircleCommand
              .unique(user.id, circleName, desc)

          def addUserToCircle(
              user: User,
              displayName: String,
              circle: CircleOut
          ): F[Unit] =
            addUserToCircleCommand
              .execute(
                (circle.circleId, user.id, displayName)
              )
              .void

          def changeDisplayName(
              member: CircleMemberOut,
              circle: CircleOut,
              newDisplayName: String
          ): F[Unit] =
            changeDisplayNameCommand
              .execute(
                (newDisplayName, circle.circleId, member.circleMemberId)
              )
              .void

          def deleteCircle(circleId: CircleId): F[Unit] =
            deleteCircleCommand.execute(circleId.value).void

          def removeUserFromCircle(
              circle: CircleOut,
              member: CircleMemberOut
          ): F[Unit] =
            removeUserFromCircleCommand
              .execute((circle.circleId, member.circleMemberId))
              .void

          def updateCircle(
              circle: CircleOut,
              name: Option[String],
              description: Option[String]
          ): F[Unit] =
            (name, description) match
              case (Some(n), Some(d)) =>
                updateCircleNameAndDescCommand
                  .execute((n, d, circle.circleId))
                  .void
              case (Some(n), None) =>
                updateCircleNameCommand.execute((n, circle.circleId)).void
              case (None, Some(d)) =>
                updateCircleDescCommand.execute((d, circle.circleId)).void
              case (None, None) =>
                F.unit
      yield circleRepository
