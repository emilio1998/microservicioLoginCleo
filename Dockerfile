# Etapa 1: Build con Maven
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copiamos los archivos del proyecto
COPY . .

# Compilamos el proyecto (salta los tests)
RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagen final
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copiamos el jar generado desde la etapa builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]