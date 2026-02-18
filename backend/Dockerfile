FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# 1. Copier uniquement les fichiers de configuration Gradle pour mettre en cache les dépendances
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./

# 2. Donner les droits et télécharger les dépendances 
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 3. Copier le code source 
COPY src src

# Port exposé par Spring Boot
EXPOSE 8080

ENV GRADLE_USER_HOME=/app/.gradle

# L'option --no-daemon est importante pour Docker
CMD ["./gradlew", "bootRun"]