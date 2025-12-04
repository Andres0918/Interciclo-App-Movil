# Usar una imagen base de OpenJDK para ejecutar Spring Boot
FROM openjdk:17-jdk-slim
LABEL authors="eduardo"

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR generado en el directorio target del proyecto
COPY target/auth-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto 8081 (el puerto del Auth Service)
EXPOSE 8081

# Comando para ejecutar el servicio Auth
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
