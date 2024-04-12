package exsplit.datamapper.settledTabs

import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop
import exsplit.spec._
import cats.effect._
import cats.syntax.all._
import cats._
import java.time.LocalDate
import exsplit.datamapper._

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

/** Repository trait for managing settled tabs. Extends the SettledTabMapper
  * trait. Adds methods for retrieving settled tabs based on expense list ID,
  * from member ID, and to member ID.
  *
  * @tparam F
  *   the effect type
  */
trait SettledTabRepository[F[_]] extends SettledTabMapper[F]:

  /** Retrieves a list of settled tabs based on the given expense list ID.
    *
    * @param expenseListId
    *   the ID of the expense list
    * @return
    *   a list of settled tabs
    */
  def fromExpenseList(
      expenseListId: ExpenseListId
  ): F[List[SettledTabReadMapper]]

  /** Retrieves a list of settled tabs based on the given from member ID. This
    * includes all settled tabs where the from member is the specified member,
    * across all expense lists.
    *
    * @param fromMemberId
    *   the ID of the from member
    * @return
    *   a list of settled tabs
    */
  def byFromMembers(
      fromMemberId: CircleMemberId
  ): F[List[SettledTabReadMapper]]

  /** Retrieves a list of settled tabs based on the given to member ID. This
    * includes all settled tabs where the to member is the specified member,
    * across all expense lists.
    *
    * @param toMemberId
    *   the ID of the to member
    * @return
    *   a list of settled tabs
    */
  def byToMembers(
      toMemberId: CircleMemberId
  ): F[List[SettledTabReadMapper]]

/** Companion object for the SettledTabRepository trait. It provides a method
  * for creating a new instance of SettledTabRepository from a session.
  */
object SettledTabRepository:
  /** Creates a new instance of SettledTabRepository from a session. This is
    * effectful because it requires preparing the queries. This is why the
    * result is wrapped in an effect type `F`.
    *
    * @param session
    *   the session to use for database operations.
    * @return
    *   a new instance of SettledTabRepository wrapped in an effect type `F`.
    */
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[SettledTabRepository[F]] =
    (
      SettledTabMapper.fromSession(session),
      ExpenseListToSettledTabs.fromSession(session),
      FromMemberToSettledTabs.fromSession(session),
      ToMemberToSettledTabs.fromSession(session)
    )
      .mapN: (mainMapper, byExpenses, byFromMembersRepo, byToMembersRepo) =>
        new SettledTabRepository[F]:

          export mainMapper._
          export byExpenses.{listChildren as fromExpenseList}
          export byFromMembersRepo.{listChildren as byFromMembers}
          export byToMembersRepo.{listChildren as byToMembers}

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
    session
      .prepare(getSettledTabsQuery)
      .map: getSettledTabsQuery =>
        new ExpenseListToSettledTabs[F]:
          def listChildren(
              parent: ExpenseListId
          ): F[List[SettledTabReadMapper]] =
            getSettledTabsQuery.stream(parent.value, 1024).compile.toList

  private val getSettledTabsQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE expense_list_id = $text
    """
      .query(text *: text *: text *: text *: float4 *: date)
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
    session
      .prepare(getSettledTabsQuery)
      .map: getSettledTabsQuery =>
        new FromMemberToSettledTabs[F]:
          def listChildren(
              parent: CircleMemberId
          ): F[List[SettledTabReadMapper]] =
            getSettledTabsQuery.stream(parent.value, 1024).compile.toList

  private val getSettledTabsQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE from_member = $text
    """
      .query(text *: text *: text *: text *: float4 *: date)
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
    session
      .prepare(getSettledTabsQuery)
      .map: getSettledTabsQuery =>
        new ToMemberToSettledTabs[F]:
          def listChildren(
              parent: CircleMemberId
          ): F[List[SettledTabReadMapper]] =
            getSettledTabsQuery.stream(parent.value, 1024).compile.toList

  private val getSettledTabsQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE to_member = $text
    """
      .query(text *: text *: text *: text *: float4 *: date)
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
  def fromSession[F[_]: Concurrent](
      session: Session[F]
  ): F[SettledTabMapper[F]] =
    (
      session.prepare(getSettledTabQuery),
      session.prepare(createSettledTabQuery),
      session.prepare(deleteSettledTabQuery),
      session.prepare(updateSettledTabFromMemberQuery),
      session.prepare(updateSettledTabToMemberQuery),
      session.prepare(updateSettledTabAmountQuery)
    ).mapN(fromQueries)

  private def fromQueries[F[_]: Concurrent](
      getSettledTabQuery: PreparedQuery[F, String, SettledTabReadMapper],
      createSettledTabQuery: PreparedQuery[
        F,
        SettleExpenseListInput,
        SettledTabReadMapper
      ],
      deleteSettledTabQuery: PreparedCommand[F, String],
      updateSettledTabFromMemberQuery: PreparedCommand[F, (String, String)],
      updateSettledTabToMemberQuery: PreparedCommand[F, (String, String)],
      updateSettledTabAmountQuery: PreparedCommand[F, (Float, String)]
  ): SettledTabMapper[F] =
    new SettledTabMapper[F]:
      def create(input: SettleExpenseListInput): F[SettledTabReadMapper] =
        createSettledTabQuery.unique(input)

      def get(id: String): F[Either[NotFoundError, SettledTabReadMapper]] =
        getSettledTabQuery
          .option(id)
          .map(_.toRight(NotFoundError(s"SettledTab $id not found")))

      def update(b: SettledTabWriteMapper): F[Unit] =
        List(
          b.fromMember.map(updateSettledTabFromMemberQuery.execute(_, b.id)),
          b.toMember.map(updateSettledTabToMemberQuery.execute(_, b.id)),
          b.amount.map(updateSettledTabAmountQuery.execute(_, b.id))
        ).flatten.sequence.void

      def delete(id: String): F[Unit] =
        deleteSettledTabQuery.execute(id).void

  private val getSettledTabQuery: Query[String, SettledTabReadMapper] =
    sql"""
      SELECT id, expense_list_id, from_member, to_member, amount, settled_at
      FROM settled_tabs
      WHERE id = $text
    """
      .query(text *: text *: text *: text *: float4 *: date)
      .to[SettledTabReadMapper]

  private val createSettledTabQuery
      : Query[SettleExpenseListInput, SettledTabReadMapper] =
    sql"""
      INSERT INTO settled_tabs (expense_list_id, from_member, to_member, amount)
      VALUES ($text, $text, $text, $float4)
      RETURNING id, expense_list_id, from_member, to_member, amount, settled_at
    """
      .query(text *: text *: text *: text *: float4 *: date)
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
