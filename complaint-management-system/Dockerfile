FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN cp src/main/resources/application.properties.example src/main/resources/application.properties
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21.0.12_8-jre
WORKDIR /app

RUN mkdir -p /app/uploads

COPY --from=build /build/target/complaint-management-system-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
