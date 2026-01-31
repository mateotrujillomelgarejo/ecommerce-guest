FROM openjdk:21-jdk-slim

# Crear usuario no-root
RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

# Configurar directorio de trabajo
WORKDIR /app

# Copiar archivos Maven
COPY --chown=spring:spring mvnw .
COPY --chown=spring:spring .mvn .mvn
COPY --chown=spring:spring pom.xml .

# Descargar dependencias (para caching)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY --chown=spring:spring src src

# Construir el JAR
RUN ./mvnw clean package -DskipTests

# Puerto de exposición
EXPOSE 8089

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "target/notification-service-0.0.1-SNAPSHOT.jar"]