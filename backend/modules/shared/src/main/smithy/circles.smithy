$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson

@simpleRestJson
@httpBearerAuth
service CirclesService {
    operations: [
        GetCircle
        ListCirclesForUser
        AddUserToCircle
        RemoveMemberFromCircle
        ChangeDisplayName
        CreateCircle
        UpdateCircle
        DeleteCircle
        ListCircleMembers
    ]
    errors: [
        AuthError
        ForbiddenError
    ]
}

@readonly
@http(method: "GET", uri: "/circles/{circleId}")
operation GetCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output := {
        @required
        circle: CircleOut
    }
    errors: [NotFoundError]
}

@readonly
@http(method: "GET", uri: "/users/{userId}/circles")
operation ListCirclesForUser {
    input := {
        @required
        @httpLabel
        userId: UserId
    }

    output: CirclesOut
    errors: [NotFoundError]

}

@http(method: "POST", uri: "/users/{userId}/circles")
operation CreateCircle {
    input := {
        @required
        @httpLabel
        userId: UserId

        @required
        displayName: String

        @required
        circleName: String

        description: String
    }

    output := {
        @required
        circle: CircleOut
    }
    errors: [NotFoundError]
}

@idempotent
@http(method: "PATCH", uri: "/circles/{circleId}")
operation UpdateCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        circleName: String

        description: String
    }
    errors: [NotFoundError]
}

@idempotent
@http(method: "DELETE", uri: "/circles/{circleId}")
operation DeleteCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }
}

@idempotent
@http(method: "POST", uri: "/circles/{circleId}/members")
operation AddUserToCircle {
    input := {
        @required
        userId: UserId

        @required
        displayName: String

        @required
        @httpLabel
        circleId: CircleId
    }
    errors: [NotFoundError]
}

@readonly
@http(method: "GET", uri: "/circles/{circleId}/members")
operation ListCircleMembers {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output: MembersListOut
    errors: [NotFoundError]
}

@idempotent
@http(method: "DELETE", uri: "/circles/{circleId}/members/{memberId}")
operation RemoveMemberFromCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        memberId: CircleMemberId
    }
    errors: [NotFoundError]
}

@idempotent
@http(method: "PATCH", uri: "/circles/{circleId}/members/{memberId}")
operation ChangeDisplayName {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        memberId: CircleMemberId

        @required
        displayName: String
    }
    errors: [NotFoundError]
}

structure CirclesOut {
    @required
    circles: Circles
}

list Circles {
    member: CircleOut
}

structure CircleOut {
    @required
    circleId: String

    @required
    circleName: String

    @required
    description: String
}
structure CircleMemberOut {
    @required
    circleMemberId: String

    @required
    displayName: String
}

structure MembersListOut {
    @required
    members: MembersOut
}

list MembersOut {
    member: CircleMemberOut
}

string CircleId

string CircleMemberId
