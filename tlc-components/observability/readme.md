# Observability Plugin

## Overview

The **Observability Plugin** provides functionality to monitor and trace application behavior using OpenTelemetry. It offers integration for collecting both tracing and metrics data for your application, allowing you to track performance, errors, and overall health.

This plugin supports exporting metrics and traces to various backends such as **OTLP (OpenTelemetry Protocol)** and **Logging**, giving you the flexibility to choose how you want to store and visualize observability data.

## Features

- **Metrics Collection:** Gather application metrics, including performance data such as execution times and error rates.
- **Distributed Tracing:** Trace method executions across services, capturing detailed information about request flows and errors.
- **Exporters:** Support for multiple exporters like OTLP and logging for both traces and metrics.
- **Context Propagation:** Context is automatically passed across method calls for consistent trace management.

## Prerequisites

- Java 17 or higher
- OpenTelemetry SDK dependencies
- OpenTelemetry exporter setup (e.g., OTLP or logging exporter)
- A backend service (such as Jaeger, Prometheus, or any other OpenTelemetry-compatible backend) for metrics and traces visualization.

## Installation

1. **Add the plugin as a dependency** in your project.

   If you're using Maven, add the following dependency to your `pom.xml`:

   ```xml
   <dependency>
       <groupId>com.tlc</groupId>
       <artifactId>observability-plugin</artifactId>
       <version>1.0.0</version>
   </dependency>

## Configure the Observability Service

In your application, initialize the observability service with the configuration:

```java
import com.tlc.observability.config.ObservabilityConfig;
import com.tlc.observability.ObservabilityConfigurator;

public class AppStarter {

    public void start() {
        final ObservabilityConfig observabilityConfig = new ObservabilityConfig();

        // Set configuration properties
        observabilityConfig.setServiceName("my-app");
        observabilityConfig.setExporterEndpoint("http://localhost:4317");

        // Initialize the observability service
        ObservabilityConfigurator.configure(observabilityConfig);
    }
}

```

The plugin supports the following metrics:

- **TrackInvocationCount**: Tracks the count of method invocations.
- **MeasureTime**: Measures the execution time of methods.

To trace method invocations use the below annotation
- **TraceMethodExecution**:
  Use **TraceContext** to add details to the span of your trace


## Example

```java
import com.tlc.observability.annotations.*;
import com.tlc.observability.context.TracerContext;


public class MyService {

    @MeasureTime
    @TrackInvocationCount
    @TraceMethodExecution(name = "user-signup-tracker")
    public void myMethod() {
        TracerContext.get().add("user.id", 1);
        // Your method implementation here
    }
}
```

## How to enable dynamic observability?

- Download the below dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.aspectj</groupId>
        <artifactId>aspectjweaver</artifactId>
        <version>1.9.22.1</version> <!-- Choose the version according to your use case -->
    </dependency>

    <dependency>
        <groupId>io.opentelemetry.javaagent</groupId>
        <artifactId>opentelemetry-javaagent</artifactId>
        <version>2.10.0</version>
    </dependency>
</dependencies>
```

While running your spring boot application use the below command

```sh
java -javaagent:path-to-opentelemetry-javaagent -javaagent:path-to-aspectj-weaver-jar -Dotel.service.name=iam-service -Dotel.exporter.otlp.endpoint=http://localhost:4318 -jar path-to-app-jar
```
- Replace path-to-opentelemetry-javaagent with the actual path to the OpenTelemetry Java agent .jar file.
- Replace path-to-aspectj-weaver-jar with the path to the AspectJ Weaver .jar file.
- Replace iam-service with the name of your service.
- Replace -Dotel.exporter.otlp.endpoint=http://localhost:4318 with your remote config URL. Basically it should be HTTP endpoint for OTLP. 
If it is not specified, OTEL will try to connect to localhost:4318
- Replace path-to-app-jar with the path to your Spring Boot application .jar file.