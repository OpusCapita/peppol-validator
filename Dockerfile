## using multistage docker build for speed
## temp container to build
FROM openjdk:8 AS TEMP_BUILD_IMAGE

ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME

COPY build.gradle settings.gradle gradlew $APP_HOME
COPY gradle $APP_HOME/gradle
COPY . $APP_HOME

RUN chmod +x ./gradlew
RUN ./gradlew build || return 0

## actual container
FROM openjdk:8
LABEL author="Ibrahim Bilge <Ibrahim.Bilge@opuscapita.com>"

## setting heap size automatically to the container memory limits
ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -XshowSettings:vm"

ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME

# copy and extract validation artifacts
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/src/main/resources/rules.zip .
RUN unzip -q rules.zip

# copy the actual app as jar file
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/peppol-validator.jar .

HEALTHCHECK --interval=15s --timeout=3s --retries=15 \
  CMD curl --silent --fail http://localhost:3039/api/health/check || exit 1

EXPOSE 3039
ENTRYPOINT exec java $JAVA_OPTS -jar peppol-validator.jar