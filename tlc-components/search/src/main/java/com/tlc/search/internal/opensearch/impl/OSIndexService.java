package com.tlc.search.internal.opensearch.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.search.internal.analyzer.opensearch.model.BuiltInAnalyzer;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenFilter;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenizer;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInCharFilter;
import com.tlc.search.model.IndexCustomAnalyzer;
import com.tlc.search.internal.analyzer.opensearch.OpenSearchCustomAnalyzerService;
import com.tlc.search.internal.analyzer.opensearch.registrar.OpenSearchCustomAnalyzerServiceRegistrar;
import com.tlc.search.internal.opensearch.AbstractOpenSearchService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.AnalysisComponent;
import com.tlc.search.model.FieldProperty;
import com.tlc.search.request.index.settings.IndexCloneRequest;
import com.tlc.search.request.index.definition.IndexCreateRequest;
import com.tlc.search.request.index.definition.IndexExistenceRequest;
import com.tlc.search.request.index.definition.IndexRemovalRequest;
import com.tlc.search.request.index.settings.MappingsUpdateRequest;
import com.tlc.search.request.index.settings.SettingsUpdateRequest;
import com.tlc.search.service.IndexService;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.analysis.Analyzer;
import org.opensearch.client.opensearch._types.analysis.CharFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.Tokenizer;
import org.opensearch.client.opensearch._types.mapping.BooleanProperty;
import org.opensearch.client.opensearch._types.mapping.ConstantKeywordProperty;
import org.opensearch.client.opensearch._types.mapping.DateProperty;
import org.opensearch.client.opensearch._types.mapping.DateRangeProperty;
import org.opensearch.client.opensearch._types.mapping.DoubleNumberProperty;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.FieldAliasProperty;
import org.opensearch.client.opensearch._types.mapping.FloatNumberProperty;
import org.opensearch.client.opensearch._types.mapping.IntegerNumberProperty;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.LongNumberProperty;
import org.opensearch.client.opensearch._types.mapping.NestedProperty;
import org.opensearch.client.opensearch._types.mapping.ObjectProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.mapping.WildcardProperty;
import org.opensearch.client.opensearch.indices.CloneIndexRequest;
import org.opensearch.client.opensearch.indices.CloseIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexState;
import org.opensearch.client.opensearch.indices.OpenRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.opensearch.client.opensearch.indices.SoftDeletes;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.opensearch.client.util.ObjectBuilder;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the index services to execute index related actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OSIndexService extends AbstractOpenSearchService implements IndexService {

    private final OpenSearchCustomAnalyzerServiceRegistrar customAnalyzerServiceRegistrar;

    public OSIndexService(final OpenSearchClient client) {
        super(client);
        customAnalyzerServiceRegistrar = OpenSearchCustomAnalyzerServiceRegistrar.getInstance();
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
        final OpenSearchIndicesClient indices = client.indices();
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
        final OpenSearchIndicesClient indices = client.indices();

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
            final OpenSearchIndicesClient indices = client.indices();

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
            return client.count(new org.opensearch.client.opensearch.core.CountRequest.Builder()
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
                final IndexSettings osIndexSettings = settings.index();

                if (Objects.nonNull(osIndexSettings)) {
                    final com.tlc.search.model.IndexSettings indexSettings = new com.tlc.search.model.IndexSettings();
                    final SoftDeletes softDeletes = osIndexSettings.softDeletes();

                    indexSettings.setNumberOfShards(osIndexSettings.numberOfShards());
                    indexSettings.setNumberOfReplicas(osIndexSettings.numberOfReplicas());
                    indexSettings.setNumberOfRoutingShards(osIndexSettings.numberOfRoutingShards());
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
            final String field = fieldProperty.getFieldName();

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
                    builder.index(true);
                    builder.analyzer(fieldProperty.getAnalyzerName());

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

                    yield propertyBuilder.keyword(builder.index(false).build());
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
                    builder.path(field);
                    yield propertyBuilder.alias(builder.build());
                }
            };
            final Property property = propertyObjectBuilder.build();

            fieldProperties.put(property.isAlias() ? fieldProperty.getAliasName() : field, property);
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
        final IndexSettings.Builder settingsBuilder = new IndexSettings.Builder();
        final String numberOfShards = settingsUpdateRequest.getNumberOfShardsAsString();
        final String numberOfReplicas = settingsUpdateRequest.getNumberOfReplicasAsString();
        final Integer numberOfRoutingShards = settingsUpdateRequest.getNumberOfRoutingShards();

        if (Objects.nonNull(numberOfShards)) {
            settingsBuilder.numberOfShards(numberOfShards);
        }

        if (Objects.nonNull(numberOfReplicas)) {
            settingsBuilder.numberOfReplicas(numberOfReplicas);
        }

        if (Objects.nonNull(numberOfRoutingShards)) {
            settingsBuilder.numberOfRoutingShards(numberOfRoutingShards);
        }

        if (settingsUpdateRequest.isCustomAnalysisRequired()) {
            final AnalysisComponent analysisComponent = settingsUpdateRequest.getAnalyzerComponent();
            final Map<String, Analyzer> analyzerMap = new HashMap<>();
            final Collection<IndexCustomAnalyzer> customAnalyzers = analysisComponent.getCustomAnalyzers();
            final Collection<BuiltInAnalyzer> analyzers = analysisComponent.getAnalyzers();
            final Map<String, Tokenizer> tokenizerMap = analysisComponent.getTokenizers().stream().collect(Collectors.toMap(
                    BuiltInTokenizer::getName, BuiltInTokenizer::getTokenizer));
            final Map<String, TokenFilter> tokenFilterMap = analysisComponent.getTokenFilters().stream().collect(Collectors.toMap(
                    BuiltInTokenFilter::getName, BuiltInTokenFilter::getTokenFilter));
            final Map<String, CharFilter> charFilterMap = analysisComponent.getCharFilters().stream().collect(Collectors.toMap(
                    BuiltInCharFilter::getName, BuiltInCharFilter::getCharFilter));
            final Collection<OpenSearchCustomAnalyzerService> customAnalyzerServices =
                    new ArrayList<>(customAnalyzers.stream().map(customAnalyzer -> {
                        final Optional<OpenSearchCustomAnalyzerService> optionalAnalyzerService =
                                customAnalyzerServiceRegistrar.opt(customAnalyzer);

                        return optionalAnalyzerService.orElseGet(() -> null);
                    }).toList());

            customAnalyzerServices.removeIf(Objects::isNull);
            customAnalyzerServices.forEach(customAnalyzerService ->
                    analyzerMap.put(customAnalyzerService.getType().getName(), customAnalyzerService.getCustomAnalyzer()));
            analyzers.forEach(analyzer -> analyzerMap.put(analyzer.getName(), analyzer.get()));
            final IndexSettingsAnalysis.Builder analysisBuilder = new IndexSettingsAnalysis.Builder();

            if (!tokenizerMap.isEmpty()) {
                analysisBuilder.tokenizer(tokenizerMap);
            }

            if (!tokenFilterMap.isEmpty()) {
                analysisBuilder.filter(tokenFilterMap);
            }

            if (!charFilterMap.isEmpty()) {
                analysisBuilder.charFilter(charFilterMap);
            }

            if (!analyzerMap.isEmpty()) {
                analysisBuilder.analyzer(analyzerMap);
            }

            settingsBuilder.analysis(analysisBuilder.build());
        }

        return settingsBuilder.build();
    }
}
