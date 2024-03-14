$version: "2"

namespace exsplit.spec
use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
@httpBearerAuth
service ExpenseService {
    operations: [CreateExpense, GetExpense, UpdateExpense, DeleteExpense]
}

structure owedAmount {
    user: User
    amount: Amount
}

list owedAmounts {
    member: owedAmount
}

@http(method: "POST", uri: "/expenses")
operation CreateExpense {
    input: Expense 
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

structure Expense {
        @required
        initialPayer: User
        @required
        description: String
        @required
        price: Amount
        @required
        date: Timestamp
        @required
        owedToInitialPayer: owedAmounts
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
        date: Timestamp
        payers: owedAmounts
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

@range(min: 0)
float Amount

list Expenses {
    member: Expense
}

@uuidFormat
string ExpenseId
