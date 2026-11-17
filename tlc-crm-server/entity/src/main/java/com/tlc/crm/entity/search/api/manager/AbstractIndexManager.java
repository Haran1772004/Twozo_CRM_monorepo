package com.tlc.crm.entity.search.api.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.search.IndexManager;

import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.service.SearchIndexService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.search.api.model.IndexField;
import com.tlc.crm.entity.search.api.model.PresetConfig;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.entity.service.SearchFieldService;

import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.search.SearchService;
import com.tlc.search.model.AnalysisComponent;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FieldProperty;
import com.tlc.search.model.FilterClause;
import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.definition.IndexCreateRequest;
import com.tlc.search.request.index.definition.IndexExistenceRequest;
import com.tlc.search.request.index.settings.MappingsUpdateRequest;
import com.tlc.search.request.index.settings.SettingsUpdateRequest;
import com.tlc.search.service.IndexService;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.provider.SearchServiceProvider;
import com.tlc.search.service.WritableDocumentService;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;

import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Provides an abstract way to manage the search related actions of entities.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractIndexManager<T extends TlcModel> implements IndexManager<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndexManager.class);

    protected final FieldPropertiesService fieldPropertiesService;
    protected final SearchFieldService searchFieldService;
    protected final SearchService service;

    private final SearchIndexService searchIndexService;
    private final WritableDocumentService writableDocumentService;
    private final IndexService indexService;
    private final PropertyDescriptorService propertyDescriptorService;
    private final FieldService fieldService;

    protected AbstractIndexManager() {
        service = SearchServiceProvider.get();
        writableDocumentService = service.getWritableService();
        indexService = service.indexService();
        searchIndexService = SearchIndexService.getInstance();
        searchFieldService = SearchFieldService.getInstance();
        propertyDescriptorService = PropertyDescriptorService.getInstance();
        fieldPropertiesService = FieldPropertiesService.getInstance();
        fieldService = FieldService.getInstance();
    }

    /**
     * <p>
     * Provides the service to document the entities. It helps to add, update and delete the entities.
     * </p>
     *
     * @return {@link WritableDocumentService} to document the entities.
     */
    public WritableDocumentService writableService() {
        return writableDocumentService;
    }

    /**
     * <p>
     * Provides the service to read the documented entities. It helps to search the available entities.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @return {@link ReadableDocumentService} to search the entities.
     */
    public ReadableDocumentService readableService(final Long orgId) {
        return service.getReadableService(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Collection<T> models) {
        final String indexName = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(indexName);

        for (final T model : models) {
            if (!indexService.exists(IndexExistenceRequest.get(indexName))) {
                final IndexCreateRequest createRequest = IndexCreateRequest.get(indexName);
                final Index index = searchIndexService.getByName(indexName);
                final AnalysisComponent analysisComponent = index.analysisComponent();

                createRequest.setSettingsUpdateRequest(buildSettingsUpdateRequest(analysisComponent, indexName));
                createRequest.setMappingUpdateRequest(buildMappingUpdateRequest(indexName, model));
                indexService.create(createRequest);
            }
            final UpsertRequest upsertRequest = UpsertRequest.get(String.valueOf(model.id()), indexName);

            upsertRequest.setDoc(constructDoc(model.orgId(), model, indexName));
            bulkWriteRequest.addInsertRequest(upsertRequest);
        }
        writableDocumentService.performBulkOperations(bulkWriteRequest);
        writeRelatedIndexes(models, Action.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Collection<T> models) {
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

        for (final T model : models) {
            final Long modelId = model.id();
            final String modelIdAsString = String.valueOf(modelId);
            final UpsertRequest updateRequest = UpsertRequest.get(modelIdAsString, index);
            final JsonObject indexJson = get(model.orgId(), modelId, null, null);

            if (Objects.nonNull(indexJson)) {
                final Map<String, Object> indexDoc = 0 != indexJson.size() ? indexJson.containsKey(index) ? indexJson.getJsonArray(index).toList().stream().findAny().
                        map(map -> (Map<String, Object>) map).orElseGet(Map::of) : Map.of() : Map.of();
                final Map<String, Object> doc = constructDoc(model.orgId(), model, index);

                if (!indexDoc.isEmpty()) {
                    updateRelatedFields(indexDoc, doc);
                    updateRequest.setDoc(model instanceof Entity ? indexDoc : doc);
                    bulkWriteRequest.addRemoveRequest(RemoveRequest.get(modelIdAsString, index));
                    bulkWriteRequest.addInsertRequest(updateRequest);
                }
            }
        }
        writableDocumentService.performBulkOperations(bulkWriteRequest);
        writeRelatedIndexes(models, Action.UPDATE);
    }

    /**
     * {@inheritDoc}
     * NOTE - To be only used for alteration case i.e., value change, if we are using this fn for
     * cases like contact/company pUpdate use update fn above.
     */
    @Override
    public void partialUpdate(final Collection<T> models) {
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

        for (final T model : models) {
            final Long orgId = model.orgId();
            final String modelIdAsString = model.id().toString();

            if (service.getReadableService(orgId).exists(modelIdAsString, index)) {
                final UpsertRequest updateRequest = UpsertRequest.get(modelIdAsString, index);

                updateRequest.setDoc(constructDoc(orgId, model, index));
                bulkWriteRequest.addUpdateRequest(updateRequest);
            }
        }
        writableDocumentService.performBulkOperations(bulkWriteRequest);
        writeRelatedIndexes(models, Action.PARTIAL_UPDATE);
    }

    /**
     * <p>
     * Updates the documented entities partially.
     * </p>
     *
     * @param orgId Organization's id.
     * @param id    Model id.
     * @param doc   Field name & it's value pair.
     */
    public void partialUpdate(final Long orgId, final Long id, final Map<String, Object> doc) {
        final FilterClause filter = new FilterClause(new Criteria(id, Comparator.EQUAL, CommonSearchFields.ID));
        final JsonObject fetchedJson = this.get(orgId, null, List.of(CommonSearchFields.ID), filter);

        if (!Json.isEmpty(fetchedJson)) {
            final String index = getSearchIndex().getName();
            final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);
            final JsonArray activityArray = fetchedJson.getJsonArray(index);
            final int size = activityArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final UpsertRequest updateRequest = UpsertRequest.get(String.valueOf(id), index);

                updateRequest.setDoc(doc);
                bulkWriteRequest.addUpdateRequest(updateRequest);
            }
            writableService().performBulkOperations(bulkWriteRequest);
        }
    }

    /**
     * <p>
     * Updates follower ids.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param id          Model id.
     * @param followerIds Follower ids.
     */
    public void updateFollowerIds(final Long orgId, final Long id, final Collection<Long> followerIds) {
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);
        final String modelIdAsString = id.toString();

        if (service.getReadableService(orgId).exists(modelIdAsString, index)) {
            final UpsertRequest updateRequest = UpsertRequest.get(modelIdAsString, index);

            if (followerIds.isEmpty()) {
                updateRequest.addFieldToRemove(CommonSearchFields.FOLLOWER_IDS);
            } else {
                updateRequest.setDoc(Json.object().put(CommonSearchFields.FOLLOWER_IDS, followerIds));
            }
            bulkWriteRequest.addUpdateRequest(updateRequest);
            writableDocumentService.performBulkOperations(bulkWriteRequest);
            writeRelatedIndexes(orgId, id, followerIds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject get(final Long orgId, final Long id, final Collection<String> includedFields, final FilterClause filterClause) {
        final String index = getSearchIndex().getName();
        final ReadableDocumentService readableDocumentService = readableService(orgId);
        final SearchRequest searchRequest = new SearchRequest();

        searchRequest.setFilter(Objects.isNull(filterClause) ?
                new FilterClause(new Criteria(id, Comparator.EQUAL, CommonSearchFields.ID)) : filterClause);

        if (Objects.nonNull(includedFields) && !includedFields.isEmpty()) {
            searchRequest.includeFields(includedFields);
        }

        return readableDocumentService.search(SingleIndexSearchRequest.get(index, searchRequest));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Collection<T> models) {
        final String index = getSearchIndex().getName();
        final BulkWriteRequest bulkWriteRequest = BulkWriteRequest.get(index);

        for (final T model : models) {
            bulkWriteRequest.addRemoveRequest(RemoveRequest.get(model.id().toString(), index));
        }
        writableDocumentService.performBulkOperations(bulkWriteRequest);
        writeRelatedIndexes(models, Action.DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge(final T retainedModel, final Collection<Long> duplicateIds) {
    }

    /**
     * <p>
     * Gets the {@link SearchIndex}.
     * </p>
     *
     * @return {@link SearchIndex}.
     */
    public abstract SearchIndex getSearchIndex();

    /**
     * <p>
     * Constructs the document for the given entity.
     * </p>
     *
     * @param model Active entity (ex: Contact, Deal, Company, Product, Note and Activity).
     * @param index Index's name.
     * @return Constructed document.
     */
    public final Map<String, Object> constructDoc(final Long orgId, final T model, final String index) {
        final Map<String, Object> doc = new HashMap<>();

        doc.put(CommonSearchFields.ORG_ID, orgId);
        loadValuesIntoModel(model);
        loadCustomFieldsIntoDoc(model, doc);
        loadFieldValueIntoDoc(model, searchFieldService.groupFieldsByEntity(index), model.getClass().getSimpleName(), doc);
        loadAdditionalFieldsIntoDoc(orgId, model, doc);
        loadPermissionRelatedFieldsInDoc(orgId, model, doc);

        return doc;
    }

    /**
     * <p>
     *  Loads additional fields into doc.
     * </p>
     *
     * @param orgId The organization's ID.
     * @param model The model containing activity data.
     * @param doc   The map to populate with activity type and icon info.
     */
    protected void loadAdditionalFieldsIntoDoc(final Long orgId, final T model, final Map<String, Object> doc) {
    }

    /**
     * <p>
     * Constructs the document for the given entity.
     * </p>
     *
     * @param model       Active entity (ex: Contact, Deal, Company, Product, Note and Activity).
     * @param parentClass Parent's class.
     * @param index       Index's name.
     * @return Constructed document.
     */
    public final Map<String, Object> constructEntity(final Long orgId, final TlcModel model,
                                                     final Class<?> parentClass, final String index) {
        final Map<String, Object> doc = new HashMap<>();

        doc.put(CommonSearchFields.ORG_ID, orgId);
        loadFieldValueIntoDoc((T) model, searchFieldService.groupFieldsByEntity(index), parentClass.getSimpleName(), doc);
        loadFollowerIds(orgId, model.getClass().getSimpleName(), doc);

        return doc;
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param model {@link TlcModel}.
     * @param doc   Holds the entity data to be indexed.
     */
    protected abstract void loadPermissionRelatedFieldsInDoc(final Long orgId, final T model, final Map<String, Object> doc);

    /**
     * <p>
     * Loads field value to the model.
     * </p>
     *
     * @param t Represents {@link TlcModel}.
     */
    protected void loadValuesIntoModel(final T t) {
    }

    /**
     * <p>
     * Fetches the field value from the model.
     * </p>
     *
     * @param fieldName Name of the field.
     * @param tlcModel  Represents {@link Collection} of {@link TlcModel}.
     * @return Value of the field.
     */
    protected Collection<Map.Entry<String, Object>> fetchValue(final String fieldName, final T tlcModel) {
        return null;
    }

    /**
     * <p>
     * Updates related fields.
     * </p>
     *
     * @param indexDoc       A map holding values from Respective Index.
     * @param constructedDoc A map holding constructed values.
     */
    protected void updateRelatedFields(final Map<String, Object> indexDoc, final Map<String, Object> constructedDoc) {
    }

    /**
     * <p>
     * Writes the related entities index to the active entities.
     * </p>
     *
     * @param models Represents {@link Collection} of {@link TlcModel}.
     * @param action Represents {@link Action}.
     */
    protected void writeRelatedIndexes(final Collection<T> models, final Action action) {
    }

    /**
     * <p>
     * Writes follower ids in related indexes.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param id          Model id.
     * @param followerIds Follower ids.
     */
    protected void writeRelatedIndexes(final Long orgId, final Long id, final Collection<Long> followerIds) {
    }

    /**
     * <p>
     * Load follower ids in the doc.
     * </p>
     *
     * @param orgId     An organization's id.
     * @param className {@link TlcModel} name.
     * @param doc       Holds the entity data to be indexed.
     */
    protected void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc) {
    }

    /**
     * <p>
     * Loads custom fields into the doc.
     * </p>
     *
     * @param model Represents {@link Collection} of {@link TlcModel}.
     * @param doc   Holds the entity data to be indexed.
     */
    private void loadCustomFieldsIntoDoc(final T model, final Map<String, Object> doc) {

        if (model instanceof final Entity entity) {
            final ValueHolder valueHolder = entity.getValueHolder();

            if (Objects.nonNull(valueHolder)) {
                final Collection<Object> customFields = new ArrayList<>();
                final Map<Long, Object> valueHolderAsMap = valueHolder.toMap();

                for (final Map.Entry<Long, Object> valueHolderEntry : valueHolderAsMap.entrySet()) {
                    final Object fieldValue = valueHolderEntry.getValue();

                    if (Objects.nonNull(fieldValue)) {
                        final Map<String, Object> customFieldMap = new HashMap<>();
                        final FieldInfo fieldInfo = fieldService.getField(model.orgId(), valueHolderEntry.getKey());
                        final StoreType storeType = fieldInfo.storeType();

                        if (!fieldInfo.presetConfig().isDefaultField()) {
                            switch (storeType) {
                                case SINGLE_STRING, TEXT, IDENTIFIER -> {
                                    final String value = (String) fieldValue;

                                    if (!value.isBlank()) {
                                        customFieldMap.put(CommonSearchFields.NAME, fieldValue);
                                        customFields.add(customFieldMap);
                                    }
                                }
                                case ADDRESS -> {
                                    if (fieldValue instanceof final Address address) {
                                        final StringBuilder addressBuilder = new StringBuilder();

                                        final Consumer<String> addressBuilderConsumer = (addressValue) -> {
                                            if (Objects.nonNull(addressValue) && !addressValue.isBlank()) {
                                                if (!addressBuilder.isEmpty()) {
                                                    addressBuilder.append(", ");
                                                }
                                                addressBuilder.append(addressValue);
                                            }
                                        };
                                        addressBuilderConsumer.accept(address.getAddressLine1());
                                        addressBuilderConsumer.accept(address.getAddressLine2());
                                        addressBuilderConsumer.accept(address.getCity());
                                        addressBuilderConsumer.accept(address.getState());
                                        addressBuilderConsumer.accept(address.getCountryAsString());
                                        addressBuilderConsumer.accept(address.getZipcode());

                                        if (!addressBuilder.isEmpty()) {
                                            customFieldMap.put(CommonSearchFields.NAME, addressBuilder.toString());
                                            customFields.add(customFieldMap);
                                        }
                                    }
                                }
                                case SINGLE_NUMBER -> {
                                    customFieldMap.put(CommonSearchFields.NAME, fieldValue.toString());
                                    customFields.add(customFieldMap);
                                }
                                case MONETARY -> {
                                    if (fieldValue instanceof final Monetary monetary) {
                                        customFieldMap.put(CommonSearchFields.NAME, monetary.getValue().toPlainString());
                                        customFields.add(customFieldMap);
                                    }
                                }
                            }
                        }
                    }
                }

                if (NumericalConstant.ZERO < customFields.size()) {
                    doc.put(CommonSearchFields.CUSTOM_FIELDS, customFields);
                }
            }
        }
    }

    /**
     * <p>
     * Fetches the value of the field from the given model.
     * </p>
     *
     * @param fieldName      Name of the field.
     * @param model          Holds the details of the model which implements the {@link TlcModel}.
     * @param readMethodName Refers the read method name of the field.
     * @return Value of the required field.
     */
    private Object fetchValueFromModel(final String fieldName, final TlcModel model, final String readMethodName) {
        final Class<? extends TlcModel> modelClass = model.getClass();

        try {
            final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(fieldName, modelClass, readMethodName);

            return propertyDescriptor.getReadMethod().invoke(model);
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOGGER.debug(String.format("Failed to fetch value from model. Model - %s, Field - %s", modelClass.getName(), fieldName), e);
            return null;
        }
    }

    /**
     * <p>
     * Loads the field value into the given document. (Note: Only configured fields to be indexed)
     * </p>
     *
     * @param model        Holds the details of the model which implements the {@link TlcModel}
     * @param fields       Grouped index fields by entity name
     * @param parentEntity Represents the parent entity
     * @param doc          Holds the details of the entity
     */
    private void loadFieldValueIntoDoc(final T model, final Map<String, Map<String, Map<String, IndexField>>> fields,
                                       final String parentEntity, final Map<String, Object> doc) {
        final Class<? extends TlcModel> modelClass = model.getClass();

        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(modelClass, SearchField.class)) {
            final Map<String, Map<String, IndexField>> entityFields = fields.get(parentEntity);

            if (Objects.nonNull(entityFields)) {
                final String entityName = modelClass.getSimpleName();
                final Map<String, IndexField> indexFields = entityFields.get(entityName);

                if (Objects.nonNull(indexFields)) {
                    final SearchField searchField = field.getAnnotation(SearchField.class);
                    final String name = searchField.name();
                    final IndexField indexField = indexFields.get(name);
                    final Object value = fetchValueFromModel(field.getName(), model, searchField.readMethodName());

                    if (Objects.isNull(indexField)) {
                        if (value instanceof final Territory territory) {
                            addFieldValueIntoDoc(CommonSearchFields.TERRITORY_ID, territory.id(), false, false, doc);
                        }
                    } else {
                        final PresetConfig indexConfig = indexField.presetConfig();
                        final boolean hasWrapper = indexConfig.hasWrapper();
                        final boolean isPrimaryEntityOnlyNeeded = indexConfig.isPrimaryEntityOnlyNeeded();
                        final boolean isArrayType = indexConfig.isArrayType();

                        if (indexConfig.hasDependency()) {
                            loadValue(name, entityName, model, hasWrapper, isArrayType, isPrimaryEntityOnlyNeeded, fields, doc);
                        } else if (Objects.nonNull(value)) {
                            addFieldValueIntoDoc(name, convertValue(isPrimaryEntityOnlyNeeded, entityName, value, fields), hasWrapper, isArrayType, doc);
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Adds the field value into the given document.
     * </p>
     *
     * @param fieldName   Represents the field name.
     * @param value       Represents the value of the field.
     * @param hasWrapper  A flag that indicates whether the field is wrapped or not.
     * @param isArrayType A flag that indicates whether the field is array type or not.
     * @param doc         Holds the entity data to be indexed.
     */
    private void addFieldValueIntoDoc(final String fieldName, final Object value, final boolean hasWrapper,
                                      final boolean isArrayType, final Map<String, Object> doc) {
        if (hasWrapper && value instanceof Map) {
            final Map<String, ?> values = (Map<String, ?>) value;

            for (final Object entityMap : values.values()) {
                doc.putAll(isArrayType ? (Map<String, ?>) entityMap : values);
            }
        } else if (hasWrapper && value instanceof Collection) {
            ((Collection<Map<String, ?>>) value).forEach(doc::putAll);
        } else {
            doc.put(fieldName, value);
        }
    }

    /**
     * <p>
     * Fetches the field value and loads it into the given document.
     * </p>
     *
     * @param fieldName                 Name of the field.
     * @param tlcModel                  Holds the details of the model which implements the {@link TlcModel}.
     * @param hasWrapper                A flag that indicates whether the field is wrapped or not.
     * @param isArrayType               A flag that indicates whether the field is of array type.
     * @param isPrimaryEntityOnlyNeeded A flag indicating whether only primary entity is required from Collection.
     * @param fields                    Grouped index fields by entity name.
     * @param doc                       Holds the details of the entity.
     */
    private void loadValue(final String fieldName, final String parentEntity, final T tlcModel, final boolean hasWrapper,
                           final boolean isArrayType, final boolean isPrimaryEntityOnlyNeeded,
                           final Map<String, Map<String, Map<String, IndexField>>> fields, final Map<String, Object> doc) {
        final Collection<Map.Entry<String, Object>> values = fetchValue(fieldName, tlcModel);

        if (Objects.nonNull(values)) {
            for (final Map.Entry<String, Object> value : values) {
                addFieldValueIntoDoc(value.getKey(), convertValue(isPrimaryEntityOnlyNeeded, parentEntity, value.getValue(), fields), hasWrapper, isArrayType, doc);
            }
        }
    }

    /**
     * <p>
     * Identifies the field type and convert it into document.
     * </p>
     *
     * @param isPrimaryEntityOnlyRequired A flag indicating whether only primary entity is required from Collection.
     * @param value                       Value of the field.
     * @param fields                      Grouped index fields by entity name.
     * @return Converted value.
     */
    private Object convertValue(final boolean isPrimaryEntityOnlyRequired, final String parentEntity, final Object value,
                                final Map<String, Map<String, Map<String, IndexField>>> fields) {
        if (value instanceof TlcModel tlcModel) {
            final Map<String, Object> entityDoc = new HashMap<>();

            loadFieldValueIntoDoc((T) tlcModel, fields, parentEntity, entityDoc);

            return entityDoc;
        } else if (value instanceof final Collection<?> values) {
            if (values.isEmpty()) {
                return null;
            } else if (isPrimaryEntityOnlyRequired) {
                final Map<String, Object> entityDoc = new HashMap<>();

                loadFieldValueIntoDoc((T) values.stream().findFirst().get(), fields, parentEntity, entityDoc);

                return entityDoc;
            } else {
                final Collection<Map<String, Object>> entities = new ArrayList<>();

                for (final Object fieldValue : values) {
                    if (fieldValue instanceof final TlcModel tlcModel) {
                        final Map<String, Object> entityDoc = new HashMap<>();

                        loadFieldValueIntoDoc((T) tlcModel, fields, parentEntity, entityDoc);
                        entities.add(entityDoc);
                    }
                }
                return entities.isEmpty() ? values.isEmpty() ? null : values : entities;
            }
        } else if (value instanceof final Temporal temporal) {
            return temporal.getTimeStamp();
        }

        return value;
    }

    /**
     * <p>
     * Builds the {@link MappingsUpdateRequest} for the given index.
     * </p>
     *
     * @param index Represents the index name.
     * @param model Active entity (ex: Contact, Deal, Company, Product, Note and Activity).
     * @return {@link MappingsUpdateRequest} of the given index.
     */
    private MappingsUpdateRequest buildMappingUpdateRequest(final String index, final T model) {
        final MappingsUpdateRequest mappingsUpdateRequest = MappingsUpdateRequest.get(index);
        final Map<String, Map<String, Map<String, IndexField>>> fields = searchFieldService.groupFieldsByEntity(index);
        final String modelName = model.getClass().getSimpleName();

        mappingsUpdateRequest.setProperties(constructFieldProperties(modelName, modelName, fields));

        return mappingsUpdateRequest;
    }

    /**
     * <p>
     * Builds the {@link SettingsUpdateRequest} for the given index.
     * </p>
     *
     * @param index Represents the index name.
     * @return {@link SettingsUpdateRequest} of the given index.
     */
    private SettingsUpdateRequest buildSettingsUpdateRequest(final AnalysisComponent analysisComponent,
                                                             final String index) {
        final SettingsUpdateRequest settingsUpdateRequest = SettingsUpdateRequest.get(index);

        settingsUpdateRequest.setCustomAnalysisRequired(Objects.nonNull(analysisComponent));
        settingsUpdateRequest.setAnalyzerComponents(analysisComponent);

        return settingsUpdateRequest;
    }

    /**
     * <p>
     * Constructs the collection of {@link FieldProperty} for the fields of the given model class.
     * </p>
     *
     * @param parentEntity Represents the parent entity name of the current entity.
     * @param entity       Represents the entity name of the field.
     * @param fields       Grouped index fields by entity name.
     * @return Collection of {@link FieldProperty}.
     */
    private Collection<FieldProperty> constructFieldProperties(final String parentEntity, final String entity,
                                                               final Map<String, Map<String, Map<String, IndexField>>> fields) {
        final Map<String, Map<String, IndexField>> indexFields = fields.get(parentEntity);
        final Collection<FieldProperty> fieldProperties = new ArrayList<>();

        if (Objects.nonNull(indexFields)) {
            final Map<String, IndexField> fieldMap = indexFields.get(entity);

            if (Objects.nonNull(fieldMap)) {
                for (final Map.Entry<String, IndexField> fieldEntry : fieldMap.entrySet()) {
                    final IndexField indexField = fieldEntry.getValue();
                    final FieldProperty fieldProperty = new FieldProperty();
                    final PresetConfig presetConfig = indexField.presetConfig();
                    final FieldProperty.Type type = indexField.type();
                    final boolean isObjectType = FieldProperty.Type.OBJECT == type || FieldProperty.Type.ARRAY == type;

                    if (presetConfig.hasDependency()) {
                        continue;
                    } else if (presetConfig.hasWrapper()) {
                        fieldProperties.addAll(constructFieldProperties(entity, indexField.modelType().name(), fields));
                        continue;
                    } else if (isObjectType) {
                        final Collection<FieldProperty> properties = constructFieldProperties(entity, indexField.modelType().name(), fields);

                        if (!properties.isEmpty()) {
                            fieldProperty.setProperties(properties);
                        }
                    }
                    fieldProperty.setAnalyzerName(presetConfig.analyzerName());
                    fieldProperty.setType(type);
                    fieldProperty.setFieldName(fieldEntry.getKey());
                    fieldProperties.add(fieldProperty);
                }
            }
        }
        return fieldProperties;
    }
}