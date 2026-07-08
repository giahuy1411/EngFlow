FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ ./src/
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine AS run
WORKDIR /app
COPY --from=build /app/target/engflow-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
