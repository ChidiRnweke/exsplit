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
        RemoveUserFromCircle
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
}

@readonly
@http(method: "GET", uri: "/users/{userId}/circles")
operation ListCirclesForUser {
    input := {
        @required
        @httpLabel
        userId: UserId
    }

    output := {
        @required
        circles: CirclesOut
    }
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
}

@idempotent
@http(method: "PUT", uri: "/circles/{circleId}")
operation UpdateCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        circleName: String

        description: String
    }
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
@http(method: "PUT", uri: "/circles/{circleId}/members")
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
}

@readonly
@http(method: "GET", uri: "/circles/{circleId}/members")
operation ListCircleMembers {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output := {
        @required
        members: MembersListOut
    }
}

@idempotent
@http(method: "DELETE", uri: "/circles/{circleId}/members/{memberId}")
operation RemoveUserFromCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        memberId: CircleMemberId
    }
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
    link: String

    @required
    circleId: String

    @required
    circleName: String

    @required
    description: String
}

structure CircleMember {
    @required
    userId: UserId

    @required
    displayName: String
}

structure CircleMemberOut {
    @required
    link: String

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
