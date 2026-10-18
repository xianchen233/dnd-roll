FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY backend/src src
RUN ./mvnw -B -ntp package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home dndroll
COPY --from=build --chown=dndroll:dndroll /workspace/target/dnd-roll-backend-0.1.0-SNAPSHOT.jar app.jar
USER dndroll
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
