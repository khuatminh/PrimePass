# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/marketplace-0.0.1-SNAPSHOT.jar app.jar
VOLUME /app/uploads
EXPOSE 8386
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
