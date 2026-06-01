# Этап 1: Сборка (Используем образ с Maven и Java 21)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Указываем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем файл pom.xml и скачиваем зависимости (кеширование)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем весь исходный код и собираем проект
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск (Используем легкий образ только с JRE 21)
FROM eclipse-temurin:21-jre-alpine

# Указываем рабочую директорию
WORKDIR /app
VOLUME /tmp

# Копируем собранный .jar файл из первого этапа (из контейнера builder)
COPY --from=builder /app/target/*.jar app.jar

# Запускаем приложение
ENTRYPOINT ["java","-jar","app.jar"]