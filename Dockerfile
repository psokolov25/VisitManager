#Перед этим нужно поменять версию в pom.xml на 17
FROM amazoncorretto:17.0.4-alpine3.15 as builder
ADD [".\\", "/src"]
WORKDIR /src
RUN sed -i 's/\r$//' mvnw
RUN ./mvnw install -DskipTests

FROM amazoncorretto:17.0.4-alpine3.15 as packager
RUN apk add --no-cache binutils
ENV JAVA_MINIMAL="/opt/java-minimal"
RUN $JAVA_HOME/bin/jlink \
                  --verbose \
                  --add-modules \
                      java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,jdk.zipfs,jdk.unsupported \
                  --compress 2 --strip-debug --no-header-files --no-man-pages \
                  --output "$JAVA_MINIMAL"

FROM alpine:3.16.2
ENV JAVA_HOME=/opt/java-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"
COPY --from=packager "$JAVA_HOME" "$JAVA_HOME"
COPY --from=builder "/src/target/visitmanager.jar" "app.jar"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]