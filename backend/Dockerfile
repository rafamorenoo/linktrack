# ── Etapa 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiamos primero el pom y descargamos dependencias (capa cacheada)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -q

# Ahora copiamos el código y compilamos
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Etapa 2: Extrae layers del jar para imagen final más eficiente ───────────
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Etapa 3: Imagen final mínima ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S linktrack && adduser -S linktrack -G linktrack
USER linktrack

# Copiamos las layers en orden (de menos a más cambiante)
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3   CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-Djava.security.egd=file:/dev/./urandom",   "org.springframework.boot.loader.launch.JarLauncher"]
