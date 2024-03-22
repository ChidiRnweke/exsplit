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
operation GetCircle{
    input := {
        @required
        @httpLabel
        circleId: CircleId
    }

    output := {
        @required
        circle: Circle
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
@http(method: "PUT", uri: "/circles/{circleId}")
operation UpdateCircle {
    input := {

        @required
        @httpLabel
        circleId: CircleId

        @required
        name: String

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

@idempotent
@http(method: "DELETE", uri: "/circles/{circleId}/users/{userId}")
operation RemoveUserFromCircle {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        userId: UserId
    }
}

@idempotent
@http(method: "PATCH", uri: "/circles/{circleId}/users/{userId}")
operation ChangeDisplayName {
    input := {
        @required
        @httpLabel
        circleId: CircleId

        @required
        @httpLabel
        userId: UserId

        @required
        displayName: String
    }
}

list Circles {
    member: Circle
}

structure Circle {
    @required
    id: CircleId

    @required
    name: String

    description: String
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

string CircleId
