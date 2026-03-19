# Each service's Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl payment-service         \
    -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]