# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Pre-download dependencies to leverage Docker cache
RUN maven_opts="-XX:+TieredCompilation -XX:TieredStopAtLevel=1" mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security in production
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built artifact
COPY --from=builder /app/target/carbon-platform-1.0.0-SNAPSHOT.jar app.jar

# Expose default port
EXPOSE 8080

# Environment variables with defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Healthcheck using wget (pre-installed in alpine JRE)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT}/api/v1/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
