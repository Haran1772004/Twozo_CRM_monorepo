package com.tlc.crm.entity.resolver.internal;

import com.tlc.commons.concurrent.LockStore;

import com.tlc.crm.entity.builder.FieldInfoBuilder;
import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.resolver.FieldResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the {@link FieldInfo} and {@link FieldConfig} related details.
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldResolver
 */
public abstract class AbstractFieldResolver implements FieldResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFieldResolver.class);

    private final FieldInfoBuilder fieldInfoBuilder;
    private final FieldCache fieldCache;
    private final CrmFieldsManager crmFieldsManager;
    private final LockStore<Long> orgLockStore;

    protected AbstractFieldResolver(final CrmFieldsManager crmFieldsManager, final FieldCache fieldCache) {
        this.crmFieldsManager = crmFieldsManager;
        this.fieldCache = fieldCache;

        fieldInfoBuilder = FieldInfoBuilder.getInstance();
        orgLockStore = new LockStore<>(10);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @return The collection of {@link FieldInfo}
     */
    @Override
    public Collection<FieldInfo> get(final Long orgId) {
        final ReentrantReadWriteLock lock = orgLockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Collection<FieldInfo> fields;
        readLock.lock();

        try {
            fields = fieldCache.get(orgId);
        } finally {
            readLock.unlock();
        }

        if (fields.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final long fieldsFetchingStartTime = Instant.now().toEpochMilli();
                final Collection<Field> orgFields = crmFieldsManager.getFields(orgId);

                LOGGER.info("Field Caching: Time taken to fetch the fields from the DB : {}ms", Instant.now().toEpochMilli() - fieldsFetchingStartTime);

                if (Objects.nonNull(orgFields) && !orgFields.isEmpty()) {
                    LOGGER.info("Field Caching: Total fields counts to be loaded in the cache : {}", orgFields.size());
                    final long fieldsBuildingStartTime = Instant.now().toEpochMilli();
                    final Collection<FieldInfo> crmFields = fieldInfoBuilder.build(orgFields);

                    LOGGER.info("Field Caching: Time taken to build the fields : {}ms", Instant.now().toEpochMilli() - fieldsBuildingStartTime);
                    final long fieldsCachingStartTime = Instant.now().toEpochMilli();

                    fieldCache.addFieldsIntoCache(orgId, crmFields);
                    LOGGER.info("Field Caching: Time taken to load the fields into cache : {}ms", Instant.now().toEpochMilli() - fieldsCachingStartTime);

                    return crmFields;
                }
            } finally {
                writeLock.unlock();
            }
        }
        return fields;
    }

    /**
     * <p>
     * Retrieves a collection of source transient fields
     * </p>
     *
     * @param sourceNames Represents the source names
     * @return the {@link Collection} of {@link FieldInfo}
     */
    @Override
    public Map<String, Collection<FieldInfo>> getDefaultFieldsBySource(final Collection<String> sourceNames) {
        return fieldCache.getDefaultFieldsBySource(sourceNames);
    }

    /**
     * <p>
     * Fetches the transient fields for the given field IDs.
     * </p>
     *
     * @param fieldIds Represents the field IDs.
     * @return The {@link FieldInfo}
     */
    @Override
    public Map<Long, FieldInfo> getDefaultFields(final Collection<Long> fieldIds) {
        return fieldCache.getDefaultFields(fieldIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the org id.
     * @return The {@link Map} containing the key as field id value as default field configuration.
     */
    @Override
    public Map<Long, FieldConfig> getFieldConfigs(final Long orgId) {
        final ReentrantReadWriteLock lock = orgLockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Map<Long, FieldConfig> cachedFieldConfigs;

        readLock.lock();

        try {
            cachedFieldConfigs = fieldCache.getFieldConfigs(orgId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cachedFieldConfigs) || cachedFieldConfigs.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final long fieldConfigFetchingStartTime = Instant.now().toEpochMilli();
                final Collection<FieldConfig> fieldConfigs = crmFieldsManager.getFieldConfigs(orgId);

                LOGGER.info("Field Caching: Time taken to fetch the field config from the DB : {}ms", Instant.now().toEpochMilli() - fieldConfigFetchingStartTime);

                if (!fieldConfigs.isEmpty()) {
                    final long fieldConfigCachingStartTime = Instant.now().toEpochMilli();

                    fieldCache.addFieldConfigsIntoCache(orgId, fieldConfigs);
                    LOGGER.info("Field Caching: Time taken to load the field config into cache : {}ms", Instant.now().toEpochMilli() - fieldConfigCachingStartTime);
                    return fieldConfigs.stream().collect(Collectors.toMap(FieldConfig::getFieldId, Function.identity()));
                }
            } finally {
                writeLock.unlock();
            }
        }

        return cachedFieldConfigs;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of {@link FieldInfo}
     */
    @Override
    public Collection<FieldInfo> getFieldsBySource(final Long orgId, final String sourceName) {
        final Collection<FieldInfo> sourceFields = fieldCache.getFieldsBySource(orgId, sourceName);

        if (Objects.isNull(sourceFields) || sourceFields.isEmpty()) {
            final ReentrantReadWriteLock lock = orgLockStore.getLock(orgId);
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final long fieldsFetchingStartTime = Instant.now().toEpochMilli();
                final Collection<Field> orgFields = crmFieldsManager.getFields(orgId);

                LOGGER.info("Field Caching: Time taken to fetch the fields from the DB : {}ms", Instant.now().toEpochMilli() - fieldsFetchingStartTime);

                if (Objects.nonNull(orgFields) && !orgFields.isEmpty()) {
                    LOGGER.info("Field Caching: Total fields counts to be loaded in the cache : {}", orgFields.size());
                    final long fieldsBuildingStartTime = Instant.now().toEpochMilli();
                    final Collection<FieldInfo> crmFields = FieldInfoBuilder.getInstance().build(orgFields);

                    LOGGER.info("Field Caching: Time taken to build the fields : {}ms", Instant.now().toEpochMilli() - fieldsBuildingStartTime);
                    final long fieldsCachingStartTime = Instant.now().toEpochMilli();

                    fieldCache.addFieldsIntoCache(orgId, crmFields);
                    LOGGER.info("Field Caching: Time taken to load the fields into cache : {}ms", Instant.now().toEpochMilli() - fieldsCachingStartTime);

                    return crmFields.stream().filter(fieldInfo -> Objects.equals(sourceName, fieldInfo.fieldSource().getName())).toList();
                }
            } finally {
                writeLock.unlock();
            }
        }

        return sourceFields;
    }
}