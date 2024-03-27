FROM openjdk:latest as build
WORKDIR /app
COPY target/*.jar /app/app.jar
CMD ["java", "-jar", "app.jar"]