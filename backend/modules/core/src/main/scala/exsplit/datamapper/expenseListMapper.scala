package exsplit.datamapper.expenseList

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
import java.util.UUID
import exsplit.datamapper._

/** Represents a repository for managing expense lists. The main mapper contains
  * the basic CRUD operations for the expense list. The circles mapper contains
  * operations for finding expense lists that are children of a specified parent
  * circle.
  *
  * @param mainMapper
  *   The main expense list mapper.
  * @param circlesMapper
  *   The circles expense list mapper.
  */
trait ExpenseListRepository[F[_]]:
  val main: ExpenseListMapper[F]
  val byCircle: CirclesExpenseListMapper[F]

/* Companion object for the `ExpenseListRepository` trait. Provides a method for
 * creating a new instance of the repository.
 */
object ExpenseListRepository:
  /*
   * Creates a new instance of `ExpenseListRepository` using the provided
   * session. This method is effectful and returns a `F[ExpenseListRepository[F]]`
   * because it requires database operations. It uses prepared statements. This
   * means it needs to be run inside a session.
   *
   * @param session
   *   The session to be used for database operations.
   * @return
   *   A `F[ExpenseListRepository[F]]` representing the created repository
   *   instance.
   */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ExpenseListRepository[F]] =
    for
      mainMapper <- ExpenseListMapper.fromSession(session)
      circlesMapper <- CirclesExpenseListMapper.fromSession(session)
    yield new ExpenseListRepository[F]:
      val main = mainMapper
      val byCircle = circlesMapper

/** Represents the read model of an expense list. This class is a one to one
  * mapping of the expense list table in the database without the creation and
  * update timestamps.
  *
  * @param id
  *   The unique identifier of the expense list.
  * @param name
  *   The name of the expense list.
  * @param circleId
  *   The unique identifier of the circle associated with the expense list.
  */
case class ExpenseListReadMapper(id: String, name: String, circleId: String)

/** Represents the write model of an expense list. This class is used for
  * updating the expense list data. It is obtained by making a change to the
  * domain model.
  *
  * @param id
  *   The unique identifier of the expense list.
  * @param name
  *   The name of the expense list.
  */
case class ExpenseListWriteMapper(id: String, name: String)

/** A trait that defines the operations for mapping expense lists between the
  * application and the data source.
  *
  * @tparam F
  *   The effect type, representing the context in which the operations are
  *   executed.
  */
trait ExpenseListMapper[F[_]]
    extends DataMapper[
      F,
      CreateExpenseListInput,
      ExpenseListReadMapper,
      ExpenseListWriteMapper,
      ExpenseListId
    ]:

  /** Creates a new expense list.
    *
    * @param a
    *   The input data for creating the expense list.
    * @return
    *   The created expense list in the read model.
    */
  def create(a: CreateExpenseListInput): F[ExpenseListReadMapper]

  /** Updates an existing expense list.
    *
    * @param b
    *   The updated data for the expense list.
    * @return
    *   A unit value indicating the success of the update operation.
    */
  def update(b: ExpenseListWriteMapper): F[Unit]

  /** Deletes an expense list.
    *
    * @param id
    *   The unique identifier of the expense list to delete.
    * @return
    *   A unit value indicating the success of the delete operation.
    */
  def delete(id: ExpenseListId): F[Unit]

  /** Retrieves an expense list by its unique identifier.
    *
    * @param id
    *   The unique identifier of the expense list to retrieve.
    * @return
    *   Either a NotFoundError if the expense list is not found, or the expense
    *   list in the read model.
    */
  def get(id: ExpenseListId): F[Either[NotFoundError, ExpenseListReadMapper]]

/** Trait representing a mapper for finding expense lists that are children of a
  * specified parent circle.
  */
