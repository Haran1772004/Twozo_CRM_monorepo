package com.tlc.crm.entity.internal.context;

import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.crm.entity.context.ValidationCacheStateContext;

/**
 * A holder that holds the {@link ValidationCacheStateContext} value
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class ValidationCacheStateContextHolder extends ContextHolder<ValidationCacheStateContext> {

    private ValidationCacheStateContextHolder() {
    }

    private static class Instance {
        private static final ValidationCacheStateContextHolder VALIDATION_CACHE_CONTEXT_HOLDER = new ValidationCacheStateContextHolder();
    }

    /**
     * Gets the {@link ValidationCacheStateContextHolder} instance
     *
     * @return {@link ValidationCacheStateContextHolder}
     */
    public static ValidationCacheStateContextHolder getInstance() {
        return Instance.VALIDATION_CACHE_CONTEXT_HOLDER;
    }
}