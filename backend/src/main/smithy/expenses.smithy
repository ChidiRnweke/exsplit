$version: "2"

namespace exsplit.spec

use alloy#dateFormat
use alloy#simpleRestJson
use alloy#uuidFormat
use smithy4s.meta#packedInputs

@simpleRestJson
@httpBearerAuth
service ExpenseService {
    operations: [
        CreateExpense
        GetExpense
        UpdateExpense
        DeleteExpense
    ]
    errors: [
        AuthError
        ForbiddenError
    ]
}

@readonly
@http(method: "GET", uri: "/circles/{circleId}/expense_lists/{expenseListId}/expenses/{id}")
operation GetExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        expenseListId: ExpenseListId
    }

    output := {
        @required
        expenses: Expense
    }

    errors: [
        NotFoundError
    ]
}

@http(method: "PATCH", uri: "/circles/{circleId}/expense_lists/{expenseListId}/expenses/{id}")
operation UpdateExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        expenseListId: ExpenseListId

        initialPayer: String

        description: String

        price: Amount

        date: Date

        owedToInitialPayer: OwedAmounts
    }

    errors: [
        NotFoundError
    ]
}

@idempotent
@http(method: "DELETE", uri: "/circles/{circleId}/expense_lists/{expenseListId}/expenses/{id}")
operation DeleteExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        expenseListId: ExpenseListId
    }
}

@packedInputs
@http(method: "POST", uri: "/circles/{circleId}/expense_lists/{expenseListId}/expenses")
operation CreateExpense {
    input := {
        @required
        expense: Expense

        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        expenseListId: ExpenseListId
    }
}

structure Expense {
    @required
    initialPayer: PublicUserData

    @required
    description: String

    @required
    price: Amount

    @required
    date: Date

    @required
    owedToInitialPayer: OwedAmounts
}

structure OwedAmount {
    user: PublicUserData
    amount: Amount
}

list OwedAmounts {
    member: OwedAmount
}

@range(min: 0)
float Amount

list Expenses {
    member: Expense
}

@uuidFormat
string ExpenseId

@dateFormat
string Date
