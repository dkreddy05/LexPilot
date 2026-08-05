# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Install Maven wrapper or use system Maven
# For production builds, prefer a multi-stage with Maven image
RUN apk add --no-cache maven && mvn -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S lexpilot && adduser -S lexpilot -G lexpilot

COPY --from=builder /app/target/*.jar app.jar

USER lexpilot

EXPOSE 8080

# Java 21: enable virtual threads via system property (also set in application.yml)
ENTRYPOINT ["java", \
  "-Dspring.threads.virtual.enabled=true", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
