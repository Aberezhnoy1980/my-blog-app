# Build executable Boot JAR from multi-module root.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /work
COPY gradlew .
COPY gradlew.bat .
COPY gradle ./gradle
COPY settings.gradle .
COPY build.gradle .
COPY my-blog-back-app ./my-blog-back-app
RUN ./gradlew --no-daemon :my-blog-back-app:bootJar -x test

# Runtime image: Java 21 + app.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /work/my-blog-back-app/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
