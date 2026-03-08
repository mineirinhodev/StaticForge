# ========================================
# Stage 1: Build
# ========================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and build files first (cache dependency downloads)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix Windows CRLF line endings and make executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw package -DskipTests -B

# ========================================
# Stage 2: Runtime
# ========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
