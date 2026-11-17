package com.tlc.observability.internal.context;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages the context of internal metrics, which is stored on a per-thread basis using ThreadLocal.
 * <p>
 * The internal metrics context is used to store key-value pairs associated with the execution context
 * of the current thread. This class provides methods to initialize, add, remove, retrieve, and clear the
 * attributes stored in the context.
 * </p>
 *
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 */
public class InternalMetricsContext {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = ThreadLocal.withInitial(HashMap::new);

    /**
     * Initializes the ThreadLocal context if it has not been set already.
     * <p>
     * If the ThreadLocal map is null, it is replaced with a new instance of ConcurrentHashMap.
     * </p>
     */
    public static void init() {
        if (THREAD_LOCAL.get() == null) {
            THREAD_LOCAL.set(new ConcurrentHashMap<>());
        }
    }

    /**
     * Retrieves the attributes stored in the ThreadLocal context.
     * <p>
     * This method returns an Optional containing the map of attributes, or an empty Optional if no
     * attributes are currently stored.
     * </p>
     *
     * @return Optional containing the map of attributes, or empty if no attributes are set.
     */
    public static Optional<Map<String, Object>> getAttributes() {
        return Optional.ofNullable(THREAD_LOCAL.get());
    }

    /**
     * Adds a key-value pair to the ThreadLocal context attributes.
     * <p>
     * This method ensures that both the key and value are non-null before attempting to add them to
     * the context.
     * </p>
     *
     * @param key   The key to add.
     * @param value The value to associate with the key.
     */
    public static void add(final String key, final Object value) {
        if (Objects.nonNull(key) && Objects.nonNull(value)) {
            getAttributes().ifPresent(context -> context.put(key, value));
        }
    }

    /**
     * Removes a specific attribute from the ThreadLocal context.
     * <p>
     * The method checks the context for the specified key and removes it if present.
     * </p>
     *
     * @param key The key to remove.
     */
    public static void remove(final String key) {
        getAttributes().ifPresent(context -> context.remove(key));
    }

    /**
     * Clears all attributes from the ThreadLocal context.
     * <p>
     * This method completely removes the context associated with the current thread.
     * </p>
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }
}

