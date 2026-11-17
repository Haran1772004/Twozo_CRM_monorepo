package com.tlc.crm.fields.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.fields.cache.FieldTypeCache;
import com.tlc.crm.fields.manager.FieldTypeManager;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.sql.resource.OMETADDRESS;
import com.tlc.crm.fields.sql.resource.OMETDATERANGE;
import com.tlc.crm.fields.sql.resource.OMETDATETIMERANGE;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.fields.sql.resource.OMETTIMERANGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the field type details into cache and provides API to access the field type details
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class FieldTypeCacheImpl implements FieldTypeCache {

    private final Cache<Long, FieldType> fieldTypeCache;
    private final Cache<String, Map<Long, FieldType>> sourceDependentTypeCache;
    private final Cache<Long, Collection<FieldSubType>> fieldSubTypeCache;
    private final FieldTypeManager fieldTypeManager;
    private final Cache<StoreType, Map<String, Map<StoreSubType, String>>> fieldSubTypeColumnCache;
    private final FieldSourceService fieldSourceService;
    private final ReentrantReadWriteLock lock;

    private static class Instance {
        private static final FieldTypeCacheImpl INSTANCE = new FieldTypeCacheImpl();
    }

    /**
     * Gets the {@link FieldTypeCache} instance
     *
     * @return {@link FieldTypeCacheImpl}
     */
    public static FieldTypeCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private FieldTypeCacheImpl() {
        this.fieldTypeCache = CacheManager.getInstance().createCache();
        this.sourceDependentTypeCache = CacheManager.getInstance().createCache();
        this.fieldSubTypeCache = CacheManager.getInstance().createCache();
        this.fieldTypeManager = FieldTypeManager.getInstance();
        fieldSubTypeColumnCache = CacheManager.getInstance().createCache();
        fieldSourceService = FieldSourceService.getInstance();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<FieldType> fieldTypes = fieldTypeManager.getAllFieldTypes();
        final Map<Long, Collection<FieldSubType>> fieldSubTypes = fieldTypeManager.getGroupedSubTypes();
        final Map<Long, FieldType> commonFieldTypes = fieldTypes.stream().filter(FieldType::isCommon)
                .collect(Collectors.toMap(FieldType::id, fieldType -> fieldType));
        final Map<String, Map<Long, FieldType>> sourceDependentFieldTypes = fieldTypes.stream().filter(fieldType -> !fieldType.isCommon() && Objects.nonNull(fieldType.source()))
                .collect(Collectors.groupingBy(type -> type.source().getName(), Collectors.toMap(FieldType::id, field -> field)));

        sourceDependentTypeCache.putAll(sourceDependentFieldTypes);
        fieldSourceService.getSources().forEach(source -> sourceDependentTypeCache.computeIfAbsent(source.getName(),
                type -> new HashMap<>()).putAll(commonFieldTypes));

        if (!fieldTypes.isEmpty()) {
            fieldTypes.forEach(type -> fieldTypeCache.put(type.id(), type));
        }

        if (!fieldSubTypes.isEmpty()) {
            fieldSubTypeCache.putAll(fieldSubTypes);
        }
        fieldSubTypeColumnCache.putAll(groupFieldSubType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        fieldTypeCache.clear();
        fieldSubTypeCache.clear();
        fieldSubTypeColumnCache.clear();
        sourceDependentTypeCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId represents the Field type ID
     * @return {@link FieldType} which contains the field type info
     */
    @Override
    public FieldType get(final Long fieldTypeId) {
        return fieldTypeCache.get(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link FieldType}
     */
    @Override
    public Collection<FieldType> getAllFieldTypes() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Collection<FieldType> fieldTypes = new ArrayList<>();

            fieldTypeCache.forEach((id, fieldType) -> fieldTypes.add(fieldType));

            return fieldTypes;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link FieldSubType}
     */
    @Override
    public Collection<FieldSubType> getAllFieldSubTypes() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Collection<FieldSubType> fieldSubTypes = new ArrayList<>();

            fieldSubTypeCache.forEach((typeId, subTypes) -> fieldSubTypes.addAll(subTypes));

            return fieldSubTypes;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @param storeType Represents the {@link StoreType}.
     * @return Outer {@link Map} key as store type table name, inner {@link Map} key as {@link StoreSubType} value as their table column name
     */
    @Override
    public Map<String, Map<StoreSubType, String>> getFieldSubTypeColumns(final StoreType storeType) {
        return fieldSubTypeColumnCache.get(storeType);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id
     * @return The {@link Collection} of {@link FieldSubType}
     */
    @Override
    public Collection<FieldSubType> getFieldSubtypes(final Long fieldTypeId) {
        return fieldSubTypeCache.get(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName Represents the source name.
     * @return The {@link Map} containing key as field type id value as {@link FieldType}.
     */
    @Override
    public Map<Long, FieldType> getSourceDependentFieldType(final String sourceName) {
        return sourceDependentTypeCache.get(sourceName);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldType Represents the {@link FieldType}
     */
    @Override
    public void setFieldType(final FieldType fieldType) {
        fieldTypeCache.put(fieldType.id(), fieldType);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId   Represents the field type id
     * @param fieldSubTypes Represents the collection of {@link FieldSubType}
     */
    @Override
    public void setFieldSubType(final Long fieldTypeId, final Collection<FieldSubType> fieldSubTypes) {
        fieldSubTypeCache.put(fieldTypeId, fieldSubTypes);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldType Represents the {@link FieldType}
     */
    @Override
    public void setSourceDependentType(final FieldType fieldType) {
        sourceDependentTypeCache.computeIfAbsent(fieldType.source().getName(), fieldTypeMap ->
                new HashMap<>()).put(fieldType.id(), fieldType);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupedFieldSubtypeColumns Which contains the group of field subtype columns
     */
    @Override
    public void setFieldSubTypeColumn(final Map<StoreType, Map<String, Map<StoreSubType, String>>> groupedFieldSubtypeColumns) {
        fieldSubTypeColumnCache.putAll(groupedFieldSubtypeColumns);
    }

    /**
     * {@inheritDoc}
     *
     * @return Grouped field subtype columns.
     */
    public Map<StoreType, Map<String, Map<StoreSubType, String>>> groupFieldSubType() {
        final Map<StoreSubType, String> addressColumns = new HashMap<>(Map.of(
                StoreSubType.CITY, OMETADDRESS.CITY,
                StoreSubType.STATE, OMETADDRESS.STATE,
                StoreSubType.COUNTRY, OMETADDRESS.COUNTRY,
                StoreSubType.ZIPCODE, OMETADDRESS.ZIPCODE,
                StoreSubType.ADDRESS_LINE1, OMETADDRESS.ADDRESS_LINE1,
                StoreSubType.ADDRESS_LINE2, OMETADDRESS.ADDRESS_LINE2));

        addressColumns.putAll(Map.of(
                StoreSubType.LATITUDE, OMETADDRESS.LATITUDE,
                StoreSubType.LONGITUDE, OMETADDRESS.LONGITUDE));
        final Map<String, Map<StoreSubType, String>> addressTableDetails = new HashMap<>(
                Map.of(OMETADDRESS.TABLE, addressColumns));

        return Map.of(
                StoreType.PHONE, Map.of(OMETPHONE.TABLE, Map.of(StoreSubType.PHONE_TYPE, OMETPHONE.TYPE,
                        StoreSubType.PHONE_NUMBER, OMETPHONE.NUMBER, StoreSubType.COUNTRY_CODE, OMETPHONE.COUNTRY_ID)),
                StoreType.EMAIL, Map.of(OMETEMAIL.TABLE, Map.of(StoreSubType.EMAIL_TYPE, OMETEMAIL.TYPE,
                        StoreSubType.EMAIL_VALUE, OMETEMAIL.VALUE)),
                StoreType.PHONES, Map.of(OMETPHONE.TABLE, Map.of(StoreSubType.PHONE_TYPE, OMETPHONE.TYPE,
                        StoreSubType.PHONE_NUMBER, OMETPHONE.NUMBER, StoreSubType.COUNTRY_CODE, OMETPHONE.COUNTRY_ID)),
                StoreType.EMAILS, Map.of(OMETEMAIL.TABLE, Map.of(StoreSubType.EMAIL_TYPE, OMETEMAIL.TYPE,
                        StoreSubType.EMAIL_VALUE, OMETEMAIL.VALUE)),
                StoreType.MONETARY, Map.of(OMETMONETARY.TABLE, Map.of(StoreSubType.VALUE, OMETMONETARY.VALUE,
                        StoreSubType.CURRENCY, OMETMONETARY.CURRENCY_ID)),
                StoreType.DEAL_VALUE, Map.of(OMETMONETARY.TABLE, Map.of(StoreSubType.VALUE, OMETMONETARY.VALUE,
                        StoreSubType.CURRENCY, OMETMONETARY.CURRENCY_ID)),
                StoreType.DATE_RANGE, Map.of(OMETDATERANGE.TABLE, Map.of(StoreSubType.FROM, OMETDATERANGE.FROM,
                        StoreSubType.TO, OMETDATERANGE.TO)),
                StoreType.TIME_RANGE, Map.of(OMETTIMERANGE.TABLE, Map.of(StoreSubType.FROM, OMETTIMERANGE.FROM,
                        StoreSubType.TO, OMETTIMERANGE.TO)),
                StoreType.DATE_TIME_RANGE, Map.of(OMETDATETIMERANGE.TABLE, Map.of(StoreSubType.FROM, OMETDATETIMERANGE.FROM,
                        StoreSubType.TO, OMETDATETIMERANGE.TO)),
                StoreType.ADDRESS, addressTableDetails);
    }
}