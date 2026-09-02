# syntax=docker/dockerfile:1

# ============================================================================
# Shipment Service - production Dockerfile
#   многозадачная сборка, минимальный рантайм, non-root пользователь,
#   healthcheck, graceful shutdown
# ============================================================================

# ---- Этап 1: сборка ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# Используем кэш слоёв Docker: сначала резолвим зависимости.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Этап 2: рантайм ----
# eclipse-temurin:21-jre-alpine — минимальный JRE-образ с CA-сертификатами.
FROM eclipse-temurin:21-jre-alpine AS runtime

# Создаём non-root пользователя.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=build /build/target/shipment-service-*.jar /app/app.jar

# Минимальный слой для осведомлённости JVM о контейнере + graceful shutdown.
# Флаги JVM в ENV, чтобы их можно было переопределить во время выполнения.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Healthcheck: зависит от публикуемого liveness-эндпоинта Actuator.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q -O - "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health/liveness" > /dev/null || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
