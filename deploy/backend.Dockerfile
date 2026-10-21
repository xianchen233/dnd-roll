FROM eclipse-temurin:25-jdk AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY backend/src src
RUN ./mvnw -B -ntp package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && install -d /usr/share/postgresql-common/pgdg \
    && curl --fail --silent --show-error https://www.postgresql.org/media/keys/ACCC4CF8.asc -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
    && . /etc/os-release \
    && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt ${VERSION_CODENAME}-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client-18 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home dndroll \
    && mkdir -p /var/lib/dnd-roll/assets /var/lib/dnd-roll/backups \
    && chown -R dndroll:dndroll /var/lib/dnd-roll
COPY --from=build --chown=dndroll:dndroll /workspace/target/dnd-roll-backend-0.1.0-SNAPSHOT.jar app.jar
USER dndroll
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
