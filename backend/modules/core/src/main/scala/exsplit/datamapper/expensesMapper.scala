package exsplit.datamapper.expenseMapper

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
import java.util.UUID
import exsplit.datamapper._
import java.time.LocalDate

/** Represents an expense read mapper. This class is a one to one mapping of the
  * expense table in the database without the creation and update timestamps.
  *
  * @param id
  *   The unique identifier of the expense.
  * @param expenseListId
  *   The identifier of the expense list that the expense belongs to.
  * @param paidBy
  *   The identifier of the circle member who paid for the expense.
  * @param description
  *   The description of the expense.
  * @param price
  *   The price of the expense.
  * @param date
  *   The date when the expense was made.
  *
  * ### Schema
  * {{{
  * create table expenses(
  * id text primary key default md5(now()::text || random()::text),
  * expense_list_id text not null references expense_lists(id),
  * paid_by varchar(255) not null references circle_members(id),
  * description text not null,
  * price float not null,
  * date date not null,
  * created_at timestamp not null default current_timestamp,
  * updated_at timestamp not null default current_timestamp
  * );
  * }}}
  */
case class ExpenseReadMapper(
    id: String,
    expenseListId: String,
    paidBy: String,
    description: String,
    price: Float,
    date: LocalDate
)

/** Represents a mapper for writing expense data. The fields are optional to
  * allow partial updates.
  *
  * @param id
  *   The unique identifier of the expense.
  * @param paidBy
  *   The optional name of the person who paid for the expense.
  * @param description
  *   The optional description of the expense.
  * @param price
  *   The optional price of the expense.
  * @param date
  *   The optional date of the expense.
  */
case class ExpenseWriteMapper(
    id: String,
    paidBy: Option[String],
    description: Option[String],
    price: Option[Float],
    date: Option[LocalDate]
)

/** Represents the data mapper for the owed amount read model. This class is a
  * one-to-one mapping of the owed amount table in the database without the
  * creation and update timestamps.
  *
  * ### Schema
  * {{{
  * create table owed_amounts (
  * id text primary key default md5(now()::text || random()::text),
  * expense_id text not null references expenses(id),
  * from_member varchar(255) not null references circle_members(id),
  * to_member varchar(255) not null references circle_members(id),
  * amount float not null,
  * created_at timestamp not null default current_timestamp,
  * updated_at timestamp not null default current_timestamp
  * );
  * }}}
  *
  * @param id
  *   The unique identifier of the owed amount.
  * @param expenseId
  *   The unique identifier of the expense.
  * @param fromMember
  *   The unique identifier of the member who owes the amount.
  * @param toMember
  *   The unique identifier of the member to whom the amount is owed.
  * @param amount
  *   The amount owed.
  */
case class OwedAmountReadMapper(
    id: String,
    expenseId: String,
    fromMember: String,
    toMember: String,
    amount: Float
)

/** Represents the data mapper for the owed amount write operation. The fields
  * are optional to allow partial updates.
  *
  * @param id
  *   The unique identifier of the owed amount.
  * @param fromMember
  *   The member who owes the amount.
  * @param toMember
  *   The member to whom the amount is owed.
  * @param amount
  *   The amount owed.
  */
case class OwedAmountWriteMapper(
    id: String,
    fromMember: Option[String],
    toMember: Option[String],
    amount: Option[Float]
)

/** Represents the input for creating an owed amount.
  *
  * @param expenseId
  *   The ID of the expense.
  * @param fromMember
  *   The ID of the member who owes the amount.
  * @param toMember
  *   The ID of the member to whom the amount is owed.
  * @param amount
  *   The amount owed.
  */
case class CreateOwedAmountInput(
    expenseId: ExpenseId,
    fromMember: CircleMemberId,
    toMember: CircleMemberId,
    amount: Amount
)

/** Trait defining the ExpenseMapper, which is responsible for mapping
  * expense-related data between the application and the underlying data
  * storage. The trait extends the DataMapper trait, which provides the basic
  * CRUD operations for the data mapper.
  *
  * @tparam F
  *   The effect type representing the context in which the operations are
  *   executed.
  */
trait ExpenseMapper[F[_]]
    extends DataMapper[
      F,
      CreateExpenseInput,
      ExpenseReadMapper,
      ExpenseWriteMapper
    ]:

  /** Creates a new expense based on the provided input.
    *
    * @param input
    *   The input data for creating the expense.
    * @return
    *   The created expense, wrapped in the effect type F.
    */
  def create(input: CreateExpenseInput): F[ExpenseReadMapper]

  /** Retrieves an expense by its ID.
    *
    * @param id
    *   The ID of the expense to retrieve.
    * @return
    *   Either the retrieved expense or a NotFoundError, wrapped in the effect
    *   type F.
    */
  def get(id: String): F[Either[NotFoundError, ExpenseReadMapper]]

  /** Updates an existing expense.
    *
    * @param b
    *   The updated expense data.
    * @return
    *   The updated expense, wrapped in the effect type F.
    */
  def update(b: ExpenseWriteMapper): F[Unit]

  /** Deletes an expense by its ID.
    *
    * @param id
    *   The ID of the expense to delete.
    * @return
    *   Unit, wrapped in the effect type F.
    */
  def delete(id: String): F[Unit]

