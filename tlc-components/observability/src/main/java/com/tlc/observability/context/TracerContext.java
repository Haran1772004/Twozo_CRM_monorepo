package com.tlc.observability.context;


import com.tlc.observability.internal.context.InternalTracerContext;

/**
 * TracerContext is a singleton class responsible for managing the context of tracing operations.
 * It provides a global instance to access and manage key-value pairs in the tracing context.
 * The context is added to an internal storage (InternalTracerContext), which is used during trace propagation.
 * This class ensures that the tracer context can be easily modified and accessed throughout the application.
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public class TracerContext {
    private static final TracerContext INSTANCE = new TracerContext();

    /**
     * Returns the singleton instance of TracerContext.
     *
     * @return the TracerContext instance
     */
    public static TracerContext get() {
        return INSTANCE;
    }

    /**
     * Adds a key-value pair to the internal tracing context.
     *
     * @param key   the key for the context
     * @param value the value associated with the key
     * @return the TracerContext instance for method chaining
     */
    public TracerContext add(final String key, final Object value) {
        InternalTracerContext.add(key, value);

        return INSTANCE;
    }

}