trait CirclesExpenseListMapper[F[_]]
    extends HasMany[F, CircleId, ExpenseListReadMapper]:

  /** Retrieves a list of expense lists that are children of the specified
    * parent circle.
    *
    * @param parentId
    *   The ID of the parent circle.
    * @return
    *   A `F[List[ExpenseListReadMapper]]` representing the list of expense
    *   lists.
    */
  def listChildren(parentId: CircleId): F[List[ExpenseListReadMapper]]

/** Companion object for the `CirclesExpenseListMapper` trait. The trait
  * represents a mapper for finding expense lists that are children of a
  * specified parent circle. The companion object provides a method for creating
  * a new instance of the mapper.
  */
object CirclesExpenseListMapper:

  /** Creates a new instance of `CirclesExpenseListMapper` using the provided
    * session. This is method is effectful and returns a
    * `F[CirclesExpenseListMapper[F]]` because it requires a database operation
    * it uses prepared statements. This means it needs to be run inside a
    * session.
    *
    * @param session
    *   The session to be used for database operations.
    * @return
    *   A `F[CirclesExpenseListMapper[F]]` representing the created mapper
    *   instance.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[CirclesExpenseListMapper[F]] =
    for getQuery <- session.prepare(getExpenseListQuery)
    yield new CirclesExpenseListMapper[F]:
      def listChildren(
          parentId: CircleId
      ): F[List[ExpenseListReadMapper]] =
        getQuery
          .stream(parentId.value, 1024)
          .compile
          .toList

  private val getExpenseListQuery: Query[String, ExpenseListReadMapper] = sql"""
    SELECT id, name, circle_id FROM expense_lists WHERE circle_id = $text
  """.query(text *: text *: text).to[ExpenseListReadMapper]

/** Companion object for the `ExpenseListMapper` trait. Provides a method for
  * creating a new instance of the mapper.
  */
object ExpenseListMapper:

  /** Creates a new instance of `ExpenseListMapper` using the provided session.
    * This method is effectful and returns a `F[ExpenseListMapper[F]]` because
    * it requires database operations. It uses prepared statements. This means
    * it needs to be run inside a session.
    *
    * @param session
    *   The session to be used for database operations.
    * @return
    *   A `F[ExpenseListMapper[F]]` representing the created mapper instance.
    */
  def fromSession[F[_]: Monad](session: Session[F]): F[ExpenseListMapper[F]] =
    for
      createQuery <- session.prepare(createExpenseListQuery)
      getQuery <- session.prepare(getExpenseListQuery)
      updateQuery <- session.prepare(updateExpenseListQuery)
      deleteQuery <- session.prepare(deleteExpenseListQuery)
    yield new ExpenseListMapper[F]:
      def create(a: CreateExpenseListInput): F[ExpenseListReadMapper] =
        createQuery.unique(a.circleId.value, a.name)

      def update(b: ExpenseListWriteMapper): F[Unit] =
        updateQuery.execute(b.name, b.id).void

      def delete(id: ExpenseListId): F[Unit] =
        deleteQuery.execute(id.value).void

      def get(
          id: ExpenseListId
      ): F[Either[NotFoundError, ExpenseListReadMapper]] =
        getQuery
          .option(id.value)
          .map:
            case Some(value) => Right(value)
            case None =>
              Left(NotFoundError(s"Expense list with id $id not found"))

  private val getExpenseListQuery: Query[String, ExpenseListReadMapper] = sql"""
    SELECT id, name, circle_id FROM expense_lists WHERE id = $text
  """.query(text *: text *: text).to[ExpenseListReadMapper]

  private val createExpenseListQuery
      : Query[(String, String), ExpenseListReadMapper] =
    sql"""
    INSERT INTO expense_lists (circle_id, name) VALUES ($text, $text)
    RETURNING id, name, circle_id
  """.query(text *: text *: text).to[ExpenseListReadMapper]

  private val updateExpenseListQuery: Command[(String, String)] = sql"""
    UPDATE expense_lists SET name = $text WHERE id = $text
  """.command

  private val deleteExpenseListQuery: Command[String] = sql"""
    DELETE FROM expense_lists WHERE id = $text
  """.command
