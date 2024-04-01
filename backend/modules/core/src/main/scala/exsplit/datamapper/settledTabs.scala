package exsplit.datamapper.settledTabs

import exsplit.db._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import java.time.LocalDate
import exsplit.datamapper.DataMapper
import exsplit.datamapper.HasMany

/** Represents the read model of a settled tab. This class is a one-to-one
  * mapping of the settled tab table in the database without the creation and
  * update timestamps.
  *
  * @param id
  *   The ID of the settled tab.
  * @param expenseListId
  *   The ID of the circle associated with the settled tab.
  * @param fromMember
  *   The ID of the member who paid the tab.
  * @param toMember
  *   The ID of the member who received the payment.
  * @param amount
  *   The amount of the tab that was settled.
  * @param settledAt
  *   The date when the tab was settled.
  *
  * ### schema
  * {{{
  *  create table settled_tabs (
  * id text primary key default md5(now()::text || random()::text),
  * expense_list_id text not null references expense_lists(id),
  * from_member text not null references circle_members(id),
  * to_member text not null references circle_members(id),
  * amount float not null,
  * settled_at date not null default current_date
  * );
  *
  * }}}
  */
case class SettledTabReadMapper(
    id: String,
    expenseListId: String,
    fromMember: String,
    toMember: String,
    amount: Float,
    settledAt: LocalDate
)

/** Represents a SettledTabWriteMapper, which is used to map settled tab data.
  * The fields are optional so that they can be updated independently or
  * together. This class is created by making a change to the domain model.
  *
  * @param id
  *   The ID of the settled tab.
  * @param fromMember
  *   The ID of the member who paid the tab.
  * @param toMember
  *   The ID of the member who received the payment.
  * @param amount
  *   The amount of the settled tab.
  */
case class SettledTabWriteMapper(
    id: String,
    fromMember: Option[String],
    toMember: Option[String],
    amount: Option[Float]
)

/** Repository trait for managing settled tabs. It contains the main mapper for
  * settled tabs and repositories for retrieving settled tabs by expenses, from
  * members, and to members.
  *
  * @tparam F
  *   The effect type, representing the context in which the repository
  *   operates.
  */
trait SettledTabRepository[F[_]]:
  /** The main mapper for settled tabs.
    */
  val main: SettledTabMapper[F]

  /** Repository for retrieving settled tabs by expenses.
    */
  val byExpenses: ExpenseListToSettledTabs[F]

  /** Repository for retrieving settled tabs by from members.
    */
  val byFromMembers: FromMemberToSettledTabs[F]

  /** Repository for retrieving settled tabs by to members.
    */
  val byToMembers: ToMemberToSettledTabs[F]

object SettledTabRepository:
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[SettledTabRepository[F]] =
    for
      mainMapper <- SettledTabMapper.fromSession(session)
      byExpenses <- ExpenseListToSettledTabs.fromSession(session)
      byFromMembers <- FromMemberToSettledTabs.fromSession(session)
      byToMembers <- ToMemberToSettledTabs.fromSession(session)
    yield new SettledTabRepository[F]:
      val main = mainMapper
      val byExpenses = byExpenses
      val byFromMembers = byFromMembers
      val byToMembers = byToMembers

/** A trait representing a data mapper for Settled Tabs. It provides methods for
  * creating, retrieving, updating, and deleting Settled Tabs.
  *
  * @tparam F
  *   The effect type, representing the context in which the operations are
  *   executed.
  */
trait SettledTabMapper[F[_]]
    extends DataMapper[
      F,
      SettleExpenseListInput,
      SettledTabReadMapper,
      SettledTabWriteMapper,
      String
    ]:

  /** Creates a new Settled Tab based on the provided input.
    *
    * @param input
    *   The input data for creating the Settled Tab.
    * @return
    *   An effect that resolves to the created Settled Tab.
    */
  def create(input: SettleExpenseListInput): F[SettledTabReadMapper]

  /** Retrieves a Settled Tab by its ID.
    *
    * @param id
    *   The ID of the Settled Tab to retrieve.
    * @return
    *   An effect that resolves to either the retrieved Settled Tab or a
    *   NotFoundError if the Settled Tab is not found.
    */
  def get(id: String): F[Either[NotFoundError, SettledTabReadMapper]]

  /** Updates a Settled Tab with the provided data.
    *
    * @param b
    *   The updated data for the Settled Tab.
    * @return
    *   An effect that resolves to Unit when the update is successful.
    */
  def update(b: SettledTabWriteMapper): F[Unit]

  /** Deletes a Settled Tab by its ID.
    *
    * @param id
    *   The ID of the Settled Tab to delete.
    * @return
    *   An effect that resolves to Unit when the deletion is successful.
    */
  def delete(id: String): F[Unit]

