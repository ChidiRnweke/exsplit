package exsplit.domainmapper

import exsplit.spec._
import exsplit.datamapper.expenseList._
import cats.effect._
import cats.syntax.all._
import cats._
import exsplit.datamapper.expenses._
import exsplit.domainmapper._
import exsplit.datamapper.circles._

extension [F[_]: MonadThrow](expenseMapper: ExpenseListRepository[F])

  /** Retrieves an expense list by its ID and maps it to an ExpenseListOut
    * object. If an error is encountered, it is rethrown. In that case the error
    * is caught in the API layer by smithy4s error handling middleware and
    * converted to an appropriate HTTP response.
    *
    * @param id
    *   The ID of the expense list.
    * @return
    *   The mapped ExpenseListOut object.
    */
  def getExpenseListOut(id: ExpenseListId): F[ExpenseListOut] =
    expenseMapper.get(id).rethrow.map(_.toExpenseListOut)

  /** Creates a new expense list with the given circle ID and name. This method
    * skips the need to create an input object and directly creates the expense
    * list.
    *
    * @param circleId
    *   The ID of the circle associated with the expense list.
    * @param name
    *   The name of the expense list.
    * @return
    *   The created ExpenseListOut object.
    */
  def createExpenseList(circleId: CircleId, name: String): F[ExpenseListOut] =
    val input = CreateExpenseListInput(circleId, name)
    expenseMapper.create(input).map(_.toExpenseListOut)

extension (expenseList: ExpenseListReadMapper)
  /** Maps an ExpenseListReadMapper object to an ExpenseListOut object. The
    * former is the representation of an expense list in the database, while the
    * latter is the representation of an expense list in the application.
    *
    * @return
    *   The mapped ExpenseListOut object.
    */
  def toExpenseListOut: ExpenseListOut =
    ExpenseListOut(expenseList.id, expenseList.name, expenseList.circleId)

extension (expenseLists: List[ExpenseListReadMapper])
  /** Maps a list of ExpenseListReadMapper objects to a list of ExpenseListOut
    * objects. The former is the representation of expense lists in the
    * database, while the latter is the representation of expense lists in the
    * application.
    *
    * @return
    *   The mapped list of ExpenseListOut objects.
    */
  def toExpenseListOuts: List[ExpenseListOut] =
    expenseLists.map(_.toExpenseListOut)

extension [F[_]: MonadThrow: Parallel](repo: ExpenseListRepository[F])

  /** Retrieves the details of an expense list, including the associated
    * expenses, total expense amount, and total owed amount. This object is an
    * aggregate that contains all the necessary information about an expense
    * lists and thus needs several repositories to be passed in.
    *
    * @param id
    *   The ID of the expense list.
    * @param expenseRepo
    *   The expense repository.
    * @param owedAmountRepo
    *   The owed amount repository.
    * @param circleMembersRepo
    *   The circle members repository.
    * @return
    *   The ExpenseListDetailOut object containing the expense list details.
    */
  def getExpenseListDetail(
      id: ExpenseListId,
      expenseRepo: ExpenseRepository[F],
      owedAmountRepo: OwedAmountRepository[F],
      circleMembersRepo: CircleMembersRepository[F]
  ): F[ExpenseListDetailOut] =
    (
      repo.getExpenseListOut(id),
      expenseRepo.listExpenseOut(id, circleMembersRepo, owedAmountRepo)
    ).parFlatMapN: (expenseList, expenses) =>
      for
        owedAmounts <- expenses.parFlatTraverse(e =>
          owedAmountRepo.getOwedAmounts(ExpenseId(e.id))
        )
        owedTotal = owedAmounts.toTotalOwed
        totalExpense = expenses.map(_.price).sum
      yield ExpenseListDetailOut(
        expenseList,
        expenses,
        totalExpense,
        owedTotal
      )

  /** Retrieves an expense list by its ID and returns it as an ExpenseListOut
    * object. If the expense list is not found, an error is thrown. The error is
    * caught in the API layer by smithy4s error handling middleware and
    * converted to an appropriate HTTP response.
    *
    * @param expenseListId
    *   The ID of the expense list.
    * @return
    *   The ExpenseListOut object.
    */
  def withValidExpenseList[A](expenseListId: ExpenseListId): F[ExpenseListOut] =
    repo.getExpenseListOut(expenseListId)
