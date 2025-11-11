# Stage 1: Build the application using Gradle
FROM azul/zulu-openjdk:24 AS builder
WORKDIR /app
COPY gradlew .
COPY gradlew.bat .
COPY gradle /app/gradle
RUN chmod +x gradlew
COPY build.gradle settings.gradle /app/
# Copy dependencies first to leverage Docker cache
# RUN gradle dependencies
COPY src /app/src
RUN ./gradlew build -x test

# Stage 2: Create the final, lightweight image
FROM azul/zulu-openjdk-alpine:24-jre-latest
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
