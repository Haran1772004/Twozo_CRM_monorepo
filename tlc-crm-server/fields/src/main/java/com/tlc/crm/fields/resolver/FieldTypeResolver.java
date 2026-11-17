package com.tlc.crm.fields.resolver;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.resolver.internal.FieldTypeResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A resolver that resolves the {@link FieldType} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldTypeResolver {

    /**
     * <p>
     * Gets the instance of {@link FieldTypeResolver}.
     * </p>
     *
     * @return {@link FieldTypeResolver}.
     */
    static FieldTypeResolver getInstance() {
        return FieldTypeResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches a collection of field types. If not cached, fetches data from the database,
     * caches it, and returns the field types from the cache.
     * </p>
     *
     * @return {@link Collection} of {@link FieldType}
     */
    Collection<FieldType> getAllFieldTypes();

    /**
     * <p>
     * Fetches a collection of the field subtypes. If not cached, fetches data from the database,
     * caches it, and returns the field subtypes from the cache.
     * </p>
     *
     * @return {@link Collection} of {@link FieldSubType}
     */
    Collection<FieldSubType> getAllFieldSubTypes();

    /**
     * <p>
     * Fetches the field type by given field type ID. If not cached, fetches data from the database,
     * caches it, and returns field type from the cache.
     * </p>
     *
     * @param id Represents the field type ID.
     * @return The {@link FieldType}.
     */
    FieldType getFieldType(final Long id);

    /**
     * <p>
     * Fetches store subtype columns grouped by their table names for the provided store type. If not cached,
     * fetches data from the specified enum class, caches it, and returns store subtype column from the cache.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}
     * @return Outer {@link Map} key as store type table name, inner {@link Map} key as {@link StoreSubType} value as their table column name.
     */
    Map<String, Map<StoreSubType, String>> getFieldSubTypeColumns(final StoreType storeType);

    /**
     * <p>
     * Fetches field types are grouped by their ID for the provided source name. If source dependent field types
     * are not cached, it fetches data from the database, caches it, and returns field types from the cache.
     * </p>
     *
     * @param sourceName Represents the source name.
     * @return The {@link Map} containing key as field type id value as {@link FieldType}.
     */
    Map<Long, FieldType> getSourceDependentFieldTypes(final String sourceName);

    /**
     * <p>
     * Fetches a collection of field subtypes for the provided field type ID. If not cached, fetches data from the database,
     * caches it, and returns field subtypes from the cache.
     * </p>
     *
     * @param fieldTypeId Represents the field type ID.
     * @return The {@link Collection} of {@link FieldSubType}
     */
    Collection<FieldSubType> getFieldSubtypes(final Long fieldTypeId);
}