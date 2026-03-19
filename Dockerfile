# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Gradle wrapper and config first for layer caching
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Copy module build files
COPY shared/build.gradle.kts shared/
COPY cli/build.gradle.kts cli/
COPY server/build.gradle.kts server/

# Download dependencies (cached layer if build files don't change)
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Copy source
COPY shared/src shared/src
COPY server/src server/src
COPY cli/src cli/src

# Build the server distribution
RUN ./gradlew :server:installDist --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the server distribution from builder
COPY --from=builder /build/server/build/install/server/ .

# Create config directory (can be overridden by volume/ConfigMap mount)
RUN mkdir -p /.claude/config && chown -R appuser:appgroup /.claude /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["bin/server"]