/** Trait representing a mapping from an expense list to settled tabs. It
  * provides methods to retrieve a list of settled tabs associated with a given
  * expense list.
  *
  * @tparam F
  *   the effect type
  */
trait ExpenseListToSettledTabs[F[_]]
    extends HasMany[F, ExpenseListId, SettledTabReadMapper]:
  /** Retrieves a list of settled tabs associated with the specified expense
    * list.
    *
    * @param parent
    *   the parent expense list ID
    * @return
    *   a list of settled tabs
    */
  def listChildren(parent: ExpenseListId): F[List[SettledTabReadMapper]]

/** Trait representing a mapping from a member to settled tabs. It provides
  * methods to retrieve a list of settled tabs associated with a given member.
  *
  * @tparam F
  *   the effect type
  */
trait FromMemberToSettledTabs[F[_]]
    extends HasMany[F, CircleMemberId, SettledTabReadMapper]:
  /** Retrieves a list of settled tabs associated with the specified member.
    *
    * @param parent
    *   the parent member ID
    * @return
    *   a list of settled tabs
    */
  def listChildren(parent: CircleMemberId): F[List[SettledTabReadMapper]]

/** Trait representing a mapping to a member from settled tabs. It provides
  * methods to retrieve a list of settled tabs associated with a given member.
  *
  * @tparam F
  *   the effect type
  */
trait ToMemberToSettledTabs[F[_]]
    extends HasMany[F, CircleMemberId, SettledTabReadMapper]:
  /** Retrieves a list of settled tabs associated with the specified member.
    *
    * @param parent
    *   the parent member ID
    * @return
    *   a list of settled tabs
    */
  def listChildren(parent: CircleMemberId): F[List[SettledTabReadMapper]]

object ExpenseListToSettledTabs:
  /** Creates a new instance of ExpenseListToSettledTabs from a session. This is
    * effectful because it requires preparing the queries. This is why the
    * result is wrapped in an effect type `F`.
    *
    * @param session
    *   the session to use for database operations
    * @return
    *   a new instance of ExpenseListToSettledTabs wrapped in an effect type `F`
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ExpenseListToSettledTabs[F]] =
    for getSettledTabsQuery <- session.prepare(getSettledTabsQuery)
    yield new ExpenseListToSettledTabs[F]:
      def listChildren(parent: ExpenseListId): F[List[SettledTabReadMapper]] =
        getSettledTabsQuery.stream(parent.value, 1024).compile.toList

  private val getSettledTabsQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE expense_list_id = $text
    """
      .query(varchar *: varchar *: varchar *: varchar *: float4 *: date)
      .to[SettledTabReadMapper]

object FromMemberToSettledTabs:
  /** Creates a new instance of FromMemberToSettledTabs from a session. This is
    * effectful because it requires preparing the queries. This is why the
    * result is wrapped in an effect type `F`.
    *
    * @param session
    *   the session to use for database operations
    * @return
    *   a new instance of FromMemberToSettledTabs wrapped in an effect type `F`
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[FromMemberToSettledTabs[F]] =
    for getSettledTabsQuery <- session.prepare(getSettledTabsQuery)
    yield new FromMemberToSettledTabs[F]:
      def listChildren(parent: CircleMemberId): F[List[SettledTabReadMapper]] =
        getSettledTabsQuery.stream(parent.value, 1024).compile.toList

  private val getSettledTabsQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE from_member = $text
    """
      .query(varchar *: varchar *: varchar *: varchar *: float4 *: date)
      .to[SettledTabReadMapper]

object ToMemberToSettledTabs:
  /** Creates a new instance of ToMemberToSettledTabs from a session. This is
    * effectful because it requires preparing the queries. This is why the
    * result is wrapped in an effect type `F`.
    *
    * @param session
    *   the session to use for database operations
    * @return
    *   a new instance of ToMemberToSettledTabs wrapped in an effect type `F`
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[ToMemberToSettledTabs[F]] =
    for getSettledTabsQuery <- session.prepare(getSettledTabsQuery)
    yield new ToMemberToSettledTabs[F]:
      def listChildren(parent: CircleMemberId): F[List[SettledTabReadMapper]] =
        getSettledTabsQuery.stream(parent.value, 1024).compile.toList

  private val getSettledTabsQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE to_member = $text
    """
      .query(varchar *: varchar *: varchar *: varchar *: float4 *: date)
      .to[SettledTabReadMapper]

/** A companion object for the SettledTabMapper trait. It provides a method for
  * creating a new instance of SettledTabMapper from a session.
  */
