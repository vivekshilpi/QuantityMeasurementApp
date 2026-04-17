FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY quantity-service/pom.xml quantity-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY service-registry/pom.xml service-registry/pom.xml
COPY quantity-common/pom.xml quantity-common/pom.xml
COPY api-gateway/src api-gateway/src

RUN mvn -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
