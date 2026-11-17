package com.tlc.crm.core.features.internal.dataimport.builder.impl;

import com.tlc.attachment.model.Record;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.FieldHeader;
import com.tlc.crm.core.features.internal.dataimport.ImportValueFormatter;
import com.tlc.crm.core.features.internal.dataimport.MergeConfig;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.builder.SourceRecordBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.model.ImportFieldWrapper;
import com.tlc.crm.core.features.internal.dataimport.model.SourceRecordBuildRequest;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.internal.formatter.ValueFormatter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides implementation for the service offered by the {@link SourceRecordBuilder} to build {@link SourceRecord} from the {@link Record}
 *
 * @author sundar
 * @version 1.0
 */
public final class SourceRecordBuilderImpl implements SourceRecordBuilder {

    private static final String ID_KEY = ImportFields.ID.getName();
    private static final String DEFAULT_ERROR_MESSAGE = "The %s contains invalid data";

    private final SourceRecordContainer sourceRecordContainer;
    private final Collection<SourceFieldHeaders> sourceFieldHeadersCollection;
    private final Map<Source, Map<Long, FieldInfo>> sourceFields;
    private final FieldTypeService fieldTypeService;
    private final ValueFormatter<ImportFieldWrapper, Object, Object> valueFormatter;
    private final ErrorTracker errorTracker;
    private final SourceSettingsService sourceSettingsService;

    public SourceRecordBuilderImpl(final SourceRecordContainer sourceRecordContainer,
                                   final Map<Source, Map<Long, FieldInfo>> sourceFields,
                                   final Collection<SourceFieldHeaders> sourceFieldHeadersCollection, final ErrorTracker errorTracker) {
        this.sourceRecordContainer = sourceRecordContainer;
        this.sourceFieldHeadersCollection = sourceFieldHeadersCollection;
        this.sourceFields = sourceFields;
        this.fieldTypeService = FieldTypeService.getInstance();
        this.valueFormatter = ImportValueFormatter.getInstance();
        this.errorTracker = errorTracker;
        this.sourceSettingsService = SourceSettingsService.getInstance();
    }

