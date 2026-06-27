# ============================================================
# DevOps Training Lab 1 - Java Spring Boot + Maven
# Multi-Stage Dockerfile
# ============================================================

# ---- Stage 1: BUILD ----------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first — download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build jar
COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Stage 2: RUNTIME (lean) -------------------------------
FROM eclipse-temurin:17-jre-alpine

# Security: create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the compiled jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set correct ownership
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# JVM tuned for containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
