package exsplit.datamapper.circles

import exsplit.db._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._

/** Describes a circle read mapper. This class is a one to one mapping of the
  * circle table in the database without the creation and update timestamps.
  *
  * @param id
  *   The ID of the circle.
  * @param name
  *   The name of the circle.
  * @param description
  *   An optional description of the circle. It is listed as required because it
  *   is read from the database, but it is optional in reality. It will simply
  *   be an empty string if it is not provided.
  */
case class CircleReadMapper(
    id: String,
    name: String,
    description: String
)

/** Represents a CircleWriteMapper, which is used to map data for writing
  * circles. The ID is required, but the name and description are optional
  * because they can be updated independently or together.
  *
  * @param id
  *   The ID of the circle.
  * @param name
  *   An optional name for the circle.
  * @param description
  *   An optional description for the circle.
  */
case class CircleWriteMapper(
    id: String,
    name: Option[String],
    description: Option[String]
)

/** Represents a Circle Member Read Mapper. This class is a one to one mapping
  * of the circle member table in the database without the creation and update
  * timestamps.
  *
  * @param id
  *   The unique identifier of the circle member.
  * @param circleId
  *   The unique identifier of the circle.
  * @param userId
  *   The unique identifier of the user.
  * @param displayName
  *   The display name of the user.
  */
case class CircleMemberReadMapper(
    id: String,
    circleId: String,
    userId: String,
    displayName: String
)

/** Represents a Circle Member Write Mapper. This class is used to map data for
  * writing circle members. The ID is required, but the display name is required
  * because it is the only field that can be updated.
  *
  * @param id
  *   The ID of the circle member.
  * @param displayName
  *   The display name of the circle member.
  */
case class CircleMemberWriteMapper(id: String, displayName: String)

/** Repository trait for managing circles. This trait defines the basic read and
  * write operations for circles. More complex operations are defined elsewhere.
  *
  * @tparam F
  *   The effect type, representing the context in which the repository
  *   operations are executed.
  */
trait CirclesRepository[F[_]]:

  /** Finds a circle by its ID.
    * @param circleId
    *   The ID of the circle to find.
    * @return
    *   An effectful computation that yields either a `NotFoundError` or a
    *   `CircleReadMapper`. The former is returned if the circle is not found.
    */
  def findCircleById(
      circleId: CircleId
  ): F[Either[NotFoundError, CircleReadMapper]]

  /** Updates a circle. The circle is identified by its ID. The name and
    * description are optional because they can be updated independently or
    * together.
    * @param circle
    *   The circle to update.
    * @return
    *   An effectful computation that yields `Unit`.
    */
  def updateCircle(circle: CircleWriteMapper): F[Unit]

  /** Creates a new circle. Adds the user creating the circle as a member of the
    * circle.
    *
    * @param userId
    *   The ID of the user creating the circle.
    * @param displayName
    *   The display name of the circle.
    * @param circleName
    *   The name of the circle.
    * @param description
    *   An optional description of the circle.
    * @return
    *   An effectful computation that yields a `CircleReadMapper`.
    */
  def createCircle(
      userId: UserId,
      displayName: String,
      circleName: String,
      description: Option[String]
  ): F[CircleReadMapper]

  /** Deletes a circle by its ID.
    * @param circleId
    *   The ID of the circle to delete.
    * @return
    *   An effectful computation that yields `Unit`.
    */
  def deleteCircle(circleId: CircleId): F[Unit]

/** Repository trait for managing circle members. Defines the basic read and
  * write operations for circle members. More complex operations are defined
  * elsewhere.
  *
  * @tparam F
  *   the effect type, representing the context in which the operations are
  *   executed
  */
trait CircleMemberRepository[F[_]]:
  /** Finds a circle member by their ID.
    *
    * @param memberId
    *   the ID of the circle member to find
    * @return
    *   an effect that may contain either a `NotFoundError` or a
    *   `CircleMemberReadMapper`. The former is returned if the circle member is
    *   not found.
    */
  def findCircleMemberById(
      memberId: CircleMemberId
  ): F[Either[NotFoundError, CircleMemberReadMapper]]

  /** Adds a new member to the specified circle. A circleReadMapper is required
    * to add a member to a circle because this ensures that the circle exists.
    *
    * @param circle
    *   The circle to add the member to.
    * @param displayName
    *   The display name of the new member.
    * @return
    *   A `CircleMemberReadMapper` representing the newly added member.
    */
  def addCircleMember(
      circle: CircleReadMapper,
      userId: UserId,
      displayName: String
  ): F[CircleMemberReadMapper]

  /** Deletes a circle member.
    *
    * @param memberId
    *   the ID of the circle member to delete
    * @return
    *   an effect that represents the completion of the deletion operation.
    */
  def deleteCircleMember(memberId: CircleMemberId): F[Unit]

  /** Updates a circle member. The display name is the only field that can be
    * updated.
    *
    * @param member
    *   the updated circle member
    * @return
    *   an effect that represents the completion of the update operation
    */
  def updateCircleMember(
      member: CircleMemberWriteMapper
  ): F[Unit]

  /** Lists the members of a circle.
    *
    * @param circleId
    *   the ID of the circle
    * @return
    *   an effect that yields either a `NotFoundError` or a list of
    *   `CircleMemberReadMapper`. The former is returned if the circle is not
    *   found.
    */
