# Build WAR from multi-module root (parent pom + my-blog-back-app).
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /work
COPY pom.xml .
COPY my-blog-back-app ./my-blog-back-app
RUN mvn -B -pl my-blog-back-app -am package -DskipTests

# Tomcat 10 + Java 21 (Jakarta EE 10 / Servlet 6).
FROM tomcat:10.1-jdk21-temurin-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /work/my-blog-back-app/target/my-blog-back-app.war /usr/local/tomcat/webapps/ROOT.war
ENV CATALINA_OPTS="-Dspring.profiles.active=prod"
EXPOSE 8080
