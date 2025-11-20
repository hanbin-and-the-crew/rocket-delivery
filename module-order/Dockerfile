FROM eclipse-temurin:17-jre-alpine

ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 19505

ENTRYPOINT ["java", "-Dserver.port=19505", "-jar", "/app.jar"]