trait CircleMembersView[F[_]]:

  /** Retrieves a list of circle members for the specified circle ID.
    *
    * @param circleId
    *   The ID of the circle.
    * @return
    *   A list of CircleMemberReadMapper objects representing the circle
    *   members.
    */
  def listCircleMembers(
      circleId: CircleId
  ): F[List[CircleMemberReadMapper]]

/** A trait representing a view of user circles. This trait defines more complex
  * operations for viewing user circles.
  */
trait UserCircleView[F[_]]:
  /** Retrieves the circles associated with a user.
    *
    * @param userId
    *   The ID of the user.
    * @return
    *   An effectful computation that may result in either a `NotFoundError` or
    *   a list of `CircleReadMapper`. The former is returned if the user is not
    *   found.
    */
  def getCirclesForUser(
      userId: UserId
  ): F[List[CircleReadMapper]]

object CircleMemberRepository:
  def apply[F[_]: Monad](session: Session[F]): F[CircleMemberRepository[F]] =
    val preparer = CircleMemberQueryPreparer(session)
    for
      findCircleMemberByIdQuery <- preparer.findCircleMemberByIdQuery
      addCircleMemberQuery <- preparer.addCircleMemberQuery
      deleteCircleMemberCommand <- preparer.deleteCircleMemberCommand
      updateCircleMemberCommand <- preparer.updateCircleMemberCommand
    yield new CircleMemberRepository[F]:
      def findCircleMemberById(
          memberId: CircleMemberId
      ): F[Either[NotFoundError, CircleMemberReadMapper]] =
        findCircleMemberByIdQuery
          .option(memberId.value)
          .map(
            _.toRight(
              NotFoundError(s"Circle member with ID = $memberId not found.")
            )
          )

      def addCircleMember(
          circle: CircleReadMapper,
          userId: UserId,
          displayName: String
      ): F[CircleMemberReadMapper] =
        addCircleMemberQuery
          .unique(displayName, userId.value, circle.id)

      def deleteCircleMember(memberId: CircleMemberId): F[Unit] =
        deleteCircleMemberCommand.execute(memberId.value).void

      def updateCircleMember(
          member: CircleMemberWriteMapper
      ): F[Unit] =
        updateCircleMemberCommand.execute(member.displayName, member.id).void

object UserCircleView:
  def apply[F[_]: Concurrent](session: Session[F]): F[UserCircleView[F]] =
    val preparer = UserCircleViewPreparer(session)
    for getCirclesForUserQuery <- preparer.getCirclesForUserQuery
    yield new UserCircleView[F]:
      def getCirclesForUser(
          userId: UserId
      ): F[List[CircleReadMapper]] =
        getCirclesForUserQuery
          .stream(userId.value, 1024)
          .compile
          .toList

object CircleMembersView:
  def apply[F[_]: Concurrent](session: Session[F]): F[CircleMembersView[F]] =
    val preparer = CircleMembersViewPreparer(session)
    for listCircleMembersQuery <- preparer.listCircleMembersQuery
    yield new CircleMembersView[F]:
      def listCircleMembers(
          circleId: CircleId
      ): F[List[CircleMemberReadMapper]] =
        listCircleMembersQuery
          .stream(circleId.value, 1024)
          .compile
          .toList

object CirclesRepository:
  def apply[F[_]: Monad](session: Session[F]): F[CirclesRepository[F]] =
    val preparer = CircleQueryPreparer(session)
    for
      findCircleByIdQuery <- preparer.findCircleByIdQuery
      updateCircleQuery <- preparer.updateCircleQuery
      updateCircleNameCommand <- preparer.updateCircleNameCommand
      updateCircleDescQuery <- preparer.updateCircleQueryDescription
      createCircleQuery <- preparer.createCircleQuery
      deleteCircleQuery <- preparer.deleteCircleQuery
    yield new CirclesRepository[F]:
      def findCircleById(
          circleId: CircleId
      ): F[Either[NotFoundError, CircleReadMapper]] =
        findCircleByIdQuery
          .option(circleId.value)
          .map(
            _.toRight(
              NotFoundError(s"Circle with ID = $circleId not found.")
            )
          )

      def updateCircle(circle: CircleWriteMapper): F[Unit] =
        circle match
          case CircleWriteMapper(id, Some(name), Some(description)) =>
            updateCircleQuery.execute(name, description, id).void
          case CircleWriteMapper(id, Some(name), None) =>
            updateCircleNameCommand.execute(name, id).void
          case CircleWriteMapper(id, None, Some(description)) =>
            updateCircleDescQuery.execute(description, id).void
          case _ => ().pure[F]
      def createCircle(
          userId: UserId,
          displayName: String,
          circleName: String,
          description: Option[String]
      ): F[CircleReadMapper] =
        createCircleQuery
          .unique(circleName, description.getOrElse(""), userId.value)

      def deleteCircle(circleId: CircleId): F[Unit] =
        deleteCircleQuery.execute(circleId.value).void

