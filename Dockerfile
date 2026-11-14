# ---------- stage 1: build ----------
FROM eclipse-temurin:27-jdk AS build
WORKDIR /workspace

# Copy only what dependency resolution needs first, so this slow layer is cached
# until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI, not while building the image
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar app.jar

# ---------- stage 2: runtime ----------
FROM eclipse-temurin:27-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8080

# Size the heap from the container's memory limit; die cleanly on OOM so the orchestrator restarts us
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]