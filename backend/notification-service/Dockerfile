# =============================================================================
# Alzheimer Care Platform - Notification Service Dockerfile
# Multi-stage build for optimized production image
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build Stage
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached layer if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application without compiling/running tests during image build.
RUN mvn clean package -Dmaven.test.skip=true -B

# -----------------------------------------------------------------------------
# Stage 2: Runtime Stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

# Metadata labels
LABEL maintainer="AlzCare Team" \
      application="notification-service" \
      version="1.0.0" \
      description="Notification management service for AlzCare platform"

# Create non-root user for security
RUN addgroup -S alzcare && adduser -S alzcare -G alzcare

# Install wget for health checks
RUN apk add --no-cache wget

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set proper ownership
RUN chown -R alzcare:alzcare /app

# Switch to non-root user
USER alzcare

# Expose application port
EXPOSE 8004

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8004/actuator/health || exit 1

# JVM configuration for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker \
               -Dlogging.level.org.springframework.web=INFO"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
