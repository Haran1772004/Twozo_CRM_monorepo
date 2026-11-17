package com.tlc.crm.entity.context;

import com.tlc.commons.executor.context.Context;
import com.tlc.crm.entity.internal.context.ValidationCacheStateContextHolder;

import java.util.Optional;

/**
 * Represents the cache context to hold the validation cache state
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class ValidationCacheStateContext implements Context {

    private final boolean isCacheEnabled;

    private ValidationCacheStateContext(final boolean isCacheEnabled) {
        this.isCacheEnabled = isCacheEnabled;
    }

    /**
     * Loads the data into the cache state context holder
     *
     * @param isCacheEnabled to be loaded into the cache context holder.
     */
    public static void load(final boolean isCacheEnabled) {

        if (!exists()) {
            ValidationCacheStateContextHolder.getInstance().setContext(new ValidationCacheStateContext(isCacheEnabled));
        }
    }

    /**
     * Get the cache enabled value from the cache state context holder
     *
     * @return The value of the cache enabled key
     */
    public static boolean get() {
        final Optional<ValidationCacheStateContext> cacheContext = ValidationCacheStateContextHolder.getInstance().getContext();

        return cacheContext.map(ValidationCacheStateContext::isCacheEnabled).orElse(false);
    }

    /**
     * Checks if the cache state context data exists.
     *
     * @return true if the cache context data exists, false otherwise
     */
    public static boolean exists() {
        return ValidationCacheStateContextHolder.getInstance().exists();
    }

    /**
     * Clears the cache context value from the current thread
     */
    public static void clear() {
        ValidationCacheStateContextHolder.getInstance().clear();
    }

    /**
     * <p>
     * Checks whether the cache is enabled or not
     * </p>
     *
     * @return True if the cache is enabled, false otherwise
     */
    public boolean isCacheEnabled() {
        return isCacheEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return EntityContextType.ENTITY_VALIDATION_CACHE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context copy() {
        return new ValidationCacheStateContext(isCacheEnabled);
    }
}