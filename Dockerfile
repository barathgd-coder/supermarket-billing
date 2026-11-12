# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Cache dependencies first (re-used until pom.xml changes)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -Dmaven.test.skip=true

# ---------- Stage 2: run ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --create-home appuser \
 && mkdir -p /app/data /app/uploads \
 && chown -R appuser:appuser /app
COPY --from=build --chown=appuser:appuser /build/target/supermarket-billing-1.0.0.jar app.jar
USER appuser

ENV APP_PROFILE=h2 \
    THYMELEAF_CACHE=true \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

EXPOSE 8080
# Render/Railway set $PORT; Spring reads it via server.port=${PORT:8080}
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
