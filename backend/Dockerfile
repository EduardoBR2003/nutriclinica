# =============================================================
# NutriClinica API — build multi-stage
# Alvo: Render free tier (512 MB RAM, 0.1 CPU)
# =============================================================

# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependências: só reprocessa se o pom mudar
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Estágio 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário sem privilégios
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app

# Tuning para 512 MB. Sem isso a JVM assume que pode usar o host inteiro
# e o container é morto por OOM antes de terminar o boot.
#   MaxRAMPercentage=70  — deixa ~150 MB para metaspace, threads e stack
#   UseSerialGC          — G1 gasta memória e threads demais para 0.1 CPU
#   TieredStopAtLevel=1  — só C1: boot bem mais rápido, importa no cold start
#   Xss512k              — 1 MB por thread é desperdício aqui
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 \
    -XX:+UseSerialGC \
    -XX:TieredStopAtLevel=1 \
    -Xss512k \
    -XX:+ExitOnOutOfMemoryError \
    -Dspring.jmx.enabled=false \
    -Djava.security.egd=file:/dev/./urandom"

# O Render injeta PORT em tempo de execução; 8080 é só o padrão local.
ENV PORT=8080
EXPOSE 8080

# sh -c é necessário para expandir as variáveis
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
