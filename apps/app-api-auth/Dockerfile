# Build
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :apps:app-api-auth:bootJar -x test

# Run
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/apps/app-api-auth/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
