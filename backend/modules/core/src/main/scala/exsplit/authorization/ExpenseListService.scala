package exsplit.authorization

import cats.MonadThrow
import exsplit.authorization.extractors._
import exsplit.spec._
import exsplit.circles._
import cats.effect._
import cats.syntax.all._
import cats._
import skunk.Session
import exsplit.datamapper.user.UserMapper
import exsplit.datamapper.circles._
import exsplit.expenseList.ExpenseListEntryPoint
import exsplit.datamapper.expenseList.ExpenseListRepository
import exsplit.datamapper.expenses.ExpenseRepository

object ExpenseListServiceWithAuth:
  def fromSession[F[_]: Concurrent: Parallel](
      userInfo: F[Email],
      session: Session[F]
  ): F[ExpenseListService[F]] =
    for
      service <- ExpenseListEntryPoint.fromSession(session)
      userMapper <- UserMapper.fromSession(session)
      circlesRepo <- CirclesRepository.fromSession(session)
      membersRepo <- CircleMembersRepository.fromSession(session)
      expenseListRepo <- ExpenseListRepository.fromSession(session)
      expenseRepo <- ExpenseRepository.fromSession(session)
      circlesAuth = CirclesAuth(circlesRepo, userMapper)
      circleMemberAuth = CircleMemberAuth(userMapper, circlesRepo, membersRepo)
      expenseListAuth = ExpenseListAuth(
        userMapper,
        circlesRepo,
        expenseListRepo
      )
      expenseAuth = ExpenseAuth(
        userMapper,
        circlesRepo,
        expenseListRepo,
        expenseRepo
      )
    yield ExpenseListServiceWithAuth(
      userInfo,
      circlesAuth,
      circleMemberAuth,
      expenseListAuth,
      expenseAuth,
      service
    )

case class ExpenseListServiceWithAuth[F[_]: Monad: Parallel](
    userInfo: F[Email],
    circlesAuth: CirclesAuth[F],
    circleMemberAuth: CircleMemberAuth[F],
    expenseListAuth: ExpenseListAuth[F],
    expenseAuth: ExpenseAuth[F],
    service: ExpenseListService[F]
) extends ExpenseListService[F]:

  def createExpenseList(
      circleId: CircleId,
      name: String
  ): F[CreateExpenseListOutput] =
    for
      _ <- circlesAuth.authCheck(userInfo, circleId)
      res <- service.createExpenseList(circleId, name)
    yield res

  def deleteExpenseList(id: ExpenseListId): F[Unit] =
    for
      expenseList <- expenseListAuth.authCheck(userInfo, id)
      _ <- service.deleteExpenseList(id)
    yield ()
  def getExpenseList(
      expenseListId: ExpenseListId,
      onlyOutstanding: Option[Boolean]
  ): F[GetExpenseListOutput] =
    for
      _ <- expenseListAuth.authCheck(userInfo, expenseListId)
      res <- service.getExpenseList(expenseListId, onlyOutstanding)
    yield res
  def getExpenseLists(circleId: CircleId): F[GetExpenseListsOutput] =
    for
      _ <- circlesAuth.authCheck(userInfo, circleId)
      res <- service.getExpenseLists(circleId)
    yield res

  def getSettledExpenseLists(
      expenseListId: ExpenseListId
  ): F[GetSettledExpenseListsOutput] =
    for
      _ <- expenseListAuth.authCheck(userInfo, expenseListId)
      res <- service.getSettledExpenseLists(expenseListId)
    yield res

  def settleExpenseList(
      expenseListId: ExpenseListId,
      fromMemberId: CircleMemberId,
      toMemberId: CircleMemberId,
      amount: Amount
  ): F[Unit] =
    for
      _ <- (
        expenseListAuth.authCheck(userInfo, expenseListId),
        circleMemberAuth.authCheck(userInfo, fromMemberId),
        circleMemberAuth.authCheck(userInfo, toMemberId)
      ).parTupled
      _ <- service.settleExpenseList(
        expenseListId,
        fromMemberId,
        toMemberId,
        amount
      )
    yield ()
  def updateExpenseList(id: ExpenseListId, name: String): F[Unit] =
    for
      _ <- expenseListAuth.authCheck(userInfo, id)
      _ <- service.updateExpenseList(id, name)
    yield ()
