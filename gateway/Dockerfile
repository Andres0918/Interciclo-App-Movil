# Usar una imagen base de OpenJDK para correr Spring Boot
FROM openjdk:17-jdk-slim
LABEL authors="eduardo"

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR generado por el build (asegúrate de que el JAR de tu API Gateway esté en target/)
COPY target/gateway-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto 8080 (el que usa el API Gateway)
EXPOSE 8080

# Comando para ejecutar el servicio de API Gateway
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
