# API Spring Boot — image en deux étapes : on compile avec le JDK, on exécute
# avec le seul JRE (image plus petite, aucun outil de build en production).

# --- 1. Build ------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dépendances d'abord : cette couche reste en cache tant que pom.xml ne change pas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Les tests tournent avant la PR (./mvnw test) : l'image ne fait que packager
COPY src/ src/
RUN ./mvnw -B -q package -Dmaven.test.skip=true

# --- 2. Exécution --------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Jamais root dans le conteneur
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app

# La JVM respecte la mémoire allouée au conteneur
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
