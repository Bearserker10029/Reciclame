# syntax=docker/dockerfile:1

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S reciclame && adduser -S reciclame -G reciclame

COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/src/main/resources/db/*.sql ./db/

RUN apk add --no-cache mariadb-client fontconfig ttf-dejavu

RUN chown -R reciclame:reciclame /app

USER reciclame

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.awt.headless=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]