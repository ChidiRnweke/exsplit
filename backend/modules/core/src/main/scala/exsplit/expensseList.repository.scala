package exsplit.expenseList

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

trait ExpenseListRepository[F[_]]:

  def getExpenseListDetail(
      expenseListId: ExpenseListId
  ): F[Either[NotFoundError, ExpenseListDetailOut]]

  def getExpenseList(
      expenseListId: ExpenseListId
  ): F[Either[NotFoundError, ExpenseListOut]]

  def createExpenseList(
      circle: CircleOut,
      name: String
  ): F[ExpenseListOut]

  def deleteExpenseList(expenseList: ExpenseListOut): F[Unit]
  def getExpenseLists(circleId: CircleOut): F[List[ExpenseListOut]]
  def updateExpenseList(expenseList: ExpenseListOut, name: String): F[Unit]
  def getAllTabs(expenseList: ExpenseListOut): List[SettledTabOut]
  def settleExpenseList(
      expenseList: ExpenseListOut,
      fromMember: CircleMemberOut,
      toMember: CircleMemberOut,
      amount: Amount
  ): F[Unit]

case class ExpenseListQueryPreparer[F[_]](session: Session[F]):

  def getExpenseListQuery: F[PreparedQuery[F, String, ExpenseListOut]] =
    val query = sql"""
      SELECT id, name, circle_id, circle_name, circle_description
      FROM expense_list_circle_view
      WHERE id = $text
    """
      .query(varchar ~ varchar ~ varchar ~ varchar ~ varchar)
      .map:
        case id ~ name ~ circleId ~ circleName ~ circleDescription =>
          val circle = CircleOut(circleId, circleName, circleDescription)
          ExpenseListOut(id, name, circle)
    session.prepare(query)

  def getExpenseListsQuery: F[PreparedQuery[F, String, ExpenseListOut]] =
    val query = sql"""
      SELECT id, name, circle_id, circle_name, circle_description
      FROM expense_list_circle_view
      WHERE circle_id = $text
    """
      .query(varchar ~ varchar ~ varchar ~ varchar ~ varchar)
      .map:
        case id ~ name ~ circleId ~ circleName ~ circleDescription =>
          val circle = CircleOut(circleId, circleName, circleDescription)
          ExpenseListOut(id, name, circle)
    session.prepare(query)

  def createExpenseListCommand
      : F[PreparedCommand[F, (String, String, String)]] =
    val command = sql"""
      INSERT INTO expense_lists (id, circle_id, name)
      VALUES ($text, $text, $text)
    """.command
    session.prepare(command)

  def updateExpenseListCommand: F[PreparedCommand[F, (String, String)]] =
    val command = sql"""
      UPDATE expense_lists
      SET name = $text
      WHERE id = $text
    """.command
    session.prepare(command)

  def deleteExpenseListCommand: F[PreparedCommand[F, String]] =
    val command = sql"""
      DELETE FROM expense_lists
      WHERE id = $text
    """.command
    session.prepare(command)

  def getExpenseListDetail: F[PreparedQuery[F, String, ExpenseListDetailOut]] =
    val query = sql"""
      SELECT id, name, circle_id, circle_name, circle_description, total_owed,
      paid_by,
      FROM expense_list_detail_circle_view
      WHERE id = $text
    """
      .query(varchar ~ varchar ~ varchar ~ varchar ~ varchar ~ numeric)
      .map:
        case id ~ name ~ circleId ~ circleName ~ circleDescription ~ totalOwed =>
          val circle = CircleOut(circleId, circleName, circleDescription)
          val paidBy = ???
          val summary = ExpenseListOut(id, name, circle)
    // TODO: Implement this
    ???
