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
import exsplit.datamapper.user._

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

/** A trait representing a view for circle members. This trait defines more
  * complex operations for viewing circle members.
  *
  * @tparam F
  *   the effect type
  */
trait CircleMemberView[F[_]]:
  /** Lists the members of a circle.
    *
    * @param circleId
    *   the ID of the circle
    * @return
    *   an effect that yields either a `NotFoundError` or a list of
    *   `CircleMemberReadMapper`. The former is returned if the circle is not
    *   found.
    */
  def listCircleMembers(
      circleId: CircleId
  ): F[Either[NotFoundError, List[CircleMemberReadMapper]]]

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
  ): F[Either[NotFoundError, List[CircleReadMapper]]]

object CircleMemberRepository:
  def apply[F[_]: Monad](session: Session[F]): F[CircleMemberRepository[F]] =
    val preparer = CircleMemberQueryPreparer(session)
    for
      findCircleMemberByIdQuery <- preparer.findCircleMemberByIdQuery
      addCircleMemberQuery <- preparer.addCircleMemberQuery
      deleteCircleMemberQuery <- preparer.deleteCircleMemberQuery
      updateCircleMemberQuery <- preparer.updateCircleMemberQuery
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
        deleteCircleMemberQuery.execute(memberId.value).void

      def updateCircleMember(
          member: CircleMemberWriteMapper
      ): F[Unit] =
        updateCircleMemberQuery.execute(member.displayName, member.id).void

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

  def deleteCircleMemberQuery: F[PreparedCommand[F, String]] =
    val command = sql"""
      DELETE FROM circle_members
      WHERE id = $text
    """.command
    session.prepare(command)

  def updateCircleMemberQuery: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE circle_members
      SET display_name = $text
      WHERE id = $text
    """.command
    session.prepare(command)
