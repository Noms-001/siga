# ============================
# Étape 1 : Build
# ============================
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Maven Wrapper
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline

# Copier le code source
COPY src ./src

# Construire l'application
RUN ./mvnw clean package -DskipTests


# ============================
# Étape 2 : Runtime
# ============================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]