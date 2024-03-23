$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson

@simpleRestJson
@httpBearerAuth
service ExpenseListService {
    operations: [
        GetExpenseLists
        GetExpenseList
        CreateExpenseList
        UpdateExpenseList
        DeleteExpenseList
        SettleExpenseList
        GetSettledExpenseLists
    ]
    errors: [
        AuthError
        NotFoundError
        ForbiddenError
    ]
}

@readonly
@http(method: "GET", uri: "/circles/{circleId}/expenseLists")
operation GetExpenseLists {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output := {
        @required
        expenseLists: ExpenseListsOut
    }
}

@readonly
@http(method: "GET", uri: "/expenseLists/{expenseListId}/settle")
operation GetSettledExpenseLists {
    input := {
        @required
        @httpLabel
        expenseListId: ExpenseListId
    }

    output := {
        @required
        settledTabs: SettledTabsOut
    }
}

@http(method: "POST", uri: "/expenseLists/{expenseListId}/settle")
operation SettleExpenseList {
    input := {
        @required
        @httpLabel
        expenseListId: ExpenseListId

        @required
        fromMemberId: CircleMemberId

        @required
        toMemberId: CircleMemberId

        @required
        amount: Amount
    }
}

@readonly
@http(method: "GET", uri: "/expenseLists/{expenseListId}")
operation GetExpenseList {
    input := {
        @required
        @httpLabel
        expenseListId: ExpenseListId

        @httpQuery("onlyOutstanding")
        onlyOutstanding: Boolean
    }

    output := {
        @required
        expenseListDetail: ExpenseListDetailOut
    }
}

@http(method: "POST", uri: "/circles/{circleId}/expenseLists")
operation CreateExpenseList {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        name: String
    }

    output := {
        @required
        expenseList: ExpenseListOut
    }
}

@idempotent
@http(method: "PUT", uri: "/expenseLists/{id}")
operation UpdateExpenseList {
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
operation DeleteExpenseList {
    input := {
        @required
        @httpLabel
        id: ExpenseListId
    }
}

structure SettledTabsOut {
    @required
    settledTabs: SettledTabs
}

list SettledTabs {
    member: SettledTabOut
}

structure SettledTabOut {
    @required
    settledTabId: String

    @required
    date: String

    @required
    fromMember: CircleMemberOut

    @required
    toMember: CircleMemberOut

    @required
    amount: Float
}

structure ExpenseListOut {
    @required
    expenseListId: String

    @required
    name: String

    @required
    circle: CircleOut
}

structure ExpenseListDetailOut {
    @required
    summary: ExpenseListOut

    @required
    expenses: ExpensesOut

    @required
    totalExpense: Float

    @required
    totalOwed: OwedAmountsTotalsOut
}

list OwedAmountsTotalsOut {
    member: OwedAmountTotalsOut
}   

structure ExpenseListsOut {
    @required
    expenseLists: ExpenseLists
}

list ExpenseLists {
    member: ExpenseListOut
}

string ExpenseListId
