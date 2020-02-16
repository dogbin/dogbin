FROM gradle:jdk8 AS build-env
COPY --chown=gradle:gradle . .

# People apparently use windows hosts
RUN apt-get update && apt-get install -y dos2unix
RUN ["dos2unix", "gradlew"]

RUN ./gradlew --no-daemon :app:shadowJar
RUN cp app/build/libs/*all.jar dogbin.jar

FROM openjdk:8-jre-alpine

COPY --from=build-env /home/gradle/dogbin.jar dogbin.jar
COPY documents/ /documents
# "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2"
CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "dogbin.jar"]
