FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/ui-client-*.jar ui-client-server.jar
COPY build /app/build

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "-Dserver.port=3000", "-Dtwozo.csp.enabled=true",  "-Dtwozo.csp.nonce-enabled=true", "ui-client-server.jar"]