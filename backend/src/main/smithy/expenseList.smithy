$version: "2"

namespace exsplit.spec
use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
@httpBearerAuth
service ExpenseListService {
    operations: [getExpenseLists, getExpenseListById, createExpenseList, updateExpenseList, deleteExpenseList]
    errors: [AuthError, NotFoundError]
}

@readonly
@http(method: "GET", uri: "/circles/{circle_id}/expense_lists")
operation getExpenseLists {
    input := {
        @required
        @httpLabel
        circle_id: CircleId

    }
    output := {
        @required
        expense_lists: ExpenseLists
    }
}

@readonly
@http(method: "GET", uri: "/circles/{circle_id}/expense_lists/{expense_list_id}")
operation getExpenseListById {
    input := {
        @required
        @httpLabel
        expense_list_id: ExpenseListId

        @required
        @httpLabel
        circle_id: CircleId

    }
    output := {
        @required
        expense_list: ExpenseListDetail
    }
}


@http(method: "POST", uri: "/circles/{circle_id}/expense_lists")
operation createExpenseList {
    input := {

        @required
        @httpLabel
        circle_id: CircleId
        @required
        name: String

    }
    output := {
        @required
        expense_list: ExpenseList
    }
}

@idempotent
@http(method: "PUT", uri: "/circles/{circle_id}/expense_lists/{id}")
operation updateExpenseList {
    input := {
        @required
        @httpLabel
        id: ExpenseListId


        @required
        @httpLabel
        circle_id: CircleId
        
        @required
        name: String
    }
}

@idempotent
@http(method: "DELETE", uri: "/circles/{circle_id}/expense_lists/{id}")
operation deleteExpenseList {
    input := {
        @required
        @httpLabel
        id: ExpenseListId

        @required
        @httpLabel
        circle_id: CircleId
    }
}

structure ExpenseList {
    @required
    id: ExpenseListId
    @required
    name: String
    @required
    circle_id: CircleId
}

structure ExpenseListDetail {
    @required
    id: ExpenseListId
    @required
    name: String
    @required
    circle_id: CircleId
    expenses: Expenses
}

list ExpenseLists {
    member: ExpenseList
}


@uuidFormat
string ExpenseListId