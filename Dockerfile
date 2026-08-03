# Этап 1: Сборка (Maven + JDK)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Копируем pom.xml и скачиваем зависимости (кеширование)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код и собираем .jar
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск (только JRE - легкая версия Java)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Копируем файл ключа Google (если он нужен внутри контейнера)
# ВАЖНО: на проде лучше передавать его содержимое через переменные окружения
COPY src/main/resources/google-credentials.json /app/google-credentials.json

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]