FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/ ./src/
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine AS run
WORKDIR /app
# Timezone: LocalDate.now() trong StreakService phải chạy theo giờ VN (UTC+7),
# không phải UTC của container — nếu không streak sẽ lệch ngày cho user học 0h-7h sáng.
RUN apk add --no-cache tzdata
ENV TZ=Asia/Ho_Chi_Minh
COPY --from=build /app/target/engflow-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "/app/app.jar"]
