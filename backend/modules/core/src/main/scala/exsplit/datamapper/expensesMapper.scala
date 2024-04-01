package exsplit.datamapper.expenses

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

/*
 * Represents a repository for managing expenses. The main mapper contains the
 * basic CRUD operations for the expense. The circle members mapper contains
 * operations for finding expenses that are children of a specified parent circle
 * member. The expense lists mapper contains operations for finding expenses that
 * are children of a specified parent expense list.
 */
trait ExpenseRepository[F[_]]:
  val mainMapper: ExpenseMapper[F]
  val expenseDetail: ExpenseDetailMapper[F]
  val circleMembers: CircleMemberToExpenseMapper[F]
  val expenseLists: ExpenseListToExpenseMapper[F]

/* Represents a repository for managing owed amounts. The main mapper contains the
 * basic CRUD operations for the owed amount. The circle members mapper contains
 * operations for finding owed amounts that are children of a specified parent
 * circle member. The expenses mapper contains operations for finding owed amounts
 * that are children of a specified parent expense.
 */
trait OwedAmountRepository[F[_]]:
  val mainMapper: OwedAmountMapper[F]
  val circleMembers: CircleMemberToOwedAmountMapper[F]
  val expenses: ExpensesToOwedAmountMapper[F]

/* Companion object for the `ExpenseRepository` trait. Provides a method for
 * creating a new instance of the repository.
 */
object ExpenseRepository:
  /*
   * Creates a new instance of `ExpenseRepository` using the provided session. This
   * method is effectful and returns a `F[ExpenseRepository[F]]` because it requires
   * database operations. It uses prepared statements. This means it needs to be run
   * inside a session.
   *
   * @param session
   *   The session to be used for database operations.
   * @return
   *   A `F[ExpenseRepository[F]]` representing the created repository instance.
   */
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[ExpenseRepository[F]] =
    for
      mainMapper <- ExpenseMapper.fromSession(session)
      expenseDetail <- ExpenseDetailMapper.fromSession(session)
      circleMembers <- CircleMemberToExpenseMapper.fromSession(session)
      expenseLists <- ExpenseListToExpenseMapper.fromSession(session)
    yield new ExpenseRepository[F]:
      val mainMapper = mainMapper
      val expenseDetail = expenseDetail
      val circleMembers = circleMembers
      val expenseLists = expenseLists

/* Companion object for the `OwedAmountRepository` trait. Provides a method for
 * creating a new instance of the repository.
 */
object OwedAmountRepository:
  /*
   * Creates a new instance of `OwedAmountRepository` using the provided session. This
   * method is effectful and returns a `F[OwedAmountRepository[F]]` because it requires
   * database operations. It uses prepared statements. This means it needs to be run
   * inside a session.
   *
   * @param session
   *   The session to be used for database operations.
   * @return
   *   A `F[OwedAmountRepository[F]]` representing the created repository instance.
   */
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[OwedAmountRepository[F]] =
    for
      mainMapper <- OwedAmountMapper.fromSession(session)
      circleMembers <- CircleMemberToOwedAmountMapper.fromSession(session)
      expenses <- ExpensesToOwedAmountMapper.fromSession(session)
    yield new OwedAmountRepository[F]:
      val mainMapper = mainMapper
      val circleMembers = circleMembers
      val expenses = expenses

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

/** Represents a view of expense detail with the name of the person who paid for
  * the expense.
  *
  * @param id
  *   The unique identifier of the expense.
  * @param expenseListId
  *   The identifier of the expense list that the expense belongs to.
  * @param paidBy
  *   The identifier of the circle member who paid for the expense.
  * @param paidByName
  *   The name of the person who paid for the expense.
  * @param description
  *   The description of the expense.
  * @param price
  *   The price of the expense.
  * @param date
  *   The date when the expense was made.
  */
case class ExpenseDetailRead(
    id: String,
    expenseListId: String,
    paidBy: String,
    paidByName: String,
    description: String,
    price: Float,
    date: LocalDate
)

