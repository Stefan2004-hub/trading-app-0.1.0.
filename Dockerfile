FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/trading-app-0.1.0.jar app.jar

CMD ["sh", "-c", "java -Dserver.port=${PORT:-10000} -jar app.jar"]
