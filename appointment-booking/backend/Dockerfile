# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
# Use Eclipse Temurin 17 (LTS) to compile and package the fat JAR.
# Maven wrapper downloads all dependencies into the Docker layer cache,
# so subsequent builds only re-download on pom.xml changes.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first (maximise layer cache reuse)
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies in a separate layer
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source code and build (skip tests — tests run in CI, not in Docker build)
COPY src ./src
RUN ./mvnw package -DskipTests -q


# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime
# Use the slim JRE image — no compiler, no build tools in production.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS production

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Give ownership to appuser
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 5000

# Health check — calls the Spring Boot health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:5000/actuator/health || exit 1

# JVM flags:
#   -XX:+UseContainerSupport       — respect container CPU/memory limits
#   -XX:MaxRAMPercentage=75.0      — use 75% of container RAM for heap
#   -Djava.security.egd            — faster SecureRandom in containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
