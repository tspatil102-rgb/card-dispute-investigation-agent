# Multi-stage Dockerfile for building and running the Spring Boot app on Google Cloud Run

# Build stage (Maven + JDK 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven wrapper and pom first for caching
COPY pom.xml ./
RUN mvn -B -f pom.xml -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -f pom.xml -DskipTests package

# Runtime stage (Eclipse Temurin JRE 21)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Default JAR name from pom; update if artifactId/version differ
ARG JAR_NAME=spring-boot-sample-0.0.1-SNAPSHOT.jar
COPY --from=build /workspace/target/${JAR_NAME} /app/app.jar

ENV JAVA_OPTS="-Xms128m -Xmx512m"
EXPOSE 8080

# Optional healthcheck — adjust path if actuator is disabled
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
