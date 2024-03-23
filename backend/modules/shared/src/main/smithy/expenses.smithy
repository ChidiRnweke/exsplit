$version: "2"

namespace exsplit.spec

use alloy#dateFormat
use alloy#simpleRestJson

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
        expenses: ExpenseOut
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

        initialPayer: CircleMember

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
        @httpLabel
        expenseListId: ExpenseListId

        @required
        expense: Expense
    }

    output := {
        @required
        expense: ExpenseOut
    }

    errors: [
        NotFoundError
    ]
}

structure Expense {
    @required
    initialPayer: CircleMember

    @required
    description: String

    @required
    price: Amount

    @required
    date: Date

    @required
    owedToInitialPayer: OwedAmounts
}

@range(min: 0)
float Amount

string ExpenseId

@dateFormat
string Date

structure OwedAmount {
    user: CircleMember
    amount: Amount
}

list OwedAmounts {
    member: OwedAmount
}

structure ExpenseOut {
    link: String

    @required
    initialPayer: CircleMemberOut

    @required
    description: String

    @required
    price: Float

    @required
    date: String

    @required
    owedToInitialPayer: OwedAmountsOut
}

list ExpensesOut {
    member: ExpenseOut
}

structure OwedAmountOut {
    @required
    link: String

    @required
    user: CircleMemberOut

    @required
    amount: Float
}

structure OwedAmountTotalsOut {
    @required
    fromMember: CircleMemberOut

    @required
    toMember: CircleMemberOut

    @required
    amount: Float
}

list OwedAmountsOut {
    member: OwedAmountOut
}
