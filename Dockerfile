FROM eclipse-temurin:17-jre
WORKDIR /home/app
COPY target/classes /home/app/classes
COPY target/dependency/* /home/app/libs/
ENV REDIS_URL=redis://192.168.8.45:6379
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/home/app/libs/*:/home/app/classes/", "ru.aritmos.Application"]
