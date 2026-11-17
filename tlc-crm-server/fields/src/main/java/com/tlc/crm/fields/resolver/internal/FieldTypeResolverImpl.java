package com.tlc.crm.fields.resolver.internal;

import com.tlc.crm.fields.cache.FieldTypeCache;
import com.tlc.crm.fields.manager.FieldTypeManager;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.resolver.FieldTypeResolver;
import com.tlc.crm.fields.service.FieldSourceService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the {@link FieldType} related details
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldTypeResolver
 */
public class FieldTypeResolverImpl implements FieldTypeResolver {

    private final FieldTypeManager fieldTypeManager;
    private final FieldTypeCache fieldTypeCache;
    private final FieldSourceService fieldSourceService;
    private final ReentrantReadWriteLock lock;

    private final AtomicBoolean fieldTypeCacheLoaded;
    private final AtomicBoolean fieldSubTypeCacheLoaded;
    private final AtomicBoolean fieldSubTypeColumnCacheLoaded;

    private FieldTypeResolverImpl() {
        fieldTypeCache = FieldTypeCache.getInstance();
        fieldTypeManager = FieldTypeManager.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        lock = new ReentrantReadWriteLock();
        fieldTypeCacheLoaded = new AtomicBoolean();
        fieldSubTypeCacheLoaded = new AtomicBoolean();
        fieldSubTypeColumnCacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private final static FieldTypeResolverImpl INSTANCE = new FieldTypeResolverImpl();
    }

    /**
     * Gets the instance of {@link FieldTypeResolverImpl}.
     *
     * @return {@link FieldTypeResolverImpl}.
     */
    public static FieldTypeResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link FieldType}
     */
    @Override
    public Collection<FieldType> getAllFieldTypes() {
        if (!fieldTypeCacheLoaded.get()) {
            reloadFieldType();
        }

        return fieldTypeCache.getAllFieldTypes();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link FieldSubType}
     */
    @Override
    public Collection<FieldSubType> getAllFieldSubTypes() {
        if (!fieldSubTypeCacheLoaded.get()) {
            reloadFieldSubtypes();
        }

        return fieldTypeCache.getAllFieldSubTypes();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId represents the field type ID.
     * @return The {@link FieldType}.
     */
    @Override
    public FieldType getFieldType(final Long fieldTypeId) {
        final FieldType existingFieldType = fieldTypeCache.get(fieldTypeId);

        if (Objects.isNull(existingFieldType)) {
            final FieldType fieldType = fieldTypeManager.get(fieldTypeId);

            fieldTypeCache.setFieldType(fieldType);

            return fieldType;
        }

        return existingFieldType;
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType Represents the {@link StoreType}.
     * @return Outer {@link Map} key as store type table name, inner {@link Map} key as {@link StoreSubType} value as their table column name.
     */
    @Override
    public Map<String, Map<StoreSubType, String>> getFieldSubTypeColumns(final StoreType storeType) {
        final Map<String, Map<StoreSubType, String>> fieldSubTypeColumns = fieldTypeCache.getFieldSubTypeColumns(storeType);

        if (Objects.isNull(fieldSubTypeColumns)) {

            if (!fieldSubTypeColumnCacheLoaded.get()) {
                reloadFieldSubTypeColumns();
            }

            return fieldTypeCache.getFieldSubTypeColumns(storeType);
        }

        return fieldSubTypeColumns;
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName Represents the source name.
     * @return The {@link Map} containing key as field type id value as {@link FieldType}.
     */
    @Override
    public Map<Long, FieldType> getSourceDependentFieldTypes(final String sourceName) {
        final Map<Long, FieldType> sourceDependentTypes = fieldTypeCache.getSourceDependentFieldType(sourceName);

        if (Objects.isNull(sourceDependentTypes)) {
            final Collection<FieldType> sourceDependentFieldTypes =
                    fieldTypeManager.getSourceDependentFieldType(fieldSourceService.getSource(sourceName).id());

            for (final FieldType fieldType : sourceDependentFieldTypes) {
                fieldTypeCache.setSourceDependentType(fieldType);
            }

            return sourceDependentFieldTypes.stream().collect(Collectors.toMap(FieldType::id, Function.identity()));
        }

        return sourceDependentTypes;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type ID.
     * @return The {@link Collection} of {@link FieldSubType}.
     */
    @Override
    public Collection<FieldSubType> getFieldSubtypes(final Long fieldTypeId) {
        final Collection<FieldSubType> fieldSubTypes = fieldTypeCache.getFieldSubtypes(fieldTypeId);

        if (Objects.isNull(fieldSubTypes)) {
            final Collection<FieldSubType> subTypes = fieldTypeManager.getFieldSubTypes(fieldTypeId);

            fieldTypeCache.setFieldSubType(fieldTypeId, subTypes);

            return subTypes;
        }

        return fieldSubTypes;
    }

    /**
     * <p>
     * Reloads the field types into cache.
     * </p>
     */
    private void reloadFieldType() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!fieldTypeCacheLoaded.get()) {
                final Collection<FieldType> fieldTypes = fieldTypeManager.getAllFieldTypes();

                if (!fieldTypes.isEmpty()) {

                    for (final FieldType fieldType : fieldTypes) {
                        fieldTypeCache.setFieldType(fieldType);
                    }
                    fieldTypeCacheLoaded.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * Reloads the field subtypes into cache.
     * </p>
     */
    private void reloadFieldSubtypes() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!fieldSubTypeCacheLoaded.get()) {

                for (final Map.Entry<Long, Collection<FieldSubType>> subtypesEntry : fieldTypeManager.getGroupedSubTypes().entrySet()) {
                    fieldTypeCache.setFieldSubType(subtypesEntry.getKey(), subtypesEntry.getValue());
                }
                fieldSubTypeCacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * Reloads the field subtype columns into cache.
     * </p>
     */
    private void reloadFieldSubTypeColumns() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!fieldSubTypeColumnCacheLoaded.get()) {
                fieldTypeCache.setFieldSubTypeColumn(fieldTypeCache.groupFieldSubType());
                fieldSubTypeColumnCacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}