    /**
     * Builds the {@link SourceRecord} with field value mapping and {@link Record}
     *
     * @param sourceRecordBuildRequest Represents the request to build source record
     * @return {@link SourceRecord} that contains the collection of {@link JsonObject} grouped by sources
     */
    @Override
    public SourceRecord build(final SourceRecordBuildRequest sourceRecordBuildRequest) {
        final long orgId = sourceRecordBuildRequest.orgId();
        final Record record = sourceRecordBuildRequest.record();
        final TimezoneInfo timezoneInfo = sourceRecordBuildRequest.timezoneInfo();
        final int index = record.index();
        final SourceRecord sourceRecord = new SourceRecord(index);
        final MergeConfig config = sourceRecordContainer.getMergeConfig();
        final boolean mergeConfigExists = Objects.nonNull(config);
        final FormattedValueContainer formattedValueContainer = new FormattedValueContainer(record);

        for (final SourceFieldHeaders sourceFieldHeaders : sourceFieldHeadersCollection) {
            final Source source = sourceFieldHeaders.getSource();
            final String sourceName = source.getName();

            if (!recordHasValue(record, sourceFieldHeaders.getFieldHeaders())) {
                continue;
            }

            if (sourceSettingsService.isSourceEnabled(orgId, sourceName)) {
                final Map<Long, FieldInfo> fields = sourceFields.get(source);
                final Collection<FieldInfo> distinctFields = sourceRecordContainer.getDistinctFields(source);

                final Map<FieldInfo, Map<Object, SourceRecord>> indexedData = sourceRecordContainer.indexData(source);
                final FieldInfo sourceIdField = fields.values().stream().filter(importField -> Objects.equals(ID_KEY, importField.aliasName()))
                        .findFirst().orElse(null);
                final boolean registeredSource = Source.isPrimarySource(source);
                JsonObject jsonObject = null;

                try {
                    final Optional<FieldHeader> optionalIdHeader = sourceFieldHeaders.getFieldHeader(sourceIdField);
                    Object idValue = null;
                    boolean isUpdatableIndex = false;

                    if (optionalIdHeader.isPresent()) {
                        final FieldHeader idHeader = optionalIdHeader.get();
                        idValue = record.get(idHeader.getHeaderIndex());
                        isUpdatableIndex = true;
                    } else {
                        final Optional<Long> optionalId = sourceRecordContainer.fetchSourceModelId(source, index);

                        if (optionalId.isPresent()) {
                            idValue = optionalId.get();
                        }
                    }

                    if (Objects.nonNull(idValue) && NumberUtils.parseNumber(idValue) instanceof final Long id) {
                        jsonObject = Objects.requireNonNullElse(fetchSourceJson(source, index, indexedData.get(sourceIdField), id, isUpdatableIndex),
                                Json.object());

                        jsonObject.put(ID_KEY, id);
                        indexedData.computeIfAbsent(sourceIdField, idMapKey -> new HashMap<>()).putIfAbsent(id, sourceRecord);
                    }

                    if (Objects.isNull(jsonObject)) {
                        jsonObject = fetchSourceJson(orgId, source, index, sourceFieldHeaders, formattedValueContainer, timezoneInfo);
                    }

                    for (final FieldInfo importField : sourceFieldHeaders.getFields()) {

                        if (Objects.equals(sourceIdField, importField) || sourceRecordContainer.containsRelatedSource(importField)) {
                            continue;
                        }
                        final String fieldReference = registeredSource ? String.valueOf(importField.id()) : importField.aliasName();
                        final ImportFieldWrapper importFieldWrapper = importField.storeType().isTemporalType()
                                ? new ImportFieldWrapper(importField, timezoneInfo)
                                : new ImportFieldWrapper(importField);

                        if (distinctFields.contains(importField) || importField.storeType().isOwnerType()) {
                            final Object indexValue = formattedValueContainer.getFormattedValue(orgId, importFieldWrapper, sourceFieldHeaders);

                            indexDistinctValue(importField, indexValue, index);
                            jsonObject.put(fieldReference, indexValue);
                            continue;
                        }
                        final Object rowValue = formattedValueContainer.getFormattedValue(orgId, importFieldWrapper, sourceFieldHeaders);

                        if (Objects.nonNull(rowValue)) {

                            if (mergeConfigExists && config.exists(importField)) {
                                final Object indexedValue = indexSourceRecord(indexedData, formattedValueContainer, importField,
                                        jsonObject.opt(fieldReference), rowValue, sourceRecord);

                                jsonObject.put(fieldReference, indexedValue);
                            } else {
                                jsonObject.put(fieldReference, rowValue);
                            }
                        }
                    }

                    if (jsonObject.size() > 0) {
                        sourceRecord.put(source, jsonObject);
                    }
                } catch (Throwable throwable) {
                    sourceRecordContainer.increaseInvalidSourceCount(index);
                    errorTracker.track(index, throwable, String.format(DEFAULT_ERROR_MESSAGE, sourceName));
                }
            } else {
                sourceRecordContainer.increaseInvalidSourceCount(index);
                errorTracker.track(index, source, "i18n.source.is.disabled");
            }
        }
        return sourceRecord;
    }

    /**
     * Indexes the distinct values into the {@link SourceRecordContainer}
     *
     * @param importField             Represents the field to be indexed
     * @param indexValue              Represents the index value
     * @param index                   Represents the record index
     */
    private void indexDistinctValue(final FieldInfo importField, final Object indexValue, final Integer index) {

        if (indexValue instanceof final JsonArray jsonArray) {
            jsonArray.forEach(distinctValue -> sourceRecordContainer.indexDistinctField(importField, distinctValue, index));
        } else {
            sourceRecordContainer.indexDistinctField(importField, indexValue, index);
        }
    }

