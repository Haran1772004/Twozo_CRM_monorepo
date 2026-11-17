package com.tlc.crm.fields.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.fields.internal.status.FieldErrorCodes;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.resolver.FieldTypeResolver;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements the {@link FieldTypeService} provides functionality for accessing field type.
 *
 * @author kishore J
 * @version 1.0
 * @see FieldTypeService
 */
public class FieldTypeServiceImpl implements FieldTypeService {

    private static final String IS_FILTERABLE = "isFilterable";

    private final FieldTypeResolver fieldTypeResolver;

    private FieldTypeServiceImpl() {
        fieldTypeResolver = FieldTypeResolver.getInstance();
    }

    private static class Instance {
        private static final FieldTypeServiceImpl INSTANCE = new FieldTypeServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FieldTypeServiceImpl}.
     * </p>
     *
     * @return {@link FieldTypeServiceImpl}.
     */
    public static FieldTypeServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the field type id.
     * @return {@link FieldType} which contains the field type information
     */
    @Override
    public FieldType get(final Long id) {
        return fieldTypeResolver.getFieldType(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the field type id.
     * @return {@link FieldType} which contains the field type information
     */
    @Override
    public Collection<FieldType> getFieldTypesByIds(final Collection<Long> ids) {
        return fieldTypeResolver.getAllFieldTypes().stream().filter(Objects::nonNull)
                .filter(fieldType -> ids.contains(fieldType.id())).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param typeId Represents the field type id
     * @return {@link Map} of grouped field subtypes by its store subtype
     */
    @Override
    public Map<StoreSubType, FieldSubType> groupSubtypesByStoreSubType(final Long typeId) {
        final Collection<FieldSubType> fieldSubTypes = getSubTypesByFieldTypeId(typeId);

        return Objects.isNull(fieldSubTypes) ? Collections.emptyMap()
                : fieldSubTypes.stream().collect(Collectors.toMap(FieldSubType::storeSubType, Function.identity()));
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType Represents the store type
     * @return {@link Map} of grouped field subtypes by its store subtype
     */
    @Override
    public Map<StoreSubType, FieldSubType> groupSubtypesByStoreSubType(final StoreType storeType) {
        final Map<StoreSubType, FieldSubType> fieldSubTypes = new TreeMap<>(Comparator.comparing(StoreSubType::getType));

        for (final FieldSubType fieldSubType : getSubTypesByStoreType(storeType)) {
            fieldSubTypes.put(fieldSubType.storeSubType(), fieldSubType);
        }

        return fieldSubTypes;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id
     * @return {@link Collection} of {@link FieldSubType}
     */
    @Override
    public Collection<FieldSubType> getSubTypesByFieldTypeId(final Long fieldTypeId) {
        return fieldTypeResolver.getFieldSubtypes(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType Represents the store type
     * @return {@link Collection} of {@link FieldSubType}
     */
    @Override
    public Collection<FieldSubType> getSubTypesByStoreType(final StoreType storeType) {
        final FieldType fieldType = getFieldTypeByStoreType(storeType);

        return Objects.nonNull(fieldType) ? getSubTypesByFieldTypeId(fieldType.id()) : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the field subtype id
     * @return {@link FieldSubType} which contains the field subtype info
     */
    @Override
    public FieldSubType getSubType(final Long id) {
        final Collection<FieldSubType> fieldSubTypes = fieldTypeResolver.getAllFieldSubTypes();

        return Objects.isNull(fieldSubTypes) ? null : fieldSubTypes.stream().filter(fieldSubType ->
                Objects.equals(id, fieldSubType.id())).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType Represents the store type
     * @return {@link FieldType} which contains the field type info
     */
    @Override
    public FieldType getFieldTypeByStoreType(final StoreType storeType) {
        final Collection<FieldType> fieldTypes = FieldTypeResolver.getInstance().getAllFieldTypes();

        return Objects.isNull(fieldTypes) ? null : fieldTypes.stream().filter(fieldType ->
                fieldType.storeType() == storeType).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType Represents the {@link Collection} store type
     * @return
     */
    @Override
    public Collection<FieldType> getFieldTypesByStoreTypes(final Collection<StoreType> storeType) {
        final Collection<FieldType> fieldTypes = FieldTypeResolver.getInstance().getAllFieldTypes();

        if(Objects.nonNull(fieldTypes) && !fieldTypes.isEmpty()) {
            final Collection<FieldType> filteredFieldTypes = new ArrayList<>();

            for(final FieldType fieldType : fieldTypes) {

                if( storeType.contains(fieldType.storeType())) {
                    filteredFieldTypes.add(fieldType);
                }
            }

            return filteredFieldTypes;
        }

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType    Represents the {@link StoreType}
     * @param storeSubType Represents the store subtype
     * @return {@link FieldSubType} which contains the field subtype info
     */
    @Override
    public FieldSubType getFieldSubTypeByStoreType(final StoreType storeType, final StoreSubType storeSubType) {
        final Collection<FieldSubType> fieldSubTypes = fieldTypeResolver.getAllFieldSubTypes();

        return Objects.isNull(fieldSubTypes) ? null : fieldSubTypes.stream().filter(subType -> storeType == subType.fieldType().storeType() &&
                storeSubType == subType.storeSubType()).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link FieldType}
     */
    @Override
    public Collection<FieldType> getCommonFieldTypes() {
        return fieldTypeResolver.getAllFieldTypes();
    }

    /**
     * <p>
     * Fetches the column name of the field subtype.
     * </p>
     *
     * @param fieldSubType For fetch the column name.
     * @return Column name of the field subtype.
     */
    public final String getSubTypeColumn(final FieldSubType fieldSubType) {
        final Map<String, Map<StoreSubType, String>> groupedFieldSubTypeColumns = fieldTypeResolver.getFieldSubTypeColumns
                (fieldSubType.fieldType().storeType());
        final StoreSubType storeSubType = fieldSubType.storeSubType();

        for (final Map<StoreSubType, String> subTypeColumns : groupedFieldSubTypeColumns.values()) {
            if (Objects.nonNull(subTypeColumns) && subTypeColumns.containsKey(storeSubType)) {
                return subTypeColumns.get(storeSubType);
            }
        }

        throw ErrorCode.get(FieldErrorCodes.FIELD_SUBTYPE_COLUMN_NOT_FOUND, String.format("Field subtype : %s", storeSubType.toString()));
    }

    /**
     * {@inheritDoc}
     *
     * @param entitySources Represents the entity sources
     * @return {@link Collection} of {@link FieldType}
     */
    @Override
    public Collection<FieldType> getTypesBySource(final String... entitySources) {
        return getTypesBySource(List.of(entitySources));
    }

    /**
     * {@inheritDoc}
     *
     * @param entitySources Represents the collection of entity sources
     * @return {@link Collection} of {@link FieldType}
     */
    @Override
    public Collection<FieldType> getTypesBySource(final Collection<String> entitySources) {
        final List<FieldType> fields = new ArrayList<>();

        for (final String entitySource : entitySources) {
            final Map<Long, FieldType> sourceFields = fieldTypeResolver.getSourceDependentFieldTypes(entitySource);

            if (Objects.nonNull(sourceFields) && !sourceFields.isEmpty()) {
                fields.addAll(sourceFields.values());
            }
        }

        return fields;
    }

    /**
     * <p>
     * Fetches the column name of the field subtype.
     * </p>
     *
     * @param storeType For fetch the column name.
     * @return Column name of the field subtype.
     */
    public final Map<String, Map<StoreSubType, String>> getTableDetails(final StoreType storeType) {
        return fieldTypeResolver.getFieldSubTypeColumns(storeType);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link FieldType}
     */
    @Override
    public Collection<FieldType> getAllFieldTypes() {
        return fieldTypeResolver.getAllFieldTypes();
    }

    /**
     * {@inheritDoc}
     *
     * @param storeType Represents the store type
     * @return {@link Map} grouped by {@link StoreSubType} and field subtype ID string
     */
    @Override
    public Map<StoreSubType, String> groupStoreSubTypeIds(final StoreType storeType) {
        return getSubTypesByStoreType(storeType).stream().collect(Collectors.toMap(FieldSubType::storeSubType, FieldSubType::idAsString));
    }

    /**
     * {@inheritDoc}
     *
     * @param subTypeName Represents the name of the subtype.
     * @param storeType   Represents the store type.
     * @return {@link FieldSubType} which contains the field type info.
     */
    @Override
    public Optional<FieldSubType> getFieldSubTypesByName(final String subTypeName, final StoreType storeType) {
        return getSubTypesByStoreType(storeType).stream().filter(fieldSubType -> Objects.equals(fieldSubType.name(), subTypeName)).findFirst();
    }

    /**
     * <p>
     * Retrieves a collection of {@code FieldSubType} objects that are filterable based on the provided field type ID.
     * </p>
     *
     * @param fieldTypeId the unique identifier of the field type whose filterable subtypes are to be retrieved
     * @return a collection of filterable {@code FieldSubType} objects associated with the specified field type ID
     */
    @Override
    public Collection<FieldSubType> getFilterableFieldSubType(final Long fieldTypeId) {
        final Collection<FieldSubType> filterableFieldSubType = getSubTypesByFieldTypeId(fieldTypeId);

        return Objects.isNull(filterableFieldSubType) ? Collections.emptyList() : filterableFieldSubType.stream()
                .filter(fieldSubType -> {
                    final String config = fieldSubType.config();
                    final JsonObject configJson = Objects.nonNull(config) ? Json.object(config) : Json.object();
                    return configJson.optBoolean(IS_FILTERABLE, true);
                }).toList();
    }
}