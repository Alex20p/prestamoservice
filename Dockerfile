# ==========================================
# Etapa 1: Build y empaquetado con JDK 21
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copiar archivos del Gradle Wrapper y configuración de construcción
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Convertir saltos de línea Windows (CRLF) a Linux (LF) y dar permisos de ejecución
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Descargar dependencias para aprovechar la caché de capas de Docker
RUN ./gradlew dependencies --no-daemon || true

# Copiar el código fuente de la aplicación
COPY src src

# Compilar y empaquetar el JAR ejecutable de producción
RUN ./gradlew bootJar --no-daemon -x test

# ==========================================
# Etapa 2: Imagen final de ejecución con JRE 21
# ==========================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Crear usuario y grupo no-root para seguridad
RUN addgroup --system prestagroup && adduser --system --ingroup prestagroup prestauser
USER prestauser:prestagroup

# Copiar únicamente el archivo bootJar ejecutable ignorando cualquier plain.jar
COPY --from=build --chown=prestauser:prestagroup /app/build/libs/*SNAPSHOT.jar app.jar

# Configurar puerto predeterminado (Render inyecta PORT dinámicamente)
ENV PORT=8080
EXPOSE 8080

# Iniciar la aplicación
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]