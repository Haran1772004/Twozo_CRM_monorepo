package com.tlc.crm.entity.validator;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.validator.internal.EntityViolationsCacheImpl;
import com.tlc.validator.TlcModel;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;

/**
 * Stores the violations of the validated {@link Entity} into cache
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public interface EntityViolationsCache extends LocalCache {

    /**
     * <p>
     * Gets the instance of {@link  EntityViolationsCache}
     * </p>
     *
     * @return the instance of {@link EntityViolationsCache}
     */
    static EntityViolationsCache getInstance() {
        return EntityViolationsCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets constraint violations from the cache
     * </p>
     *
     * @param entity Represents the {@link Entity}
     * @return The collection of constraint violations of {@link TlcModel}
     */
    Collection<ConstraintViolation<TlcModel>> get(final Entity entity);

    /**
     * <p>
     * Puts the entity and its constraint violations
     * </p>
     *
     * @param entity               Represents the {@link Entity}
     * @param constraintViolations The violations got by validating the entity
     */
    void put(final Entity entity, final Collection<ConstraintViolation<TlcModel>> constraintViolations);

    /**
     * <p>
     * Clears the cache
     * </p>
     */
    void clear();

    /**
     * <p>
     * Checks whether caching is enabled
     * </p>
     *
     * @return True if cache enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * <p>
     * Enables or disables caching based on the provided value.
     * </p>
     *
     * @param isCacheEnabled Represents the value of the cache enable key
     */
    void setEnabled(final boolean isCacheEnabled);
}