# backend-java/Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copier les fichiers Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline

# Copier le code source
COPY src src

# Builder l'application
RUN ./mvnw package -DskipTests

# Image finale
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copier le JAR
COPY --from=builder /app/target/*.jar app.jar

# Créer les dossiers de stockage
RUN mkdir -p /app/storage/images /app/storage/videos

# Exposer le port
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]