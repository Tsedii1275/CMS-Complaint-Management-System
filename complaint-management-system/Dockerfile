FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21.0.12_8-jre
WORKDIR /app

RUN groupadd --system cms \
    && useradd --system --gid cms --home-dir /app --no-create-home cms \
    && mkdir -p /app/uploads \
    && chown -R cms:cms /app

COPY --from=build --chown=cms:cms /build/target/complaint-management-system-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER cms

ENTRYPOINT ["java", "-jar", "app.jar"]
