$version: "2"

namespace exsplit.spec

use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service CirclesService {
    operations: [
        GetCircles
        AddUserToCircle
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
@http(method: "GET", uri: "/users/{userId}/circles")
operation GetCircles {
    input := {
        @required
        @httpLabel
        userId: UserId
    }

    output := {
        @required
        circles: Circles
    }
}

@http(method: "POST", uri: "/users/{userId}/circles")
operation CreateCircle {
    input := {
        @required
        @httpLabel
        userId: UserId

        @required
        name: String

        description: String
    }
}

@idempotent
@http(method: "PUT", uri: "/users/{userId}/circles/{circleId}")
operation UpdateCircle {
    input := {
        @required
        @httpLabel
        userId: UserId

        @required
        @httpLabel
        circleId: CircleId

        @required
        name: String

        description: String
    }
}

@idempotent
@http(method: "DELETE", uri: "/users/{userId}/circles/{circleId}")
operation DeleteCircle {
    input := {
        @required
        @httpLabel
        userId: UserId

        @required
        @httpLabel
        circleId: CircleId
    }
}

@idempotent
@http(method: "PUT", uri: "/circles/{circleId}/users")
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
@http(method: "GET", uri: "/circles/{circleId}/users")
operation ListCircleMembers {
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output := {
        @required
        members: Members
    }
}

list Circles {
    member: Circle
}

@uuidFormat
string CircleId

structure Circle {
    @required
    id: CircleId

    @required
    name: String

    description: String

    @required
    members: Members
}

list Members {
    member: CircleMember
}

structure CircleMember {
    @required
    userId: String

    @required
    displayName: String
}
