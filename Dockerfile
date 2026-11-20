FROM eclipse-temurin:27-jre@sha256:acfb350b50e707addf3dcbb47f7052d1882d1e5f1670ce838a9c7838d82d4fe5 AS runtime
WORKDIR /app
USER 10001

FROM runtime AS spring-service
ARG SERVICE
COPY services/${SERVICE}/build/libs/${SERVICE}.jar /app/service.jar
ENTRYPOINT ["java", "-jar", "/app/service.jar"]

FROM runtime AS biller-service
COPY services/biller-service/build/quarkus-app/ /app/
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
