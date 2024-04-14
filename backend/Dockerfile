FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.9.1_3.3.0 as build

COPY . /build

WORKDIR /build

RUN sbt universal:packageZipTarball

RUN tar -xvf /build/modules/core/target/universal/exsplit.tgz 

FROM eclipse-temurin:17-jre as runtime

COPY --from=build /build/exsplit /app

ENTRYPOINT ["bash", "/app/bin/exsplit"]

