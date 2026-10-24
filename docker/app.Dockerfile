# Uma imagem para a API ou para o worker. Escolha com --build-arg MODULE=api|worker.
FROM eclipse-temurin:24-jdk-alpine AS build
ARG MODULE=api
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/
COPY api/pom.xml api/
COPY worker/pom.xml worker/
COPY architecture-tests/pom.xml architecture-tests/
RUN ./mvnw -B -q -pl ${MODULE} -am dependency:resolve dependency:resolve-plugins || true

COPY . .
RUN ./mvnw -B -q -pl ${MODULE} -am package -DskipTests -Dspotless.check.skip=true \
    && cp ${MODULE}/target/devready-${MODULE}-*-exec.jar /workspace/app.jar

FROM eclipse-temurin:24-jre-alpine
RUN addgroup -S devready && adduser -S devready -G devready
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER devready
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
