# Dockerfile parametrizado para os microsserviços PetFriends.
# O serviço a empacotar é escolhido via build-arg MODULE.

# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# pom pai + módulos (copiamos tudo para o -am resolver dependências internas)
COPY pom.xml .
COPY petfriends-shared-events petfriends-shared-events
COPY petfriends-almoxarifado  petfriends-almoxarifado
COPY petfriends-transporte    petfriends-transporte
COPY petfriends-pedidos       petfriends-pedidos
ARG MODULE
RUN mvn -q -pl ${MODULE} -am -DskipTests package

# ---- runtime ----
FROM eclipse-temurin:17-jre AS run
WORKDIR /app
ARG MODULE
COPY --from=build /app/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
