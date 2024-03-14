$version: "2"

namespace exsplit.spec
use alloy#simpleRestJson
use alloy#uuidFormat
use alloy#dateFormat
use smithy4s.meta#packedInputs


@simpleRestJson
@httpBearerAuth
service ExpenseService {
    operations: [CreateExpense, GetExpense, UpdateExpense, DeleteExpense]
    errors: [AuthError, ForbiddenError]
}

@readonly
@http(method: "GET", uri: "/circles/{circle_id}/expense_lists/{expense_list_id}/expenses/{id}")
operation GetExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

        @required
        @httpLabel    
        circle_id: CircleId

        @required
        @httpLabel
        expense_list_id: ExpenseListId
    }
    output := {
        expenses: Expenses
    }
    errors: [NotFoundError]
}


@http(method: "PATCH", uri: "/circles/{circle_id}/expense_lists/{expense_list_id}/expenses/{id}")
operation UpdateExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

        @required
        @httpLabel    
        circle_id: CircleId

        @required
        @httpLabel
        expense_list_id: ExpenseListId
        
        initialPayer: String
        description: String
        price: Amount
        date: Date
        owedToInitialPayer: OwedAmounts
    }
    errors: [NotFoundError]

}

@idempotent
@http(method: "DELETE", uri: "/circles/{circle_id}/expense_lists/{expense_list_id}/expenses/{id}")
operation DeleteExpense {
    input := {
        @required
        @httpLabel
        id: ExpenseId

        @required
        @httpLabel    
        circle_id: CircleId

        @required
        @httpLabel
        expense_list_id: ExpenseListId

    }
}

@packedInputs
@http(method: "POST", uri: "/circles/{circle_id}/expense_lists/{expense_list_id}/expenses")
operation CreateExpense {
    input := {
        @required
        expense: Expense

        @required
        @httpLabel    
        circle_id: CircleId

        @required
        @httpLabel
        expense_list_id: ExpenseListId
    } 
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


@range(min: 0)
float Amount

list Expenses {
    member: Expense
}

@uuidFormat
string ExpenseId

@dateFormat
string Date