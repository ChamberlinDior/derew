# ── Stage 1 : Build ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2 : Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="tech@oviro.com"
LABEL version="1.0.0"

RUN addgroup -S oviro && adduser -S oviro -G oviro
WORKDIR /app

COPY --from=builder /build/target/oviro-backend-1.0.0.jar app.jar

RUN chown oviro:oviro app.jar
USER oviro

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
