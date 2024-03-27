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
