FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
COPY source/target/source-1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]