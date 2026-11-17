FROM mcr.microsoft.com/playwright/java:v1.52.0-noble

WORKDIR /app

RUN apt-get -y update; apt-get -y install curl

COPY target/server-starter-*.jar crm-server.jar
COPY handle-oom.sh /app/
RUN chmod +x /app/handle-oom.sh

EXPOSE 8080
EXPOSE 5005

ENTRYPOINT ["java", "-Dtlc.dev.mode=true", "-Dtlc.security.enabled=true", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/tmp/heapdumps/heapdump.hprof", "-XX:OnOutOfMemoryError=/app/handle-oom.sh", "-jar", "crm-server.jar"]