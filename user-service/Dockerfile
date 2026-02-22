# Pre-built JAR is copied in — run `mvn package -pl user-service` before `cdk deploy`
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY target/user-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
