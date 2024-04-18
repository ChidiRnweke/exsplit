$version: "2"

namespace exsplit.spec

use alloy#dateFormat
use alloy#simpleRestJson
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
        InvalidTokenError
    ]
}

@readonly
@http(method: "GET", uri: "/api/expenses/{id}")
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

@http(method: "PATCH", uri: "/api/expenses/{id}")
operation UpdateExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId
        
        @required
        paidBy: CircleMemberId

        description: String

        price: Amount
        
        @timestampFormat("epoch-seconds")
        date: Timestamp

        owedToPayer: OwedAmounts
    }

    errors: [
        NotFoundError
    ]
}

@idempotent
@http(method: "DELETE", uri: "/api/expenses/{id}")
operation DeleteExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId
    }
}

@http(method: "POST", uri: "/api/expense_lists/{expenseListId}/expenses")
operation CreateExpense {
    input := {
        @required
        @httpLabel
        expenseListId: ExpenseListId

        @required
        paidBy: CircleMemberId

        @required
        description: String

        @required
        price: Amount

        @required
        @timestampFormat("epoch-seconds")
        date: Timestamp

        @required
        owedToPayer: OwedAmounts
    }

    output := {
        @required
        expense: ExpenseOut
    }

    errors: [
        NotFoundError
    ]
}

@range(min: 0)
float Amount

string ExpenseId

structure OwedAmount {
    @required
    circleMemberId: CircleMemberId
    
    @required
    amount: Amount
}

list OwedAmounts {
    member: OwedAmount
}

structure ExpenseOut {
    @required
    id: String

    @required
    paidBy: CircleMemberOut

    @required
    description: String

    @required
    price: Float

    @required
    @timestampFormat("epoch-seconds")
    date: Timestamp

    @required
    owedToPayer: OwedAmountsOut
}

list ExpensesOut {
    member: ExpenseOut
}

structure OwedAmountOut {
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
