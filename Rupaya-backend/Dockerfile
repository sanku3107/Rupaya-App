# ===============================
# Stage 1: Build Stage
# ===============================
FROM maven:4.0.0-rc-5-sapmachine-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first (dependency caching)
COPY pom.xml .

# Download dependencies
RUN mvn -q -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests


# ===============================
# Stage 2: Runtime Stage
# ===============================
FROM sapmachine:21-jre

# Set working directory
WORKDIR /app

# Create non-root user (recommended)
RUN useradd -m spring
USER spring

# Copy jar from build stage (rename to app.jar)
COPY --from=build /app/target/Rupaya-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose application port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]