# ---------- Stage 1: build the JAR with Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/siteguard.jar app.jar
EXPOSE 8080
# Limited memory because the Render free plan has only 512 MB RAM
CMD ["java", "-XX:+UseSerialGC", "-Xmx320m", "-jar", "app.jar"]
