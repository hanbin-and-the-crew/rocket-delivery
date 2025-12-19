FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 19508

ENTRYPOINT ["java", "-Dserver.port=19508", "-jar", "/app.jar"]