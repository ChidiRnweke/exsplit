$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson

@simpleRestJson
@httpBearerAuth
service ExpenseListService {
    operations: [
        getExpenseLists
        getExpenseListById
        createExpenseList
        updateExpenseList
        deleteExpenseList
    ]
    errors: [
        AuthError
        NotFoundError
        ForbiddenError
    ]
}

@readonly
@http(method: "GET", uri: "/circles/{circleId}/expenseLists")
operation getExpenseLists {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output := {
        @required
        expenseLists: ExpenseLists
    }
}

@readonly
@http(method: "GET", uri: "/expenseLists/{expenseListId}")
operation getExpenseListById {
    input := {
        @required
        @httpLabel
        expenseListId: ExpenseListId

    }

    output := {
        @required
        expenseListDetail: ExpenseListDetail
    }
}

@http(method: "POST", uri: "/circles/{circleId}/expenseLists")
operation createExpenseList {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        name: String
    }

    output := {
        @required
        expenseListDetail: ExpenseList
    }
}

@idempotent
@http(method: "PUT", uri: "/expenseLists/{id}")
operation updateExpenseList {
    input := {
        @required
        @httpLabel
        id: ExpenseListId

        @required
        name: String
    }
}

@idempotent
@http(method: "DELETE", uri: "/expenseLists/{id}")
operation deleteExpenseList {
    input := {
        @required
        @httpLabel
        id: ExpenseListId
    }
}

structure ExpenseList {
    @required
    id: ExpenseListId

    @required
    name: String

    @required
    circleId: CircleId
}

structure ExpenseListDetail {
    @required
    id: ExpenseListId

    @required
    name: String

    @required
    circleId: CircleId

    expenses: Expenses
}

list ExpenseLists {
    member: ExpenseList
}

string ExpenseListId
