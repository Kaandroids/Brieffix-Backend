# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Cache dependency layer separately from source code
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S briefix && adduser -S briefix -G briefix

COPY --from=build /app/target/*.jar app.jar

RUN chown briefix:briefix app.jar
USER briefix

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
