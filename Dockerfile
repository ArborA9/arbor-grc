# ---------------------------------------------------------------------------
# Arbor GRC Multi-Stage Dockerfile
# Stage 1: Build Frontend (Vite + React + TypeScript)
# Stage 2: Build Backend (Maven + Spring Boot 3 + Java 21)
# Stage 3: Runtime Image (Eclipse Temurin JRE 21 Alpine)
# ---------------------------------------------------------------------------

# --- STAGE 1: Frontend Build ---
FROM node:20-alpine AS frontend-builder
WORKDIR /build/frontend

# Install dependencies with package locks
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# Copy source and build static SPA bundle
COPY frontend/ ./
RUN npm run build

# --- STAGE 2: Backend Maven Build ---
FROM maven:3.9.9-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /build

# Copy pom.xml and source
COPY pom.xml ./
COPY src ./src
RUN mkdir -p docs/frameworks

# Integrate compiled frontend bundle into Spring Boot static resources
COPY --from=frontend-builder /build/frontend/dist ./src/main/resources/static/

# Package executable Spring Boot jar
RUN mvn clean package -DskipTests -B

# --- STAGE 3: Runtime Container ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for container healthcheck
RUN apk add --no-cache curl

# Create unprivileged system group and user
RUN addgroup -S arbor && adduser -S arbor -G arbor

# Create local data directories for runtime operations
RUN mkdir -p /app/.run/evidence /app/.run/audit-reports /app/.run/h2 && \
    chown -R arbor:arbor /app

# Copy packaged jar from builder stage
COPY --from=backend-builder --chown=arbor:arbor /build/target/arbor-grc-*.jar /app/app.jar

USER arbor

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