    /**
     * Checks if the provided {@link Record} contains values at any header index within the specified field headers
     *
     * @param record       Containing row values
     * @param fieldHeaders Which contains the {@link Collection} of {@link FieldHeader}
     * @return true if the given {@link Record} contains values at any header index within the collection of field headers
     */
    private boolean recordHasValue(final Record record, final Collection<FieldHeader> fieldHeaders) {

        for (final FieldHeader fieldHeader : fieldHeaders) {

            if (record.exists(fieldHeader.getHeaderIndex())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indexes the source records based on the specified import field and value, and loads them into the {@link SourceRecordContainer}
     *
     * @param indexedData             Contains the indexed data grouped by source
     * @param formattedValueContainer Contains the formatted values of the {@link Record}
     * @param importField             Holds information about the importable field
     * @param existingValue           Refers to the value already present for the field in the source JSON
     * @param rowValue                The value used to index the SourceRecord
     * @param sourceRecord            Contains a collection of source JsonObjects grouped by sources
     * @return The formatted indexed value
     */
    private Object indexSourceRecord(final Map<FieldInfo, Map<Object, SourceRecord>> indexedData,
                                     final FormattedValueContainer formattedValueContainer, final FieldInfo importField,
                                     final Object existingValue, final Object rowValue, final SourceRecord sourceRecord) {
        final Map<Object, SourceRecord> indexedSourceRecords = indexedData.computeIfAbsent(importField, importFieldKey -> new HashMap<>());
        final Function<Object, ?> sourceRecordIndexer = value -> {
            final Object indexValue = formattedValueContainer.getIndexValue(importField, value);

            if (Objects.isNull(indexValue)) {
                return null;
            }
            indexedSourceRecords.putIfAbsent(indexValue, sourceRecord);

            return indexValue;
        };

        if (rowValue instanceof final JsonArray array) {
            final Consumer<Consumer<Object>> arrayValueIndexer = action -> {

                for (int index = 0; index < array.size(); index++) {
                    action.accept(array.get(index));
                }
            };

            if (existingValue instanceof final Collection<?> existingCollection) {
                final Map<Object, Object> indexValueMap = new LinkedHashMap<>();

                for (final Object value : existingCollection) {
                    final Object indexValue = formattedValueContainer.getIndexValue(importField, value);

                    indexValueMap.putIfAbsent(indexValue, value);
                }

                arrayValueIndexer.accept(value -> {
                    final Object indexValue = sourceRecordIndexer.apply(value);

                    indexValueMap.putIfAbsent(indexValue, value);
                });
                return Json.array(indexValueMap.values());
            } else {
                arrayValueIndexer.accept(sourceRecordIndexer::apply);
                return array;
            }
        }
        sourceRecordIndexer.apply(rowValue);
        return rowValue;
    }

    /**
     * Fetches the existing source Json from the {@link SourceRecordContainer}
     *
     * @param orgId                   Represents the organization id
     * @param source                  Represents the source
     * @param index                   Represents the row index
     * @param sourceFieldHeaders      Which contains the field headers of the source
     * @param formattedValueContainer Contains the formatted values of the {@link Record}
     * @param timezoneInfo            Represents the timezone of the user
     * @return {@link JsonObject} with data if it already presents within the {@link SourceRecordContainer} otherwise return empty JSON
     */
    private JsonObject fetchSourceJson(final long orgId, final Source source, final int index, final SourceFieldHeaders sourceFieldHeaders,
                                       final FormattedValueContainer formattedValueContainer, final TimezoneInfo timezoneInfo) {
        final Map<FieldInfo, Map<Object, SourceRecord>> indexedData = sourceRecordContainer.indexData(source);
        final SortedMap<Integer, JsonObject> sourceJsonsByIndex = new TreeMap<>(Comparator.reverseOrder());
        final Collection<String> keys = new HashSet<>();
        final Collection<FieldInfo> fields = sourceFieldHeaders.getFields();
        final Map<String, FieldInfo> importFieldsByKeys = fields.stream().collect(Collectors.toMap(importField -> {
            final String aliasName = importField.aliasName();

            if (ID_KEY.equals(aliasName) || !Source.isPrimarySource(Source.get(importField.fieldSource().getName()))) {
                return aliasName;
            } else {
                return String.valueOf(importField.id());
            }
        }, Function.identity()));

        for (final FieldInfo importField : fields) {

            if (ID_KEY.equals(importField.aliasName())) {
                continue;
            }
            final Map<Object, SourceRecord> indexedValues = indexedData.get(importField);

            if (Objects.nonNull(indexedValues)) {
                final ImportFieldWrapper importFieldWrapper = importField.storeType().isTemporalType()
                        ? new ImportFieldWrapper(importField, timezoneInfo)
                        : new ImportFieldWrapper(importField);
                final Object formattedValue = formattedValueContainer.getFormattedValue(orgId, importFieldWrapper, sourceFieldHeaders);

                if (Objects.nonNull(formattedValue)) {
                    final Consumer<Object> sourceJsonCollector = value -> {
                        final Object indexValue = formattedValueContainer.getIndexValue(importField, value);

                        if (indexedValues.containsKey(indexValue)) {
                            final SourceRecord sourceRecord = indexedValues.get(indexValue);
                            final JsonObject jsonObject = sourceRecord.get(source);

                            if (Objects.nonNull(jsonObject) && !sourceJsonsByIndex.containsValue(jsonObject)) {
                                keys.addAll(jsonObject.keySet());
                                sourceJsonsByIndex.put(sourceRecord.index(), jsonObject);
                            }
                        }
                    };

                    if (formattedValue instanceof final JsonArray array) {
                        array.forEach(sourceJsonCollector);
                    } else {
                        sourceJsonCollector.accept(formattedValue);
                    }
                }
            }
        }

        if (sourceJsonsByIndex.isEmpty()) {
            return Json.object();
        }

        if (sourceJsonsByIndex.size() == 1) {
            final Integer primaryIndex = sourceJsonsByIndex.firstKey();

            sourceRecordContainer.mapMergedIndex(source, primaryIndex, index);
            return sourceJsonsByIndex.get(primaryIndex);
        } else {
            return mergeAndFetchSourceJson(source, index, sourceJsonsByIndex, keys, formattedValueContainer, importFieldsByKeys);
        }
    }

    /**
     * Merges the matched source JSONs and returns the primary source JSON.
     *
     * @param source                  Represents the source
     * @param index                   Represents the index
     * @param sourceJsonsByIndex      Collection of source JSONs grouped by index
     * @param keys                    Collection of keys gathered from the matched source JSONs
     * @param formattedValueContainer Container for formatted values of the {@link Record}
     * @param importFieldKeyMap       Map containing import fields grouped by keys
     * @return the source {@link JsonObject}
     */
    private JsonObject mergeAndFetchSourceJson(final Source source, final int index, final SortedMap<Integer, JsonObject> sourceJsonsByIndex,
                                               final Collection<String> keys, final FormattedValueContainer formattedValueContainer,
                                               final Map<String, FieldInfo> importFieldKeyMap) {
        final Integer primaryIndex = sourceJsonsByIndex.lastKey();
        final JsonObject primaryJson = sourceJsonsByIndex.get(primaryIndex);

        for (final Map.Entry<Integer, JsonObject> sourceJsonEntry : sourceJsonsByIndex.headMap(primaryIndex).entrySet()) {

            if (keys.isEmpty()) {
                break;
            }
            final Collection<String> removableKeys = new HashSet<>();
            final JsonObject sourceJson = sourceJsonEntry.getValue();

            for (final String key : keys) {

                if (sourceJson.containsKey(key)) {
                    removableKeys.add(key);
                    replaceFieldValue(primaryJson, key, sourceJson.get(key), formattedValueContainer, importFieldKeyMap);
                }
            }
            keys.removeAll(removableKeys);
            sourceRecordContainer.mapMergedIndex(source, primaryIndex, sourceJsonEntry.getKey());
        }
        sourceRecordContainer.mapMergedIndex(source, primaryIndex, index);
        return primaryJson;
    }

    /**
     * Replaces the value of the given key in the provided primary JSON with the specified value
     *
     * @param primaryJson             Represents the primary JSON
     * @param key                     The key whose values should be replaced in the primary JSON
     * @param value                   The value to replace with
     * @param formattedValueContainer Container for formatted values of the {@link Record}
     * @param importFieldsByKeys      Map containing import fields grouped by keys
     */
    private void replaceFieldValue(final JsonObject primaryJson, final String key, final Object value,
                                   final FormattedValueContainer formattedValueContainer, final Map<String, FieldInfo> importFieldsByKeys) {
        final Consumer<Collection<?>> collectionValueReplacer = collection -> {
            final Collection<Object> valueCollection = new HashSet<>(collection);

            if (primaryJson.containsKey(key)) {
                final Object currentValue = primaryJson.get(key);

                if (currentValue instanceof final Collection<?> collectionValue) {
                    valueCollection.addAll(collectionValue);
                } else if (currentValue instanceof final JsonArray arrayValue) {
                    valueCollection.addAll(arrayValue.toList());
                }
            }
            final Map<Object, Collection<Object>> valuesByIndexValue = new HashMap<>();

            for (final Object collectedValue : valueCollection) {
                final Object indexValue = formattedValueContainer.getIndexValue(importFieldsByKeys.get(key), collectedValue);

                if (Objects.nonNull(indexValue)) {
                    valuesByIndexValue.computeIfAbsent(indexValue, nonExistingIndexValue -> new HashSet<>()).add(collectedValue);
                }
            }
            final JsonArray valueArray = Json.array();

            for (final Collection<Object> collectionValue : valuesByIndexValue.values()) {
                collectionValue.stream().findFirst().ifPresent(valueArray::put);
            }
            primaryJson.put(key, valueArray);
        };

        if (value instanceof final JsonArray jsonArray) {
            collectionValueReplacer.accept(jsonArray.toList());
        } else if (value instanceof final Collection<?> collection) {
            collectionValueReplacer.accept(collection);
        } else {
            primaryJson.put(key, value);
        }
    }

    /**
     * Fetches the {@link JsonObject} of appropriate source from the {@link SourceRecordContainer}
     *
     * @param source        Represents the source
     * @param index         Represents the row index
     * @param indexedValues Represents the {@link Map} of indexed value and {@link SourceRecord} entries
     * @param value         Represents the index value
     * @return {@link JsonObject} of regarding source
     */
    private JsonObject fetchSourceJson(final Source source, final int index, final Map<Object, SourceRecord> indexedValues,
                                       final Object value, final boolean isUpdatableIndex) {

        if (Objects.nonNull(value)) {

            if (Objects.nonNull(indexedValues) && indexedValues.containsKey(value)) {
                final SourceRecord sourceRecord = indexedValues.get(value);

                if (Objects.nonNull(sourceRecord)) {
                    final int primaryIndex = sourceRecord.index();

                    if (isUpdatableIndex) {
                        sourceRecordContainer.mapUpdatedIndex(source, primaryIndex, index);
                    } else {
                        sourceRecordContainer.mapMergedIndex(source, primaryIndex, index);
                    }
                    return Objects.requireNonNull(indexedValues.putIfAbsent(value, sourceRecord)).get(source);
                }
            } else {

                if (isUpdatableIndex) {
                    sourceRecordContainer.addUpdatedIndex(source, index);
                } else {
                    sourceRecordContainer.addMergedIndex(source, index);
                }
            }
        }
        return null;
    }

    /**
     * Container that contains the formatted values of {@link Record} grouped by {@link FieldInfo}
     *
     * @author sundar
     * @version 1.0
     */
    private class FormattedValueContainer {

        private final Map<FieldInfo, Object> formattedValues;
        private final Record recordToFormat;

        private FormattedValueContainer(final Record recordToBeFormatted) {
            this.formattedValues = new HashMap<>();
            this.recordToFormat = recordToBeFormatted;
        }

        /**
         * Gets formatted value for provided import field
         *
         * @param orgId              Represents the organization id
         * @param importFieldWrapper Represents the import value format request
         * @param sourceFieldHeaders Which contains the header details of the fields of a source
         * @return the formatted value
         */
        private Object getFormattedValue(final long orgId, final ImportFieldWrapper importFieldWrapper, final SourceFieldHeaders sourceFieldHeaders) {
            final FieldInfo importField = importFieldWrapper.fieldInfo();

            return Objects.isNull(sourceFieldHeaders) ? null : (formattedValues.computeIfAbsent(importField, importFieldKey -> {

                if (sourceFieldHeaders.hasSubFields(importField)) {
                    return valueFormatter.format(orgId, importFieldWrapper, buildSubTypeValues(sourceFieldHeaders.getFieldHeaders(importField)));
                }
                final Optional<FieldHeader> optionalFieldHeader = sourceFieldHeaders.getFieldHeader(importField);

                return optionalFieldHeader.map(fieldHeader -> valueFormatter.format(orgId, importFieldWrapper,
                        recordToFormat.get(fieldHeader.getHeaderIndex()))).orElse(null);
            }));
        }

        /**
         * Builds the subtype values contained {@link JsonObject}
         *
         * @param fieldHeaders Which contains the subfield and header index mappings
         * @return {@link JsonObject}
         */
        private JsonObject buildSubTypeValues(final Collection<FieldHeader> fieldHeaders) {
            final JsonObject subFieldValues = Json.object();

            for (final FieldHeader fieldHeader : fieldHeaders) {
                fieldHeader.optSubFieldId().ifPresent(subFieldId -> subFieldValues.put(String.valueOf(subFieldId),
                        recordToFormat.get(fieldHeader.getHeaderIndex())));
            }
            return subFieldValues.size() == 0 ? null : subFieldValues;
        }

        /**
         * Gets the indexed value of provided source and {@link FieldInfo}
         *
         * @param importField Which contains the importable field info
         * @param value       To be converted for indexing
         * @return the index value
         */
        private Object getIndexValue(final FieldInfo importField, final Object value) {

            if (value instanceof final Map<?, ?> mapValue) {
                return getIndexValue(importField, mapValue);
            } else if (value instanceof final JsonObject jsonValue) {
                return getIndexValue(importField, jsonValue.toMap());
            }
            return value;
        }

        /**
         * Gets the index value of email, phone, address, company and contact values from provide {@link Map}
         *
         * @param importField Which contains the importField info
         * @param mapValue    Which contains the sub-field values
         * @return needed values from the {@link Map}
         */
        private Object getIndexValue(final FieldInfo importField, final Map<?, ?> mapValue) {

            if (Objects.nonNull(importField)) {
                final StoreType storeType = importField.storeType();

                return switch (storeType) {
                    case EMAIL, EMAILS ->
                            mapValue.get(fieldTypeService.groupStoreSubTypeIds(storeType).get(StoreSubType.EMAIL_VALUE));
                    case PHONE, PHONES ->
                            mapValue.get(fieldTypeService.groupStoreSubTypeIds(storeType).get(StoreSubType.PHONE_NUMBER));
                    case ADDRESS ->
                            mapValue.get(fieldTypeService.groupStoreSubTypeIds(storeType).get(StoreSubType.ADDRESS_LINE1));
                    case COMPANY, CONTACT -> {
                        final Object idValue = mapValue.get(ID_KEY);

                        yield Objects.isNull(idValue) ? mapValue.get(String.valueOf(importField.id())) : idValue;
                    }
                    default -> null;
                };
            }
            return null;
        }
    }
}