case class CircleMemberQueryPreparer[F[_]](session: Session[F]):
  def findCircleMemberByIdQuery
      : F[PreparedQuery[F, String, CircleMemberReadMapper]] =
    val query = sql"""
      SELECT cm.id, cm.circle_id, cm.user_id, cm.display_name
      FROM circle_members cm
      WHERE cm.id = $text
    """
      .query(varchar *: varchar *: varchar *: varchar)
      .to[CircleMemberReadMapper]
    session.prepare(query)

  def addCircleMemberQuery: F[
    PreparedQuery[F, (String, String, String), CircleMemberReadMapper]
  ] =
    val query = sql"""
      INSERT INTO circle_members (display_name, user_id, circle_id)
      VALUES ($text, $text, $text)
      RETURNING id, circle_id, user_id, display_name
    """
      .query(varchar *: varchar *: varchar *: varchar)
      .to[CircleMemberReadMapper]
    session.prepare(query)

  def deleteCircleMemberCommand: F[PreparedCommand[F, String]] =
    val command = sql"""
      DELETE FROM circle_members
      WHERE id = $text
    """.command
    session.prepare(command)

  def listCircleMembersQuery
      : F[PreparedQuery[F, String, CircleMemberReadMapper]] =
    val query = sql"""
      SELECT cm.id, cm.circle_id, cm.user_id, cm.display_name
      FROM circle_members cm
      WHERE cm.circle_id = $text
    """
      .query(varchar *: varchar *: varchar *: varchar)
      .to[CircleMemberReadMapper]
    session.prepare(query)

  def updateCircleMemberCommand: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE circle_members
      SET display_name = $text
      WHERE id = $text
    """.command
    session.prepare(command)

case class CircleQueryPreparer[F[_]](session: Session[F]):
  def findCircleByIdQuery: F[PreparedQuery[F, String, CircleReadMapper]] =
    val query = sql"""
      SELECT c.id, c.name, c.description
      FROM circles c
      WHERE c.id = $text
    """
      .query(varchar *: varchar *: varchar)
      .to[CircleReadMapper]
    session.prepare(query)

  def updateCircleNameCommand: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE circles
      SET name = $text
      WHERE id = $text
    """.command
    session.prepare(command)

  def updateCircleQueryDescription: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE circles
      SET description = $text
      WHERE id = $text
    """.command
    session.prepare(command)

  def updateCircleQuery: F[PreparedCommand[F, (String, String, String)]] =
    val command = sql"""
      UPDATE circles
      SET name = $text, description = $text
      WHERE id = $text
    """.command
    session.prepare(command)

  def createCircleQuery
      : F[PreparedQuery[F, (String, String, String), CircleReadMapper]] =
    val command = sql"""
      INSERT INTO circles (id, name, description)
      VALUES ($text, $text, $text)
      RETURNING id, name, description
    """
      .query(varchar *: varchar *: varchar)
      .to[CircleReadMapper]
    session.prepare(command)

  def deleteCircleQuery: F[PreparedCommand[F, String]] =
    val command = sql"""
      DELETE FROM circles
      WHERE id = $text
    """.command
    session.prepare(command)

case class UserCircleViewPreparer[F[_]](session: Session[F]):
  def getCirclesForUserQuery: F[PreparedQuery[F, String, CircleReadMapper]] =
    val query = sql"""
      SELECT c.id, c.name, c.description
      FROM circles c
      JOIN circle_members cm ON c.id = cm.circle_id
      WHERE cm.user_id = $text
    """
      .query(varchar *: varchar *: varchar)
      .to[CircleReadMapper]
    session.prepare(query)

case class CircleMembersViewPreparer[F[_]](session: Session[F]):
  def listCircleMembersQuery
      : F[PreparedQuery[F, String, CircleMemberReadMapper]] =
    val query = sql"""
      SELECT cm.id, cm.circle_id, cm.user_id, cm.display_name
      FROM circle_members cm
      WHERE cm.circle_id = $text
    """
      .query(varchar *: varchar *: varchar *: varchar)
      .to[CircleMemberReadMapper]
    session.prepare(query)
