package com.tlc.commons.executor.context;

import com.tlc.commons.executor.context.registry.ContextHolderRegistry;

/**
 * Active context binding on the current thread. Clears ALL holders on close.
 * Must be used in try-with-resources.
 *
 * @author kumar
 * @version 1.0
 */
public final class ContextScope implements AutoCloseable {

    private volatile boolean closed;

    ContextScope() {
        this.closed = false;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            for (final ContextHolder<?> holder : ContextHolderRegistry.getInstance().getContextHolders()) {
                try {
                    holder.clear();
                } catch (final Exception ignored) {
                }
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
