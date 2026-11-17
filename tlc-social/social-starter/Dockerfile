FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY target/social-starter-*.jar social-server.jar
COPY handle-oom.sh /app/
RUN chmod +x /app/handle-oom.sh

EXPOSE 8080
EXPOSE 5010

ENTRYPOINT ["java", "-Dtlc.security.enabled=true", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5010", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/tmp/heapdumps/heapdump.hprof", "-XX:OnOutOfMemoryError=/app/handle-oom.sh","-jar", "social-server.jar"]