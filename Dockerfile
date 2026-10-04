# syntax=docker/dockerfile:1

############################
# Stage 1 - build the jar
############################
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM first and resolve dependencies so Docker layer caching
# skips re-downloading the internet on every source change.
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src ./src
# -Prelease: excludes application.properties from the jar; runtime config
# must be supplied via the /app/config volume.
RUN ./mvnw -B clean package -Prelease -DskipTests

############################
# Stage 2 - runtime
############################
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user; /app/config is a read-only mount point.
RUN useradd --system --uid 1001 sumapp \
    && mkdir -p /app/config \
    && chown -R sumapp:sumapp /app
USER sumapp

COPY --from=build /build/target/simpleusermanagement.jar /app/app.jar

# Spring Boot reads ./config/application.json / application.properties
# from the working directory, i.e. /app/config inside the container.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
