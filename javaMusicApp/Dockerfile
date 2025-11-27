# Dockerfile
# Use a JDK 21 base for the build stage to satisfy Gradle toolchain requirements
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY . .
# Ensure the Gradle wrapper is executable and use it to build (avoids missing Gradle or toolchain download issues)
RUN chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8088/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]