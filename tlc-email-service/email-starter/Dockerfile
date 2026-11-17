FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get -y update; apt-get -y install curl

COPY target/email-starter-*.jar email-service.jar
COPY handle-oom.sh /app/
RUN chmod +x /app/handle-oom.sh

EXPOSE 8080
EXPOSE 5008

ENTRYPOINT ["java", "-Dtlc.dev.mode=true", "-Dtlc.security.enabled=true", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5008", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/tmp/heapdumps/heapdump.hprof", "-XX:OnOutOfMemoryError=/app/handle-oom.sh", "-jar", "email-service.jar"]