## Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY plm-backend/pom.xml plm-backend/pom.xml
COPY plm-backend/plm-service plm-backend/plm-service
COPY plm-backend/plm-api plm-backend/plm-api

RUN mvn -q -f plm-backend/pom.xml -N install \
 && mvn -q -f plm-backend/plm-service/pom.xml -DskipTests install \
 && mvn -q -f plm-backend/plm-api/pom.xml -DskipTests package

## Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/plm-backend/plm-api/target/plm-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