/** Represents a view of owed amount detail with the name of the person who owes
  * the amount.
  *
  * @param id
  *   The unique identifier of the owed amount.
  * @param expenseId
  *   The unique identifier of the expense.
  * @param fromMember
  *   The unique identifier of the member who owes the amount.
  * @param fromMemberName
  *   The name of the person who owes the amount.
  * @param toMember
  *   The unique identifier of the member to whom the amount is owed.
  * @param toMemberName
  *   The name of the person to whom the amount is owed.
  * @param amount
  *   The amount owed.
  */
case class OwedAmountDetailRead(
    id: String,
    expenseId: String,
    fromMember: String,
    fromMemberName: String,
    toMember: String,
    toMemberName: String,
    amount: Float
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
      ExpenseWriteMapper,
      ExpenseId
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
  def get(id: ExpenseId): F[Either[NotFoundError, ExpenseReadMapper]]

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
  def delete(id: ExpenseId): F[Unit]

case class OwedAmountKey(
    expenseId: ExpenseId,
    fromMember: CircleMemberId,
    toMember: CircleMemberId
)

/** This trait represents a mapper that maps a circle member to the owed amount.
  * It extends the `HasMany` trait with the type parameters `F[_]`,
  * `CircleMemberId`, and `OwedAmountReadMapper`.
  */
trait CircleMemberToOwedAmountMapper[F[_]]
    extends HasMany[F, CircleMemberId, OwedAmountReadMapper]:

  /** Retrieves a list of owed amounts for a given circle member. This includes
    * all the amounts that the member owes to others, in all expenseLists.
    *
    * @param fromMember
    *   The ID of the circle member.
    * @return
    *   A computation in the context `F` that yields a list of
    *   `OwedAmountReadMapper`.
    */
  def listChildren(fromMember: CircleMemberId): F[List[OwedAmountReadMapper]]

  /** Retrieves a list of owed amounts for a given circle member. This includes
    * all the amounts that the member is owed by others, in all expenseLists.
    *
    * @param toMember
    *   The ID of the circle member.
    * @return
    *   A computation in the context `F` that yields a list of
    *   `OwedAmountReadMapper`.
    */
  def toMember(toMember: CircleMemberId): F[List[OwedAmountReadMapper]]

/** This trait represents a mapper that maps an expense to the owed amount. It
  * extends the `HasMany` trait with the type parameters `F[_]`, `ExpenseId`,
  * and `OwedAmountReadMapper`.
  */
trait ExpensesToOwedAmountMapper[F[_]]
    extends HasMany[F, ExpenseId, OwedAmountReadMapper]:

  /** Retrieves a list of owed amounts for the children of a given expense.
    *
    * @param parent
    *   The ID of the expense.
    * @return
    *   A computation in the context `F` that yields a list of
    *   `OwedAmountReadMapper`.
    */
  def listChildren(parent: ExpenseId): F[List[OwedAmountReadMapper]]

/** This trait represents a mapper that maps an expense list to an expense. It
  * extends the `HasMany` trait with the type parameters `F[_]`,
  * `ExpenseListId`, and `ExpenseReadMapper`.
  */
trait ExpenseListToExpenseMapper[F[_]]
    extends HasMany[F, ExpenseListId, ExpenseReadMapper]:

  /** Retrieves a list of expenses for the children of a given expense list.
    *
    * @param parent
    *   The ID of the expense list.
    * @return
    *   A computation in the context `F` that yields a list of
    *   `ExpenseReadMapper`.
    */
  def listChildren(parent: ExpenseListId): F[List[ExpenseReadMapper]]

/** This trait represents a mapper that maps a circle member to an expense. It
  * extends the `HasMany` trait with the type parameters `F[_]`,
  * `CircleMemberId`, and `ExpenseReadMapper`.
  */
trait CircleMemberToExpenseMapper[F[_]]
    extends HasMany[F, CircleMemberId, ExpenseReadMapper]:

  /** Retrieves a list of expenses for the children of a given circle member.
    *
    * @param paidBy
    *   The ID of the circle member.
    * @return
    *   A computation in the context `F` that yields a list of
    *   `ExpenseReadMapper`.
    */
  def listChildren(paidBy: CircleMemberId): F[List[ExpenseReadMapper]]

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
      OwedAmountWriteMapper,
      OwedAmountKey
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
  def get(id: OwedAmountKey): F[Either[NotFoundError, OwedAmountReadMapper]]

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
  def delete(id: OwedAmountKey): F[Unit]

/** Trait defining the methods for retrieving expenses with additional details.
  * Since this is not a one-to-one mapping of the database table, it is defined
  * as a separate trait.
  *
  * @tparam F
  *   The effect type.
  */
trait ExpenseDetailMapper[F[_]]:
  /** Retrieves the expense detail for the given expense ID.
    * @param id
    *   The ID of the expense.
    * @return
    *   Either the expense detail or a NotFoundError if the expense is not
    *   found.
    */
  def get(id: ExpenseId): F[Either[NotFoundError, ExpenseDetailRead]]

/** Trait defining the methods for retrieving owed amounts with additional
  * details. Since this is not a one-to-one mapping of the database table, it is
  * defined as a separate trait.
  * @tparam F
  *   The effect type.
  */
trait OwedAmountDetailMapper[F[_]]
    extends HasMany[F, ExpenseId, OwedAmountDetailRead]:
  /** Retrieves the list of owed amounts for the given expense ID.
    * @param id
    *   The ID of the expense.
    * @return
    *   The list of owed amounts.
    */
  def listChildren(id: ExpenseId): F[List[OwedAmountDetailRead]]

/** A data mapper trait for mapping expense lists to their owed amount details.
  * This reads the owed amounts for all expenses that belong to a given expense
  * list.
  *
  * @tparam F
  *   The effect type for performing database operations.
  */
trait ExpenseListOwedAmountMapper[F[_]]
    extends HasMany[F, ExpenseListId, OwedAmountDetailRead]:

  /** Retrieves a list of owed amount details associated with the given expense
    * list. This includes all the owed amounts for all the expenses in the list.
    *
    * @param parent
    *   The ID of the parent expense list.
    * @return
    *   A list of owed amount details.
    */
  def listChildren(parent: ExpenseListId): F[List[OwedAmountDetailRead]]

/** Companion object for the ExpenseMapper trait. Contains the factory method
  * for creating an instance of the ExpenseMapper.
  */
object ExpenseListOwedAmountMapper:
  /** Creates an instance of ExpenseListOwedAmountMapper from a session. This is
    * an effectful operation because the query needs to be prepared before the
    * mapper can be created.
    *
    * @param session
    *   The session to use for database operations.
    * @return
    *   An effectful computation that yields an instance of
    *   ExpenseListOwedAmountMapper.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ExpenseListOwedAmountMapper[F]] =
    for getOwedAmountDetailQuery <- session.prepare(getOwedAmountDetailQuery)
    yield new ExpenseListOwedAmountMapper[F]:

      def listChildren(id: ExpenseListId): F[List[OwedAmountDetailRead]] =
        getOwedAmountDetailQuery
          .stream(id.value, 1024)
          .compile
          .toList

  private val getOwedAmountDetailQuery: Query[String, OwedAmountDetailRead] =
    sql"""
         SELECT oa.id, oa.expense_id, oa.from_member, cm1.name, oa.to_member, cm2.name, oa.amount
         FROM owed_amounts oa
         INNER JOIN circle_members cm1 ON oa.from_member = cm1.id
         INNER JOIN circle_members cm2 ON oa.to_member = cm2.id
         INNER JOIN expenses e ON oa.expense_id = e.id
         WHERE e.expense_list_id = $text
       """
      .query(
        varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: float4
      )
      .to[OwedAmountDetailRead]

object ExpenseDetailMapper:
  /** Creates a new instance of ExpenseDetailMapper using the provided session.
    * This is an effectful operation because the query needs to be prepared
    * before the mapper can be created.
    *
    * @param session
    *   The database session.
    * @tparam F
    *   The effect type.
    * @return
    *   The new ExpenseDetailMapper instance.
    */
  def fromSession[F[_]: Monad](
      session: Session[F]
  ): F[ExpenseDetailMapper[F]] =
    for getExpenseDetailQuery <- session.prepare(getExpenseDetailQuery)
    yield new ExpenseDetailMapper[F]:
      def get(id: ExpenseId): F[Either[NotFoundError, ExpenseDetailRead]] =
        getExpenseDetailQuery
          .option(id.value)
          .map:
            case Some(value) => Right(value)
            case None =>
              Left(NotFoundError(s"Expense with id = $id not found."))

  private val getExpenseDetailQuery: Query[String, ExpenseDetailRead] =
    sql"""
         SELECT e.id, e.expense_list_id, e.paid_by, cm.name, e.description, e.price, e.date
         FROM expenses e
         JOIN circle_members cm ON e.paid_by = cm.id
         WHERE e.id = $text
       """
      .query(varchar *: varchar *: varchar *: varchar *: text *: float4 *: date)
      .to[ExpenseDetailRead]

object OwedAmountDetailMapper:
  /** Creates a new instance of OwedAmountDetailMapper using the provided
    * session. This is an effectful operation because the query needs to be
    * prepared before the mapper can be created.
    * @param session
    *   The database session.
    * @tparam F
    *   The effect type.
    * @return
    *   The new OwedAmountDetailMapper instance.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[OwedAmountDetailMapper[F]] =
    for getOwedAmountDetailQuery <- session.prepare(getOwedAmountDetailQuery)
    yield new OwedAmountDetailMapper[F]:
      def listChildren(id: ExpenseId): F[List[OwedAmountDetailRead]] =
        getOwedAmountDetailQuery
          .stream(id.value, 1024)
          .compile
          .toList

  private val getOwedAmountDetailQuery: Query[String, OwedAmountDetailRead] =
    sql"""
         SELECT oa.id, oa.expense_id, oa.from_member, cm1.name, oa.to_member, cm2.name, oa.amount
         FROM owed_amounts oa
         JOIN circle_members cm1 ON oa.from_member = cm1.id
         JOIN circle_members cm2 ON oa.to_member = cm2.id
         WHERE oa.id = $text
       """
      .query(
        varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: float4
      )
      .to[OwedAmountDetailRead]

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

      def get(
          id: OwedAmountKey
      ): F[Either[NotFoundError, OwedAmountReadMapper]] =
        getOwedAmountQuery
          .option(id)
          .map:
            case Some(value) => Right(value)
            case None =>
              Left(NotFoundError(s"Owed amount with id = $id not found."))

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

      def delete(id: OwedAmountKey): F[Unit] =
        deleteOwedAmountQuery
          .execute(id)
          .void

  private val getOwedAmountQuery: Query[OwedAmountKey, OwedAmountReadMapper] =
    sql"""
         SELECT id, expense_id, from_member, to_member, amount
         FROM owed_amounts
         WHERE expense_id = $text and from_member = $varchar and to_member = $varchar
       """
      .query(varchar *: varchar *: varchar *: varchar *: float4)
      .contramap: (key: OwedAmountKey) =>
        (key.expenseId.value, key.fromMember.value, key.toMember.value)
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

  private val deleteOwedAmountQuery: Command[OwedAmountKey] =
    sql"""
         DELETE FROM owed_amounts
         WHERE expense_id = $text and from_member = $varchar and to_member = $varchar
       """.command
      .contramap: (key: OwedAmountKey) =>
        (key.expenseId.value, key.fromMember.value, key.toMember.value)

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

      def get(id: ExpenseId): F[Either[NotFoundError, ExpenseReadMapper]] =
        getExpenseQuery
          .option(id.value)
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

      def delete(id: ExpenseId): F[Unit] =
        deleteExpenseQuery
          .execute(id.value)
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

/** Companion object for the CircleMemberToOwedAmountMapper trait. Contains the
  * factory method for creating an instance of the
  * CircleMemberToOwedAmountMapper.
  */
object CircleMemberToOwedAmountMapper:
  /** Creates a CircleMemberToOwedAmountMapper from a session. This is an
    * effectful operation because the queries need to be prepared before the
    * mapper can be created.
    *
    * @param session
    *   The session to create the mapper from.
    * @return
    *   The created CircleMemberToOwedAmountMapper.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[CircleMemberToOwedAmountMapper[F]] =
    for
      listChildrenQuery <- session.prepare(listChildrenQuery)
      toMemberQuery <- session.prepare(toMemberQuery)
    yield new CircleMemberToOwedAmountMapper[F]:

      def listChildren(
          fromMember: CircleMemberId
      ): F[List[OwedAmountReadMapper]] =
        listChildrenQuery.stream(fromMember.value, 1024).compile.toList

      def toMember(toMember: CircleMemberId): F[List[OwedAmountReadMapper]] =
        toMemberQuery.stream(toMember.value, 1024).compile.toList

  private val listChildrenQuery: Query[String, OwedAmountReadMapper] =
    sql"""
         SELECT id, expense_id, from_member, to_member, amount
         FROM owed_amounts
         WHERE from_member = $varchar
       """
      .query(varchar *: varchar *: varchar *: varchar *: float4)
      .to[OwedAmountReadMapper]

  private val toMemberQuery: Query[String, OwedAmountReadMapper] =
    sql"""
         SELECT id, expense_id, from_member, to_member, amount
         FROM owed_amounts
         WHERE to_member = $varchar
       """
      .query(varchar *: varchar *: varchar *: varchar *: float4)
      .to[OwedAmountReadMapper]

/** Companion object for the CircleMemberToExpenseMapper trait. Contains the
  * factory method for creating an instance of the CircleMemberToExpenseMapper.
  */
object CircleMemberToExpenseMapper:
  /** Creates a CircleMemberToExpenseMapper from a database session. This is an
    * effectful operation because the query needs to be prepared before the
    * mapper can be created.
    *
    * @param session
    *   The database session.
    * @return
    *   A CircleMemberToExpenseMapper wrapped in an effect type F.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[CircleMemberToExpenseMapper[F]] =
    for listChildrenQuery <- session.prepare(listChildrenQuery)
    yield new CircleMemberToExpenseMapper[F]:

      def listChildren(paidBy: CircleMemberId): F[List[ExpenseReadMapper]] =
        listChildrenQuery.stream(paidBy.value, 1024).compile.toList

  private val listChildrenQuery: Query[String, ExpenseReadMapper] =
    sql"""
         SELECT id, expense_list_id, paid_by, description, price, date
         FROM expenses
         WHERE paid_by = $varchar
       """
      .query(varchar *: varchar *: varchar *: text *: float4 *: date)
      .to[ExpenseReadMapper]

/** Companion object for the ExpenseMapper trait. Contains the factory method
  * for creating an instance of the ExpenseMapper.
  */
object ExpenseListToExpenseMapper:
  /** Creates a new ExpenseListToExpenseMapper from a session. This is an
    * effectful operation because the query needs to be prepared before the
    * mapper can be created.
    *
    * @param session
    *   the session to use for database operations
    * @return
    *   a new ExpenseListToExpenseMapper wrapped in an effect type F
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ExpenseListToExpenseMapper[F]] =
    for listChildrenQuery <- session.prepare(listChildrenQuery)
    yield new ExpenseListToExpenseMapper[F]:

      def listChildren(parent: ExpenseListId): F[List[ExpenseReadMapper]] =
        listChildrenQuery.stream(parent.value, 1024).compile.toList

  private val listChildrenQuery: Query[String, ExpenseReadMapper] =
    sql"""
         SELECT id, expense_list_id, paid_by, description, price, date
         FROM expenses
         WHERE expense_list_id = $text
       """
      .query(varchar *: varchar *: varchar *: text *: float4 *: date)
      .to[ExpenseReadMapper]

/** Companion object for the `ExpensesToOwedAmountMapper` trait. Contains the
  * factory method for creating an instance of the `ExpensesToOwedAmountMapper`.
  */
object ExpensesToOwedAmountMapper:
  /** Creates an instance of `ExpensesToOwedAmountMapper` from a session. This
    * is an effectful operation because the query needs to be prepared before
    * the mapper can be created.
    *
    * @param session
    *   The session to use for database operations.
    * @return
    *   A `F` wrapped `ExpensesToOwedAmountMapper`.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ExpensesToOwedAmountMapper[F]] =
    for listChildrenQuery <- session.prepare(listChildrenQuery)
    yield new ExpensesToOwedAmountMapper[F]:

      def listChildren(parent: ExpenseId): F[List[OwedAmountReadMapper]] =
        listChildrenQuery.stream(parent.value, 1024).compile.toList

  private val listChildrenQuery: Query[String, OwedAmountReadMapper] =
    sql"""
         SELECT id, expense_id, from_member, to_member, amount
         FROM owed_amounts
         WHERE expense_id = $text
       """
      .query(varchar *: varchar *: varchar *: varchar *: float4)
      .to[OwedAmountReadMapper]
