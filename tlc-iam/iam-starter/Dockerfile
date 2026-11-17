FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get -y update; apt-get -y install curl

COPY target/iam-starter-*.jar iam-server.jar
COPY handle-oom.sh /app/
RUN chmod +x /app/handle-oom.sh

EXPOSE 8080
EXPOSE 5006

ENTRYPOINT ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006", "-XX:HeapDumpPath=/tmp/heapdumps/heapdump.hprof", "-XX:OnOutOfMemoryError=/app/handle-oom.sh", "-jar", "iam-server.jar"]