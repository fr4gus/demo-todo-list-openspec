# syntax=docker/dockerfile:1

# dev: source is bind-mounted by docker-compose.override.yml; Maven recompiles on restart.
FROM maven:3.9-eclipse-temurin-21 AS dev
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
EXPOSE 8080
CMD ["mvn", "-B", "spring-boot:run"]

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/todo-backend-*.jar app.jar

# prod: JRE only, non-root, no build tooling.
FROM eclipse-temurin:21-jre-jammy AS prod
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
