$version: "2"

namespace exsplit.spec
use alloy#simpleRestJson
use alloy#uuidFormat
use alloy#dateFormat


@simpleRestJson
@httpBearerAuth
service ExpenseService {
    operations: [CreateExpense, GetExpense, UpdateExpense, DeleteExpense]
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
        expenses: Expenses
    }
    errors: [AuthError, NotFoundError]

}


@http(method: "PATCH", uri: "/expenses/{id}")
operation UpdateExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId
        payer: String
        description: String
        price: Amount
        date: Date
        owedToInitialPayer: OwedAmounts
    }
    errors: [AuthError, NotFoundError]

}

@idempotent
@http(method: "DELETE", uri: "/expenses/{id}")
operation DeleteExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

    }
    errors: [AuthError, NotFoundError]
}

structure Expense {
        @required
        initialPayer: User
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
    user: User
    amount: Amount
}

list OwedAmounts {
    member: OwedAmount
}

@http(method: "POST", uri: "/expenses")
operation CreateExpense {
    input: Expense 
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