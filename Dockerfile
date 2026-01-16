# =============================================================================
# Multi-stage Dockerfile for Az104Simulator (Spring Boot + Gradle)
# Hardened for production with DevSecOps best practices
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Builder - Compile the application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS builder

# Set working directory
WORKDIR /build

# Copy Gradle wrapper and dependencies definition first (for layer caching)
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer if build files don't change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application (skip tests for faster builds, run tests in CI)
RUN ./gradlew bootJar --no-daemon -x test

# Verify JAR was created and extract it for layer optimization
RUN mkdir -p /build/extracted && \
    JAR_FILE=$(ls /build/build/libs/*.jar | grep -v plain | head -1) && \
    echo "Found JAR: ${JAR_FILE}" && \
    java -Djarmode=layertools -jar "${JAR_FILE}" extract --destination /build/extracted

# -----------------------------------------------------------------------------
# Stage 2: Runtime - Minimal secure production image
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21.0.5_11-jre-alpine AS runtime

# Metadata
LABEL maintainer="SingularIT" \
      description="Az104 Exam Simulator - Hardened Spring Boot Application" \
      version="1.0.0" \
      org.opencontainers.image.source="https://github.com/singularit/az104simulator"

# Install runtime dependencies for healthcheck and security
# Note: Using latest stable versions from Alpine repos
RUN apk add --no-cache \
    curl \
    tini && \
    rm -rf /var/cache/apk/*

# Create non-root user and group with fixed UID/GID
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup -h /app -D

# Create necessary directories with proper permissions
RUN mkdir -p /app /app/data /tmp-app && \
    chown -R appuser:appgroup /app /tmp-app && \
    chmod 755 /app && \
    chmod 1777 /tmp-app

# Set working directory
WORKDIR /app

# Copy application layers from builder (optimized for caching)
COPY --from=builder --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/application/ ./

# Switch to non-root user
USER appuser:appgroup

# Environment variables for runtime configuration
ENV SPRING_PROFILES_ACTIVE=default \
    APP_LOCALE_DEFAULT=en \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -XX:+OptimizeStringConcat \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Djava.io.tmpdir=/tmp-app"

# Declare volumes for writable directories (needed for read-only filesystem)
VOLUME ["/tmp-app", "/app/data"]

# Expose application port
EXPOSE 8080

# Healthcheck (polling root endpoint as Actuator is not enabled)
# Adjust timeout and interval as needed for your app startup time
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

# Use tini as init process to handle signals properly
ENTRYPOINT ["/sbin/tini", "--"]

# Run the Spring Boot application
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]

