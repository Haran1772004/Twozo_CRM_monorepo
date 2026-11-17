package com.tlc.crm.fields.service;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.internal.FieldTypeServiceImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Provides an abstract way to deal with {@link FieldType}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldTypeService {

    /**
     * <p>
     * Gets the {@link FieldTypeService} instance.
     * </p>
     *
     * @return {@link FieldTypeService}.
     */
    static FieldTypeService getInstance() {
        return FieldTypeServiceImpl.getInstance();
    }

    /**
     * Gets the field type by its id
     *
     * @param id Represents the field type ID
     * @return {@link FieldType} which contains the field type information.
     */
    FieldType get(final Long id);

    /**
     * Gets the field type by its id
     *
     * @param ids Represents the {@link Collection} of field type ID
     * @return {@link FieldType} which contains the field type information.
     */
    Collection<FieldType> getFieldTypesByIds(final Collection<Long> ids);

    /**
     * Groups the field subtypes by the {@link StoreSubType}
     *
     * @param typeId Represents the field type ID
     * @return {@link Map} of grouped field subtypes by its store subtype
     */
    Map<StoreSubType, FieldSubType> groupSubtypesByStoreSubType(final Long typeId);

    /**
     * Groups the field subtypes by the {@link StoreSubType}
     *
     * @param storeType Represents the store type
     * @return {@link Map} of grouped field subtypes by its store subtype
     */
    Map<StoreSubType, FieldSubType> groupSubtypesByStoreSubType(final StoreType storeType);

    /**
     * Gets all the field subtypes by field type ID
     *
     * @param fieldTypeId Represents the field type ID
     * @return {@link Collection} of {@link FieldSubType}
     */
    Collection<FieldSubType> getSubTypesByFieldTypeId(final Long fieldTypeId);

    /**
     * Gets all the field subtypes by store type
     *
     * @param storeType Represents the store type
     * @return {@link Collection} of {@link FieldSubType}
     */
    Collection<FieldSubType> getSubTypesByStoreType(final StoreType storeType);

    /**
     * Gets the field subtype by its ID
     *
     * @param id Represents the field subtype id
     * @return {@link FieldSubType} which contains the field subtype info
     */
    FieldSubType getSubType(final Long id);

    /**
     * Gets the field type by the {@link StoreType}
     *
     * @param storeType Represents the store type
     * @return {@link FieldType} which contains the field type info
     */
    FieldType getFieldTypeByStoreType(final StoreType storeType);

    /**
     * Gets the {@link Collection} of field type by the {@link StoreType}
     *
     * @param storeTypes Represents the {@link Collection} store type
     * @return the {@link Collection} of {@link FieldType} which contains the field type info
     */
    Collection<FieldType> getFieldTypesByStoreTypes(final Collection<StoreType> storeTypes);

    /**
     * Gets the field type by the {@link StoreSubType}
     *
     * @param storeType    Represents the {@link StoreType}
     * @param storeSubType Represents the store subtype
     * @return {@link FieldSubType} which contains the field type info
     */
    FieldSubType getFieldSubTypeByStoreType(final StoreType storeType, final StoreSubType storeSubType);

    /**
     * Gets the common field types
     *
     * @return {@link Collection} of {@link FieldType}
     */
    Collection<FieldType> getCommonFieldTypes();

    /**
     * Gets all the source field types by given entity sources
     *
     * @param entitySources Represents the entity sources
     * @return {@link Collection} of {@link FieldType}
     */
    Collection<FieldType> getTypesBySource(final String... entitySources);

    /**
     * Gets all the source field types by given entity sources
     *
     * @param entitySources Represents the collection of entity sources
     * @return {@link Collection} of {@link FieldType}
     */
    Collection<FieldType> getTypesBySource(final Collection<String> entitySources);

    /**
     * Gets all the field types
     *
     * @return {@link Collection} of {@link FieldType}
     */
    Collection<FieldType> getAllFieldTypes();

    /**
     * Groups the {@link FieldSubType} ID string by {@link StoreSubType}
     *
     * @param storeType Represents the store type
     * @return {@link Map} grouped by {@link StoreSubType} and field subtype ID string
     */
    Map<StoreSubType, String> groupStoreSubTypeIds(final StoreType storeType);

    /**
     * Gets the field type by the {@link StoreSubType}.
     *
     * @param subTypeName Represents the name of the subtype.
     * @param storeType   Represents the store type.
     * @return {@link FieldSubType} which contains the field type info.
     */
    Optional<FieldSubType> getFieldSubTypesByName(final String subTypeName, final StoreType storeType);

    /**
     * <p>
     * Fetches the column name of the field subtype.
     * </p>
     *
     * @param storeType For fetch the column name.
     * @return Column name of the field subtype.
     */
    Map<String, Map<StoreSubType, String>> getTableDetails(final StoreType storeType);

    /**
     * <p>
     * Retrieves a collection of {@code FieldSubType} objects that are filterable based on the provided field type ID.
     * </p>
     *
     * @param fieldTypeId the unique identifier of the field type whose filterable subtypes are to be retrieved
     * @return a collection of filterable {@code FieldSubType} objects associated with the specified field type ID
     */
    Collection<FieldSubType> getFilterableFieldSubType(final Long fieldTypeId);
}