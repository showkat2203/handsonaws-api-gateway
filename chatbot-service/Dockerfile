## Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY plm-backend/pom.xml plm-backend/pom.xml
COPY plm-backend/plm-service plm-backend/plm-service
COPY chatbot-service chatbot-service

RUN mvn -q -f plm-backend/pom.xml -N install \
 && mvn -q -f plm-backend/plm-service/pom.xml -DskipTests install \
 && mvn -q -f chatbot-service/pom.xml -DskipTests package

## Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/chatbot-service/target/chatbot-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
