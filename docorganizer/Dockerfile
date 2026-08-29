# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the descriptor first so the dependency layer is cached across source changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------- runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime

# Run as a non-root user; the storage volume is owned by it.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

RUN mkdir -p /app/storage && chown -R app:app /app

USER app

ENV STORAGE_DIR=/app/storage \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

VOLUME ["/app/storage"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
