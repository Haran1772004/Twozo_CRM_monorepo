package com.tlc.crm.entity.validator.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.cache.CacheConfig;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.entity.context.ValidationCacheStateContext;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.validator.EntityViolationsCache;
import com.tlc.validator.TlcModel;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Provides the implementation for {@link EntityViolationsCache}
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class EntityViolationsCacheImpl implements EntityViolationsCache {

    private final Cache<Entity, Collection<ConstraintViolation<TlcModel>>> entityConstraintViolationCache;

    private EntityViolationsCacheImpl() {
        final CacheConfig<Entity, Collection<ConstraintViolation<TlcModel>>> entityValidationCache = new CacheConfig<>();

        entityValidationCache.setMaxSize( 200000 ); // This cache can store up to 2 lakh various entities and its violations at the given time.
        entityConstraintViolationCache = CacheManager.getInstance().createCache(entityValidationCache);
    }

    private static final class Instance {
        private static final EntityViolationsCacheImpl ENTITY_VALIDATION_CACHE = new EntityViolationsCacheImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link  EntityViolationsCacheImpl}
     * </p>
     *
     * @return The instance of {@link EntityViolationsCacheImpl}
     */
    public static EntityViolationsCacheImpl getInstance() {
        return Instance.ENTITY_VALIDATION_CACHE;
    }

    /**
     * {@inheritDoc}
     *
     * @param entity Represents the entity model
     * @return The set of constraint violations of {@link TlcModel}
     */
    @Override
    public Collection<ConstraintViolation<TlcModel>> get(final Entity entity) {
        final Collection<ConstraintViolation<TlcModel>> constraintViolations = entityConstraintViolationCache.get(entity);

        return Objects.isNull(constraintViolations) ? null : Collections.unmodifiableCollection(constraintViolations);
    }

    /**
     * {@inheritDoc}
     *
     * @param entity               Represents the {@link Entity}
     * @param constraintViolations The violations got by validating the entity
     */
    @Override
    public void put(final Entity entity, final Collection<ConstraintViolation<TlcModel>> constraintViolations) {
        entityConstraintViolationCache.put(entity, constraintViolations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        entityConstraintViolationCache.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return ValidationCacheStateContext.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param isCacheEnabled Represents the value of the cache enable key
     */
    @Override
    public void setEnabled(final boolean isCacheEnabled) {
        ValidationCacheStateContext.load(isCacheEnabled);
    }
}