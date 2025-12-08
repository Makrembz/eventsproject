# Multi-stage build for Spring Boot application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY target/*.jar app.jar

# Final runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the jar from build stage
COPY --from=build /app/app.jar app.jar

# Expose the Spring Boot port (matches server.port in application.properties)
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:8089/events/actuator/health || exit 1
# HEALTHCHECK CMD wget -q --spider http://localhost:8089/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
