FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY identity ./identity
COPY planner ./planner
COPY planner-app ./planner-app
COPY frontend/build.gradle.kts frontend/package.json ./frontend/

RUN chmod +x gradlew \
  && ./gradlew :planner-app:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN useradd --system --uid 10001 watchnest
COPY --from=build /workspace/planner-app/build/libs /tmp/libs
RUN set -eux; \
    jar="$(find /tmp/libs -maxdepth 1 -type f -name 'planner-app-*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /app/app.jar; \
    chown -R watchnest:watchnest /app; \
    rm -rf /tmp/libs

USER watchnest
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
