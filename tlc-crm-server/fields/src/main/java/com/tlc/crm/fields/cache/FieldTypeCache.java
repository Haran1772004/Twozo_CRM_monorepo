package com.tlc.crm.fields.cache;

import com.tlc.crm.fields.cache.internal.FieldTypeCacheImpl;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import java.util.Collection;
import java.util.Map;

/**
 * Provides an abstract way to deal with {@link FieldType}
 *
 * @author sundar
 * @version 1.0
 */
public interface FieldTypeCache extends LocalCache {

    /**
     * Gets the instance of {@link FieldTypeCache}
     *
     * @return {@link FieldTypeCache}
     */
    static FieldTypeCache getInstance() {
        return FieldTypeCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the field type by its ID from cache
     * </p>
     *
     * @param id represents the Field type ID
     * @return {@link FieldType} which contains the field type info
     */
    FieldType get(final Long id);

    /**
     * <p>
     * Gets all the field types from cache
     * <p>
     *
     * @return {@link Collection} of {@link FieldType}
     */
    Collection<FieldType> getAllFieldTypes();

    /**
     * <p>
     * Gets all the field subtypes from cache
     * </p>
     *
     * @return {@link Collection} of {@link FieldSubType}
     */
    Collection<FieldSubType> getAllFieldSubTypes();

    /**
     * <p>
     * Fetches collection of store subtype columns grouped by their table names for the given store type.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}.
     * @return Outer {@link Map} key as store type table name, inner {@link Map} key as {@link StoreSubType} value as their table column name
     */
    Map<String, Map<StoreSubType, String>> getFieldSubTypeColumns(final StoreType storeType);

    /**
     * <p>
     * Fetches the field subtypes by given field type id.
     * </p>
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Collection} of {@link FieldSubType}
     */
    Collection<FieldSubType> getFieldSubtypes(final Long fieldTypeId);

    /**
     * <p>
     * Fetches source dependent field types grouped by their ids for the given source name
     * </p>
     *
     * @param sourceName  Represents the source name.
     * @return The {@link Map} containing key as field type id value as {@link FieldType}.
     */
    Map<Long, FieldType> getSourceDependentFieldType(final String sourceName);

    /**
     * <p>
     * Groups the field sub type and their columns.
     * </p>
     *
     * @return Outer {@link Map} key as {@link StoreType}, inner {@link Map} group of store subtypes columns
     */
    Map<StoreType, Map<String, Map<StoreSubType, String>>> groupFieldSubType();

    /**
     * <p>
     * Sets the field type into the cache.
     * </p>
     *
     * @param fieldType Represents the {@link FieldType}
     */
    void setFieldType(final FieldType fieldType);

    /**
     * <p>
     * Sets the field subtype details into the cache
     * </p>
     *
     * @param fieldTypeId   Represents the field type id
     * @param fieldSubTypes Represents the collection of {@link FieldSubType}
     */
    void setFieldSubType(final Long fieldTypeId, final Collection<FieldSubType> fieldSubTypes);

    /**
     * <p>
     * Sets the source dependent field type into the cache
     * </p>
     *
     * @param fieldType Represents the {@link FieldType}
     */
    void setSourceDependentType(final FieldType fieldType);

    /**
     * <p>
     * Sets the field subtype columns into the cache.
     * </p>
     *
     * @param groupedFieldSubtypeColumns Which contains the group of field subtype columns
     */
    void setFieldSubTypeColumn(final Map<StoreType, Map<String, Map<StoreSubType, String>>> groupedFieldSubtypeColumns);
}