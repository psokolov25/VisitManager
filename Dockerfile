#Перед этим нужно поменять версию в pom.xml на 17
#FROM amazoncorretto:17.0.4-alpine3.15 AS builder
#ADD [".\\", "/src"]
#WORKDIR /src
#COPY pom.xml .
#COPY src ./src
FROM maven:3.9.9-eclipse-temurin-17 AS builder
COPY . /app
WORKDIR /app


#ENV REDIS_SERVER="redis://redis:6379"
#ENV REDIS_SERVER_TEST="redis://localhost:6379"
#ENV DATABUS_SERVER="http://192.168.8.45:8082"
#ENV CONFIG_SERVER="http://localhost:8081"
#ENV PRINTER_SERVER="http://192.168.3.33:8084"
#ENV OIDC_ISSUER_DOMAIN="http://192.168.8.45:9090/realms/Aritmos"
#ENV OIDC_ISSUER_URL="http://192.168.8.45:9090"
#ENV OAUTH_CLIENT_ID="myclient"
#ENV KEYCLOAK_TECHLOGIN="visitmanager"
#ENV KEYCLOAK_TECHPASSWORD="visitmanager"
#RUN sed -i 's/\r$//' mvnw
#RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -Dmaven.test.skip=true
RUN mvn clean package -Dmaven.test.skip=true



#FROM amazoncorretto:17.0.4-alpine3.15 AS packager
#RUN apk add --no-cache binutils
#ENV JAVA_MINIMAL="/opt/java-minimal"
#RUN $JAVA_HOME/bin/jlink \
#                  --verbose \
#                  --add-modules \
#                      java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,jdk.zipfs,jdk.unsupported \
#                  --compress 2 --strip-debug --no-header-files --no-man-pages \
#                  --output "$JAVA_MINIMAL"

#FROM alpine:3.16.2
#ENV JAVA_HOME=/opt/java-minimal
#ENV PATH="$PATH:$JAVA_HOME/bin"
#ENV REDIS_SERVER="redis://redis:6379"
#ENV REDIS_SERVER_TEST="redis://localhost:6379"
#ENV DATABUS_SERVER="http://192.168.8.45:8082"
#ENV CONFIG_SERVER="http://localhost:8081"
#ENV PRINTER_SERVER="http://192.168.3.33:8084"
#ENV OIDC_ISSUER_DOMAIN="http://192.168.8.45:9090/realms/Aritmos"
#ENV OIDC_ISSUER_URL="http://192.168.8.45:9090"
#ENV OAUTH_CLIENT_ID="myclient"
#ENV KEYCLOAK_TECHLOGIN="visitmanager"
#ENV KEYCLOAK_TECHPASSWORD="visitmanager"
#ENV KAFKA_SERVER="192.168.8.45:9094"
#ENV LOKI_SERVER="http://192.168.3.13:3100/loki/api/v1/push"
#COPY --from=packager "$JAVA_HOME" "$JAVA_HOME"
FROM bellsoft/liberica-openjdk-alpine:17
COPY --from=builder "/app/target/visitmanager.jar" "app.jar"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]