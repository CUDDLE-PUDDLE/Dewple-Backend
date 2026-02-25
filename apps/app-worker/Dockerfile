# Build
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :apps:app-worker:bootJar -x test

# Run
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/apps/app-worker/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
