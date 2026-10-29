# Stage 1: build the jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Stage 2: run it as a non-root user on a slim JRE
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 sentinel
WORKDIR /app
COPY --from=build /build/target/sentinel-*.jar app.jar
USER sentinel
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