/** Trait defining the OwedAmountMapper, which is responsible for mapping owed
  * amount-related data between the application and the underlying data storage.
  * The trait extends the DataMapper trait, which provides the basic CRUD
  * operations for the data mapper.
  *
  * @tparam F
  *   The effect type representing the context in which the operations are
  *   executed.
  */
trait OwedAmountMapper[F[_]]
    extends DataMapper[
      F,
      CreateOwedAmountInput,
      OwedAmountReadMapper,
      OwedAmountWriteMapper
    ]:

  /** Creates a new owed amount based on the provided input.
    *
    * @param input
    *   The input data for creating the owed amount.
    * @return
    *   The created owed amount, wrapped in the effect type F.
    */
  def create(input: CreateOwedAmountInput): F[OwedAmountReadMapper]

  /** Retrieves an owed amount by its ID.
    *
    * @param id
    *   The ID of the owed amount to retrieve.
    * @return
    *   Either the retrieved owed amount or a NotFoundError, wrapped in the
    *   effect type F.
    */
  def get(id: String): F[Either[NotFoundError, OwedAmountReadMapper]]

  /** Updates an existing owed amount.
    *
    * @param b
    *   The updated owed amount data.
    * @return
    *   The updated owed amount, wrapped in the effect type F.
    */
  def update(b: OwedAmountWriteMapper): F[Unit]

  /** Deletes an owed amount by its ID.
    *
    * @param id
    *   The ID of the owed amount to delete.
    * @return
    *   Unit, wrapped in the effect type F.
    */
  def delete(id: String): F[Unit]

/** Companion object for the ExpenseMapper trait. Contains the factory method
  * for creating an instance of the ExpenseMapper.
  */
object OwedAmountMapper:

  /** Creates an instance of OwedAmountMapper from a session. This is an
    * effectful operation because the queries need to be prepared before the
    * mapper can be created.
    *
    * @param session
    *   The session to create the mapper from.
    * @return
    *   The created OwedAmountMapper.
    */
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[OwedAmountMapper[F]] =
    for
      createOwedAmountQuery <- session.prepare(createOwedAmountQuery)
      getOwedAmountQuery <- session.prepare(getOwedAmountQuery)
      updateOwedAmountFromMember <- session.prepare(updateOwedAmountFromMember)
      updateOwedAmountToMember <- session.prepare(updateOwedAmountToMember)
      updateOwedAmountAmountQuery <- session.prepare(
        updateOwedAmountAmountQuery
      )
      deleteOwedAmountQuery <- session.prepare(deleteOwedAmountQuery)
    yield new OwedAmountMapper[F]:

      def create(input: CreateOwedAmountInput): F[OwedAmountReadMapper] =
        createOwedAmountQuery
          .unique(input)

      def get(id: String): F[Either[NotFoundError, OwedAmountReadMapper]] =
        getOwedAmountQuery
          .option(id)
          .map(
            _.toRight(NotFoundError(s"Owed amount with id = $id not found."))
          )

      def update(b: OwedAmountWriteMapper): F[Unit] =
        val updates = List(
          b.fromMember.map(fromMember =>
            updateOwedAmountFromMember.execute(fromMember, b.id)
          ),
          b.toMember.map(toMember =>
            updateOwedAmountToMember.execute(toMember, b.id)
          ),
          b.amount.map(amount =>
            updateOwedAmountAmountQuery.execute(amount, b.id)
          )
        ).flatten

        updates.parSequence.void

      def delete(id: String): F[Unit] =
        deleteOwedAmountQuery
          .execute(id)
          .void

  private val getOwedAmountQuery: Query[String, OwedAmountReadMapper] =
    sql"""
         SELECT id, expense_id, from_member, to_member, amount
         FROM owed_amounts
         WHERE id = $text
       """
      .query(varchar *: varchar *: varchar *: varchar *: float4)
      .to[OwedAmountReadMapper]

  private val createOwedAmountQuery
      : Query[CreateOwedAmountInput, OwedAmountReadMapper] =
    sql"""
          INSERT INTO owed_amounts (expense_id, from_member, to_member, amount)
          VALUES ($text, $varchar, $varchar, $float4)
          RETURNING id, expense_id, from_member, to_member, amount
        """
      .query(varchar *: varchar *: varchar *: varchar *: float4)
      .contramap: (input: CreateOwedAmountInput) =>
        (
          input.expenseId.value,
          input.fromMember.value,
          input.toMember.value,
          input.amount.value
        )
      .to[OwedAmountReadMapper]

  private val deleteOwedAmountQuery: Command[String] =
    sql"""
         DELETE FROM owed_amounts
         WHERE id = $text
       """.command

  private val updateOwedAmountFromMember: Command[(String, String)] =
    sql"""
         UPDATE owed_amounts
         SET from_member = $text
         WHERE id = $text
       """.command

  private val updateOwedAmountToMember: Command[(String, String)] =
    sql"""
         UPDATE owed_amounts
         SET to_member = $text
         WHERE id = $text
       """.command

  private val updateOwedAmountAmountQuery: Command[(Float, String)] =
    sql"""
         UPDATE owed_amounts
         SET amount = $float4
         WHERE id = $text
       """.command