object SettledTabMapper:

  /** Creates a new instance of SettledTabMapper from a session. This is
    * effectful because it requires preparing the queries. This is why the
    * result is wrapped in an effect type `F`.
    *
    * @param session
    *   the session to use for database operations
    * @return
    *   a new instance of SettledTabMapper
    */
  def fromSession[F[_]: Concurrent: Parallel](
      session: Session[F]
  ): F[SettledTabMapper[F]] =
    for
      getSettledTabQuery <- session.prepare(getSettledTabQuery)
      createSettledTabQuery <- session.prepare(createSettledTabQuery)
      deleteSettledTabQuery <- session.prepare(deleteSettledTabQuery)
      updateSettledTabFromMemberQuery <- session.prepare(
        updateSettledTabFromMemberQuery
      )
      updateSettledTabToMemberQuery <- session.prepare(
        updateSettledTabToMemberQuery
      )
      updateSettledTabAmountQuery <- session.prepare(
        updateSettledTabAmountQuery
      )
    yield new SettledTabMapper[F]:
      def create(input: SettleExpenseListInput): F[SettledTabReadMapper] =
        createSettledTabQuery.unique(input)

      def get(id: String): F[Either[NotFoundError, SettledTabReadMapper]] =
        getSettledTabQuery
          .option(id)
          .map:
            case Some(value) => Right(value)
            case None =>
              Left(NotFoundError(s"Settled tab with id $id not found"))

      def update(b: SettledTabWriteMapper): F[Unit] =
        val actions = List(
          b.fromMember.map(member =>
            updateSettledTabFromMemberQuery.execute((member, b.id))
          ),
          b.toMember.map(member =>
            updateSettledTabToMemberQuery.execute((member, b.id))
          ),
          b.amount.map(amount =>
            updateSettledTabAmountQuery.execute((amount, b.id))
          )
        ).flatten

        actions.parSequence.void

      def delete(id: String): F[Unit] =
        deleteSettledTabQuery.execute(id).void

  private val getSettledTabQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE id = $text
    """
      .query(varchar *: varchar *: varchar *: varchar *: float4 *: date)
      .to[SettledTabReadMapper]

  private val createSettledTabQuery
      : Query[SettleExpenseListInput, SettledTabReadMapper] =
    sql"""
      INSERT INTO settled_tabs (expense_list_id, from_member, to_member, amount)
      VALUES ($text, $text, $text, $float4)
      RETURNING id, expense_list_id, from_member, to_member, amount, settled_at
    """
      .query(varchar *: varchar *: varchar *: varchar *: float4 *: date)
      .contramap: (input: SettleExpenseListInput) =>
        (
          input.expenseListId.value,
          input.fromMemberId.value,
          input.toMemberId.value,
          input.amount.value
        )
      .to[SettledTabReadMapper]

  private val deleteSettledTabQuery: Command[String] = sql"""
    DELETE FROM settled_tabs WHERE id = $text
  """.command

  private val updateSettledTabFromMemberQuery: Command[(String, String)] =
    sql"""
    UPDATE settled_tabs SET from_member = $text WHERE id = $text
  """.command

  private val updateSettledTabToMemberQuery: Command[(String, String)] = sql"""
    UPDATE settled_tabs SET to_member = $text WHERE id = $text
  """.command

  private val updateSettledTabAmountQuery: Command[(Float, String)] = sql"""
    UPDATE settled_tabs SET amount = $float4 WHERE id = $text
  """.command
