package com.tlc.commons.executor.context;

import java.util.Optional;

/**
 * A holder that holds the {@link Context} value
 *
 * @param <T> Represents the type of the context
 * @author sundar
 * @version 1.0
 */
public abstract class ContextHolder<T extends Context> {

    private final ThreadLocal<T> contextValueHolder;

    protected ContextHolder() {
        this.contextValueHolder = new InheritableThreadLocal<>();
    }

    /**
     * Gets the value of the context
     *
     * @return {@link T}
     */
    public final Optional<T> getContext() {
        return Optional.ofNullable(contextValueHolder.get());
    }

    /**
     * Sets the context value
     *
     * @param context Represent the value to be loaded into the current context
     */
    public final void setContext(final Context context) {
        contextValueHolder.set((T) context);
    }

    /**
     * Checks if the context value exists for the current thread
     *
     * @return true if the context value exists for the current thread; false otherwise
     */
    public final boolean exists() {
        return getContext().isPresent();
    }

    /**
     * Clears the value associated in the current context
     */
    public final void clear() {
        cleanUp();
        contextValueHolder.remove();
    }

    /**
     * Executes final activities before removing the context value from the current thread
     */
    protected void cleanUp() {
    }
}