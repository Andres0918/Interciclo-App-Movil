# Usar una imagen base de OpenJDK (como se usará Spring Boot)
FROM openjdk:17-jdk-slim
LABEL authors="EduardoArce"
# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR generado por el build (asegúrate de que tu jar esté en la carpeta target de tu servicio Eureka)
COPY target/eurekaserver-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto que usa Eureka
EXPOSE 8761

# Comando para ejecutar el servicio Eureka Server
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
