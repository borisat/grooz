FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
COPY weather/target/weather-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]