/** Companion object for the ExpenseMapper trait. Contains the factory method
  * for creating an instance of the ExpenseMapper.
  */
object ExpenseMapper:

  /** Creates an instance of ExpenseMapper from a session. This is an effectful
    * operation because the queries need to be prepared before the mapper can be
    * created.
    *
    * @param session
    *   The session to create the mapper from.
    * @return
    *   The created ExpenseMapper.
    */
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[ExpenseMapper[F]] =
    for
      createExpenseQuery <- session.prepare(createExpenseQuery)
      getExpenseQuery <- session.prepare(getExpenseQuery)
      updateExpenseDescriptionQuery <- session.prepare(
        updateExpenseDescriptionQuery
      )
      updateExpensePriceQuery <- session.prepare(updateExpensePriceQuery)
      updateExpenseDateQuery <- session.prepare(updateExpenseDateQuery)
      updateExpensePaidByQuery <- session.prepare(updateExpensePaidByQuery)
      deleteExpenseQuery <- session.prepare(deleteExpenseQuery)
    yield new ExpenseMapper[F]:
      def create(input: CreateExpenseInput): F[ExpenseReadMapper] =
        createExpenseQuery
          .unique(input)

      def get(id: String): F[Either[NotFoundError, ExpenseReadMapper]] =
        getExpenseQuery
          .option(id)
          .map(_.toRight(NotFoundError(s"Expense with id = $id not found.")))

      def update(b: ExpenseWriteMapper): F[Unit] =
        val updates = List(
          b.paidBy.map(pb => updateExpensePaidByQuery.execute(pb, b.id)),
          b.description.map(desc =>
            updateExpenseDescriptionQuery.execute(desc, b.id)
          ),
          b.price.map(price => updateExpensePriceQuery.execute(price, b.id)),
          b.date.map(date => updateExpenseDateQuery.execute(date, b.id))
        ).flatten

        updates.parSequence.void

      def delete(id: String): F[Unit] =
        deleteExpenseQuery
          .execute(id)
          .void

  private val getExpenseQuery: Query[String, ExpenseReadMapper] =
    sql"""
         SELECT id, expense_list_id, paid_by, description, price, date
         FROM expenses
         WHERE id = $text
       """
      .query(varchar *: varchar *: varchar *: text *: float4 *: date)
      .to[ExpenseReadMapper]

  private val createExpenseQuery: Query[CreateExpenseInput, ExpenseReadMapper] =
    sql"""
          INSERT INTO expenses (expense_list_id, paid_by, description, price, date)
          VALUES ($text, $varchar, $text, $float4, $date)
          RETURNING id, expense_list_id, paid_by, description, price, date
        """
      .query(varchar *: varchar *: varchar *: text *: float4 *: date)
      .contramap: (input: CreateExpenseInput) =>
        (
          input.expenseListId.value,
          input.expense.paidBy.value,
          input.expense.description,
          input.expense.price.value,
          LocalDate.parse(input.expense.date.value)
        )
      .to[ExpenseReadMapper]

  private val updateExpenseDescriptionQuery: Command[(String, String)] =
    sql"""
          UPDATE expenses
          SET description = $text
          WHERE id = $text
        """.command

  private val updateExpensePriceQuery: Command[(Float, String)] =
    sql"""
          UPDATE expenses
          SET price = $float4
          WHERE id = $text
        """.command

  private val updateExpenseDateQuery: Command[(LocalDate, String)] =
    sql"""
          UPDATE expenses
          SET date = $date
          WHERE id = $text
        """.command

  private val updateExpensePaidByQuery: Command[(String, String)] =
    sql"""
          UPDATE expenses
          SET paid_by = $varchar
          WHERE id = $text
        """.command

  private val deleteExpenseQuery: Command[String] =
    sql"""
          DELETE FROM expenses
          WHERE id = $text
        """.command
