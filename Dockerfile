# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy only the pom first so Docker can cache the dependency download layer -
# it only re-runs when pom.xml actually changes, not on every source code edit.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as a non-root user rather than the container default root
RUN useradd --create-home appuser

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]