FROM gradle:8-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Optional OpenTelemetry Java agent — enabled when OTEL_EXPORTER_OTLP_ENDPOINT is set.
# The image always ships the agent; the entrypoint conditionally attaches it so
# operators can flip OTel on/off without rebuilding.
ARG OTEL_AGENT_VERSION=2.4.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
RUN chmod 0644 /opentelemetry-javaagent.jar

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "if [ -n \"$OTEL_EXPORTER_OTLP_ENDPOINT\" ]; then exec java -javaagent:/opentelemetry-javaagent.jar -Dotel.service.name=${OTEL_SERVICE_NAME:-homekm-api} -jar app.jar; else exec java -jar app.jar; fi"]
