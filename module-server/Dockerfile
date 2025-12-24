FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-Dserver.port=9090", "-jar", "/app.jar"]