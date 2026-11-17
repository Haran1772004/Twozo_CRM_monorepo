package com.tlc.search.internal.elasticsearch.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateRangeProperty;
import co.elastic.clients.elasticsearch._types.mapping.DoubleNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.FieldAliasProperty;
import co.elastic.clients.elasticsearch._types.mapping.FloatNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.LongNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.ObjectProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.mapping.WildcardProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.ConstantKeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.indices.CloneIndexRequest;
import co.elastic.clients.elasticsearch.indices.CloseIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexState;
import co.elastic.clients.elasticsearch.indices.OpenRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.SoftDeletes;
import co.elastic.clients.util.ObjectBuilder;

import com.tlc.commons.code.ErrorCode;

import com.tlc.search.internal.elasticsearch.AbstractElasticSearchService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.FieldProperty;
import com.tlc.search.request.index.settings.MappingsUpdateRequest;
import com.tlc.search.request.index.settings.SettingsUpdateRequest;
import com.tlc.search.service.IndexService;
import com.tlc.search.request.index.settings.IndexCloneRequest;
import com.tlc.search.request.index.definition.IndexCreateRequest;
import com.tlc.search.request.index.definition.IndexExistenceRequest;
import com.tlc.search.request.index.definition.IndexRemovalRequest;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the index services to execute index related actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ESIndexService extends AbstractElasticSearchService implements IndexService {

    public ESIndexService(final ElasticsearchClient client) {
        super(client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final IndexCreateRequest createRequest) {
        final CreateIndexRequest.Builder requestBuilder = new CreateIndexRequest.Builder().index(createRequest.getIndexName());
        final SettingsUpdateRequest settingsUpdateRequest = createRequest.getSettingsUpdateRequest();
        final MappingsUpdateRequest mappingsUpdateRequest = createRequest.getMappingUpdateRequest();

        if (Objects.nonNull(settingsUpdateRequest)) {
            requestBuilder.settings(buildSettings(settingsUpdateRequest));
        }

        if (Objects.nonNull(mappingsUpdateRequest)) {
            requestBuilder.mappings(new TypeMapping.Builder().dynamic(DynamicMapping.False).
                    properties(buildProperties(mappingsUpdateRequest.getProperties())).build());
        }

        try {
            client.indices().create(requestBuilder.build());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.INDEX_CREATION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(final IndexRemovalRequest removeRequest) {
        try {
            return client.indices().delete(new DeleteIndexRequest.Builder().ignoreUnavailable(true)
                    .index(removeRequest.getIndexName()).build()).acknowledged();
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.INDEX_DELETION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final IndexExistenceRequest existsRequest) {
        try {
            return client.indices().exists(new ExistsRequest.Builder().index(existsRequest.getIndexName()).build()).value();
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.UNABLE_TO_CHECK_EXISTS, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clone(final IndexCloneRequest cloneRequest) {
        try {
            client.indices().clone(new CloneIndexRequest.Builder().index(cloneRequest.getIndexName()).target(cloneRequest.getTargetIndexName()).build());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.INDEX_CLONING_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getAllIndices() {
        try {
            return client.indices().getSettings().result().keySet();
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.FAILED_TO_FETCH_INDICES, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSettings(final SettingsUpdateRequest settingsUpdateRequest) {
        final String index = settingsUpdateRequest.getIndexName();
        final ElasticsearchIndicesClient indices = client.indices();
        final PutIndicesSettingsRequest.Builder requestBuilder = new PutIndicesSettingsRequest.Builder().index(index);

        requestBuilder.settings(buildSettings(settingsUpdateRequest));

        try {
            indices.close(new CloseIndexRequest.Builder().index(index).build());
            indices.putSettings(requestBuilder.build());
            indices.open(new OpenRequest.Builder().index(index).build());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.INDEX_SETTINGS_UPDATE_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFieldMappings(final MappingsUpdateRequest mappingsUpdateRequest) {
        final PutMappingRequest.Builder requestBuilder = new PutMappingRequest.Builder().index(mappingsUpdateRequest.getIndexName());
        final ElasticsearchIndicesClient indices = client.indices();

        requestBuilder.ignoreUnavailable(mappingsUpdateRequest.isIgnoreUnavailability()).writeIndexOnly(mappingsUpdateRequest.isWritableIndex());
        requestBuilder.dynamic(DynamicMapping.False);
        requestBuilder.properties(buildProperties(mappingsUpdateRequest.getProperties()));

        try {
            indices.putMapping(requestBuilder.build());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.FIELD_MAPPINGS_UPDATE_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<com.tlc.search.model.IndexSettings> getSettings(final List<String> indexes) {
        try {
            final ElasticsearchIndicesClient indices = client.indices();

            return convertSettingsResponse(indices.getSettings(new GetIndicesSettingsRequest.Builder()
                    .index(indexes).build()).result());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.FAILED_TO_FETCH_INDEX_SETTINGS, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count(final String index) {
        try {
            return client.count(new co.elastic.clients.elasticsearch.core.CountRequest.Builder()
                    .index(index)
                    .build()).count();
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.UNABLE_TO_FETCH_COUNT, e);
        }
    }

    /**
     * <p>
     * Converts the settings response into collection of {@link com.tlc.search.model.IndexSettings}.
     * </p>
     *
     * @param settingsResponse Response of index settings.
     * @return Converted settings response.
     */
    private Collection<com.tlc.search.model.IndexSettings> convertSettingsResponse(final Map<String, IndexState> settingsResponse) {
        final Collection<com.tlc.search.model.IndexSettings> indexSettingList = new ArrayList<>();

        settingsResponse.forEach(((index, indexState) -> {
            final IndexSettings settings = indexState.settings();

            if (Objects.nonNull(settings)) {
                final IndexSettings esIndexSettings = settings.index();

                if (Objects.nonNull(esIndexSettings)) {
                    final com.tlc.search.model.IndexSettings indexSettings = new com.tlc.search.model.IndexSettings();
                    final SoftDeletes softDeletes = esIndexSettings.softDeletes();

                    indexSettings.setNumberOfShards(esIndexSettings.numberOfShards());
                    indexSettings.setNumberOfReplicas(esIndexSettings.numberOfReplicas());
                    indexSettings.setNumberOfRoutingShards(esIndexSettings.numberOfRoutingShards());
                    indexSettings.setSoftDeleteEnabled(Objects.nonNull(softDeletes) && Boolean.TRUE.equals(softDeletes.enabled()));
                    indexSettingList.add(indexSettings);
                }
            }
        }));

        return indexSettingList;
    }

    /**
     * <p>
     * Builds the properties for field mappings.
     * </p>
     *
     * @param mappingProperties Collection of {@link FieldProperty}.
     * @return Map of field mapping properties.
     */
    private Map<String, Property> buildProperties(final Collection<FieldProperty> mappingProperties) {
        final Map<String, Property> fieldProperties = new HashMap<>();

        for (final FieldProperty fieldProperty : mappingProperties) {
            final Collection<FieldProperty> properties = fieldProperty.getProperties();
            final Property.Builder propertyBuilder = new Property.Builder();
            final String fieldName = fieldProperty.getFieldName();

            final ObjectBuilder<Property> propertyObjectBuilder = switch (fieldProperty.getType()) {
                case OBJECT, ARRAY -> {
                    final ObjectProperty.Builder builder = new ObjectProperty.Builder();

                    builder.dynamic(DynamicMapping.False);

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.object(builder.build());
                }
                case NESTED_PROPERTY -> {
                    final NestedProperty.Builder builder = new NestedProperty.Builder();

                    builder.dynamic(DynamicMapping.False);

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.nested(builder.build());
                }
                case TEXT -> {
                    final TextProperty.Builder builder = new TextProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.text(builder.index(true).build());
                }
                case CONSTANT -> {
                    final ConstantKeywordProperty.Builder builder = new ConstantKeywordProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.constantKeyword(builder.build());
                }
                case LONG -> {
                    final LongNumberProperty.Builder builder = new LongNumberProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.long_(builder.index(true).build());
                }
                case INTEGER -> {
                    final IntegerNumberProperty.Builder builder = new IntegerNumberProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.integer(builder.index(true).build());
                }
                case DOUBLE -> {
                    final DoubleNumberProperty.Builder builder = new DoubleNumberProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.double_(builder.index(true).build());
                }
                case FLOAT -> {
                    final FloatNumberProperty.Builder builder = new FloatNumberProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.float_(builder.index(true).build());
                }
                case BOOLEAN -> {
                    final BooleanProperty.Builder builder = new BooleanProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.boolean_(builder.index(true).build());
                }
                case DATE -> {
                    final DateProperty.Builder builder = new DateProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.date(builder.index(true).build());
                }
                case DATE_RANGE -> {
                    final DateRangeProperty.Builder builder = new DateRangeProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.dateRange(builder.index(true).build());
                }
                case KEYWORD -> {
                    final KeywordProperty.Builder builder = new KeywordProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.keyword(builder.index(true).build());
                }
                case WILDCARD -> {
                    final WildcardProperty.Builder builder = new WildcardProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }

                    yield propertyBuilder.wildcard(builder.build());
                }
                case ALIAS -> {
                    final FieldAliasProperty.Builder builder = new FieldAliasProperty.Builder();

                    if (Objects.nonNull(properties)) {
                        builder.properties(buildProperties(properties));
                    }
                    builder.path(fieldName);
                    yield propertyBuilder.alias(builder.build());
                }
            };
            final Property property = propertyObjectBuilder.build();

            fieldProperties.put(property.isAlias() ? fieldProperty.getAliasName() : fieldName, property);
        }

        return fieldProperties;
    }

    /**
     * <p>
     * Builds the {@link IndexSettings} to update the index configurations.
     * </p>
     *
     * @param settingsUpdateRequest {@link SettingsUpdateRequest} that holds the details of the index configurations.
     * @return {@link IndexSettings} for configure the index.
     */
    private IndexSettings buildSettings(final SettingsUpdateRequest settingsUpdateRequest) {
        final String numberOfShards = settingsUpdateRequest.getNumberOfShardsAsString();
        final String numberOfReplicas = settingsUpdateRequest.getNumberOfReplicasAsString();
        final Integer numberOfRoutingShards = settingsUpdateRequest.getNumberOfRoutingShards();
        final boolean softDelete = settingsUpdateRequest.isSoftDeleteEnabled();
        final IndexSettings.Builder settingsBuilder = new IndexSettings.Builder();

        if (Objects.nonNull(numberOfShards)) {
            settingsBuilder.numberOfShards(numberOfShards);
        }

        if (Objects.nonNull(numberOfReplicas)) {
            settingsBuilder.numberOfReplicas(numberOfReplicas);
        }

        if (Objects.nonNull(numberOfRoutingShards)) {
            settingsBuilder.numberOfRoutingShards(numberOfRoutingShards);
        }
        settingsBuilder.softDeletes(new SoftDeletes.Builder().enabled(softDelete).build());

        return settingsBuilder.build();
    }
}
