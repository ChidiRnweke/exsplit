$version: "2"

namespace exsplit.spec

use alloy#dateFormat
use alloy#simpleRestJson
use alloy#uuidFormat

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
@http(method: "GET", uri: "/expenses/{id}")
operation GetExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId
    }

    output := {
        @required
        expenses: Expense
    }

    errors: [
        NotFoundError
    ]
}

@http(method: "PATCH", uri: "/expenses/{id}")
operation UpdateExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

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
@http(method: "DELETE", uri: "/expenses/{id}")
operation DeleteExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

    }
}

@http(method: "POST", uri: "/expense_lists/{expenseListId}/expenses")
operation CreateExpense {
    input := {
        @required
        expense: Expense

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
