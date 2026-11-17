package com.tlc.observability.context;

import com.tlc.observability.internal.context.InternalMetricsContext;

public class MetricsContext {

    private static final MetricsContext INSTANCE = new MetricsContext();

    /**
     * Adds a key-value pair to the ThreadLocal attributes.
     *
     * @param key   The key to add.
     * @param value The value to associate with the key.
     * @return the MetricsContext instance for method chaining
     */
    public static MetricsContext add(final String key, final Object value) {
        InternalMetricsContext.add(key, value);
        return INSTANCE;
    }

}