# Stage 1: build the dashboard
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: build the jar, with the built dashboard bundled in as static files
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# Stage 3: run it as a non-root user on a slim JRE. One container serves the API and the dashboard.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 sentinel
WORKDIR /app
COPY --from=build /build/target/sentinel-*.jar app.jar
USER sentinel
EXPOSE 8080
# SerialGC and a small thread stack keep memory low on small hosts. Override with JAVA_TOOL_OPTIONS if needed.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-Xss512k", "-jar", "/app/app.jar"]
