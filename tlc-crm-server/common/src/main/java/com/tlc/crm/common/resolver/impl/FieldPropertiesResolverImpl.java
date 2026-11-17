package com.tlc.crm.common.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.common.cache.internal.FieldPropertiesCacheImpl;
import com.tlc.crm.common.resolver.FieldPropertiesResolver;

import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;

import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the meta data of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPropertiesResolverImpl implements FieldPropertiesResolver {

    private static final byte STRIPES_COUNT = 10;

    private final FieldPropertiesCacheImpl fieldPropertiesCache;
    private final LockStore<Class<? extends TlcModel>> lockStore;

    private static class Instance {
        private static final FieldPropertiesResolverImpl INSTANCE = new FieldPropertiesResolverImpl();
    }

    public static FieldPropertiesResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    private FieldPropertiesResolverImpl() {
        fieldPropertiesCache = FieldPropertiesCacheImpl.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Field> getFields(final Class<? extends TlcModel> modelClass) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(modelClass);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Collection<Field> existingFields;

        readLock.lock();

        try {
            existingFields = fieldPropertiesCache.getFields(modelClass);
        } finally {
            readLock.unlock();
        }

        if (existingFields.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();
            final Collection<Field> fields = new HashSet<>();

            try {
                Class<?> clazz = modelClass;

                while (Objects.nonNull(clazz)) {
                    fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                    clazz = clazz.getSuperclass();
                }
                fieldPropertiesCache.add(modelClass, fields);
            } finally {
                writeLock.unlock();
            }
            return fields;
        }

        return existingFields;
    }
}
