package com.tlc.observability.internal.context;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InternalTracerContext is a utility class for managing tracing-related context in OpenTelemetry.
 * It provides functionality to create, update, and retrieve contextual information related to the current span.
 *
 * <p>
 * The context is used to store key-value pairs, such as method names, class names, or custom attributes,
 * that are associated with the current span in a concurrent and thread-safe manner.
 * </p>
 *
 * <b>Example usage:</b>
 * <pre>
 * {@code
 * InternalTracerContext.add("method.name", "myMethod");
 * }
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public class InternalTracerContext {

    private static final InternalTracerContext INSTANCE = new InternalTracerContext();

    private static final ContextKey<Map<String, Object>> CONTEXT_KEY = ContextKey.named("tracerContext");


    /**
     * Creates a new context for the current span, adding a key-value pair representing the span's name.
     *
     * @param key the key to associate with the span name.
     * @param spanName the name of the span.
     * @return a new {@link Context} with the span's information.
     */
    public static Context createNewContextForSpan(final String key, final String spanName) {
        final Map<String, Object> contextMap = new ConcurrentHashMap<>();

        contextMap.put(key, spanName);
        return Context.current().with(CONTEXT_KEY, contextMap);
    }

    /**
     * Returns the singleton instance of InternalTracerContext.
     *
     * @return the singleton instance.
     */
    public static InternalTracerContext get() {
        return INSTANCE;
    }

    /**
     * Adds a key-value pair to the current tracing context, if available.
     *
     * @param key the key to add to the context.
     * @param value the value to associate with the key.
     * @return the singleton instance for method chaining.
     */
    public static InternalTracerContext add(final String key, final Object value) {
        getAttributes().ifPresent(contextMap -> {
            if (Objects.nonNull(key) && Objects.nonNull(value)) {
                contextMap.put(key, value);
            }
        });
        return INSTANCE;
    }

    /**
     * Retrieves the current context map for the span, if available.
     *
     * @return an {@link Optional} containing the context map, or an empty {@link Optional} if no context exists.
     */
    public static Optional<Map<String, Object>> getAttributes() {
        return Optional.ofNullable(Context.current().get(CONTEXT_KEY));
    }

    /**
     * Clears all key-value pairs from the current tracing context, if available.
     */
    public static void clearAttributes() {
        getAttributes().ifPresent(contextMap -> contextMap.clear());
    }
}
