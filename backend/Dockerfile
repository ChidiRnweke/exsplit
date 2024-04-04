FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.9.1_3.3.0 as build

COPY . /build

WORKDIR /build

RUN sbt clean

RUN sbt core/assembly

FROM eclipse-temurin:17-jre-alpine as runtime

WORKDIR /app

COPY --from=build build/modules/core/target/scala-3.3.1/exsplit.jar  /app/exsplit.jar

ENTRYPOINT ["java", "-jar", "exsplit.jar"]

