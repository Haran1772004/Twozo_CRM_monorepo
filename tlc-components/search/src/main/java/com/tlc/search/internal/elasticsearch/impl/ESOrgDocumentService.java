package com.tlc.search.internal.elasticsearch.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.ScriptField;
import co.elastic.clients.elasticsearch._types.StoredScriptId;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchHeader;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;

import co.elastic.clients.json.JsonData;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.model.RangeClause;
import com.tlc.search.model.SortClause;
import com.tlc.search.model.ScriptInfo;
import com.tlc.search.model.IndexRequestModel;
import com.tlc.search.model.CountRequest;
import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.multi.read.MultiSearchIndexRequest;
import com.tlc.search.request.index.multi.read.count.MultiIndexCountRequest;
import com.tlc.search.request.index.single.read.count.SingleIndexCountRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.service.OrgDocumentService;
import jakarta.json.JsonValue;

import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service to execute all the organisation dependent readable and writable services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ESOrgDocumentService extends ESWritableDocumentService implements OrgDocumentService {

    private static final int ZERO = 0;
    private static final String ORG_ID_FIELD = "orgId";
    private static final String CONTAINS_FORMAT = "*%s*";
    private static final String STARTS_WITH_FORMAT = "%s*";
    private static final String ENDS_WITH_FORMAT = "*%s";
    private static final String FIELDS = "fields";

    private final Long orgId;

    public ESOrgDocumentService(final Long orgId, final ElasticsearchClient client) {
        super(client);
        this.orgId = orgId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject search(final SingleIndexSearchRequest singleIndexSearchRequest) {
        final String index = singleIndexSearchRequest.getIndexName();

        if (index.isBlank()) {
            return Json.object();
        }
        final SearchRequest searchRequest = singleIndexSearchRequest.getSearchRequest();
        final FilterClause baseFilter = searchRequest.getFilter();

        if (Objects.isNull(baseFilter)) {
            return Json.object();
        }
        appendOrgFilter(searchRequest);
        final FilterClause orgAppendedFilter = searchRequest.getFilter();
        final RangeClause rangeClause = searchRequest.getRangeClause();
        final int start;
        final int fetchCount;

        if (Objects.isNull(rangeClause)) {
            start = NumericalConstant.ZERO;
            fetchCount = count(SingleIndexCountRequest.get(index, new CountRequest(orgAppendedFilter))).intValue();
        } else {
            start = rangeClause.start();
            fetchCount = rangeClause.limit();
        }

        return search(searchRequest, start, fetchCount, orgAppendedFilter, List.of(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject search(final MultiSearchIndexRequest multiSearchRequest) {
        final List<String> indexes = multiSearchRequest.getIndexNames();

        if (indexes.isEmpty()) {
            return Json.object();
        }
        final SearchRequest searchRequest = multiSearchRequest.getSearchRequest();

        if (Objects.isNull(searchRequest.getFilter())) {
            return Json.object();
        }
        appendOrgFilter(searchRequest);
        final FilterClause orgAppendedFilter = searchRequest.getFilter();
        final RangeClause rangeClause = searchRequest.getRangeClause();
        final int start;
        final int fetchCount;

        if (Objects.isNull(rangeClause)) {
            start = NumericalConstant.ZERO;
            fetchCount = count(MultiIndexCountRequest.get(indexes, new CountRequest(orgAppendedFilter))).intValue();
        } else {
            start = rangeClause.start();
            fetchCount = rangeClause.limit();
        }

        return search(searchRequest, start, fetchCount, orgAppendedFilter, indexes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject multiSearchGroupedByIndex(final MultiSingleIndexRequest multiSearchRequest) {
        final List<RequestItem> requestItems = buildMultiSearchRequestItems(multiSearchRequest);

        if (!requestItems.isEmpty()) {
            try {
                return convertMultiSearchResponseToJson(client.msearch(new MsearchRequest.Builder()
                        .searches(requestItems).ignoreUnavailable(true).build(), Map.class).responses());
            } catch (IOException e) {
                throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_SEARCH_FAILED, e);
            }
        }
        return Json.object();
    }

    /**
     * {@inheritDoc}
     *
     * @param multiSearchRequest {@link MultiSingleIndexRequest}.
     * @return one {@link JsonArray} slot per executed sub-request.
     */
    @Override
    public JsonArray multiSearchGroupedByOrder(final MultiSingleIndexRequest multiSearchRequest) {
        if (Objects.isNull(multiSearchRequest)) {
            return Json.array();
        }
        final List<RequestItem> requestItems = buildMultiSearchRequestItems(multiSearchRequest);

        if (requestItems.isEmpty()) {
            return Json.array();
        }

        try {
            final List<MultiSearchResponseItem<Map>> responses = client.msearch(new MsearchRequest.Builder()
                    .searches(requestItems).ignoreUnavailable(true).build(), Map.class).responses();

            return convertMultiSearchResponseToDocumentSlots(responses);
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_SEARCH_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long count(final MultiIndexCountRequest multiIndexCountRequest) {
        final List<String> indexes = multiIndexCountRequest.getIndexNames();
        final CountRequest countRequest = multiIndexCountRequest.getCountRequest();

        if (indexes.isEmpty() || Objects.isNull(countRequest) || Objects.isNull(countRequest.getFilter())) {
            return (long) NumericalConstant.ZERO;
        }

        return count(countRequest, indexes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long count(final SingleIndexCountRequest singleIndexCountRequest) {
        final String index = singleIndexCountRequest.getIndexName();
        final CountRequest countRequest = singleIndexCountRequest.getCountRequest();

        if (Objects.isNull(index) || index.isBlank() || Objects.isNull(countRequest) ||
                Objects.isNull(countRequest.getFilter())) {
            return (long) NumericalConstant.ZERO;
        }

        return count(countRequest, List.of(index));
    }

    /**
     * <p>
     * Checks whether the doc exists for the given id & index.
     * </p>
     *
     * @param id    Represents the doc's id.
     * @param index Represents the index name.
     * @return whether the doc exists.
     */
    @Override
    public boolean exists(final String id, final String index) {

        try {
            final ExistsRequest.Builder existsRequestBuilder = new ExistsRequest.Builder();

            existsRequestBuilder.id(id);
            existsRequestBuilder.index(index);

            return client.exists(existsRequestBuilder.build()).value();
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.EXIST_REQUEST_FAILED, e);
        }
    }

    /**
     * <p>
     * Gets the count.
     * </p>
     *
     * @param countRequest {@link CountRequest}.
     * @param indexes      index names.
     * @return the count.
     */
    private Long count(final CountRequest countRequest, final List<String> indexes) {
        final co.elastic.clients.elasticsearch.core.CountRequest.Builder requestBuilder =
                new co.elastic.clients.elasticsearch.core.CountRequest.Builder();
        final BoolQuery.Builder queryBuilder = new BoolQuery.Builder();

        appendOrgFilter(countRequest);
        requestBuilder.index(indexes);
        final FilterClause orgAppendedFilter = countRequest.getFilter();

        loadFilter(orgAppendedFilter, queryBuilder, orgAppendedFilter.getConnector());
        requestBuilder.query(new Query.Builder().bool(queryBuilder.build()).build());

        try {
            return client.count(requestBuilder.build()).count();
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.UNABLE_TO_FETCH_COUNT, "i18n.search.count.query.failed");
        }
    }

    /**
     * <p>
     * Searches based on the param inputs.
     * </p>
     *
     * @param searchRequest     {@link SearchRequest}.
     * @param start             Start point.
     * @param fetchCount        Docs to be fetched count.
     * @param orgAppendedFilter Org appended filter.
     * @param indexes           Index names.
     * @return {@link JsonObject}.
     */
    private JsonObject search(final SearchRequest searchRequest, final int start, final int fetchCount,
                              final FilterClause orgAppendedFilter, final List<String> indexes) {
        final co.elastic.clients.elasticsearch.core.SearchRequest.Builder requestBuilder =
                new co.elastic.clients.elasticsearch.core.SearchRequest.Builder();
        final List<String> selectedFields = searchRequest.getSelectFields();
        final List<String> excludedFields = searchRequest.getExcludedFields();
        final Collection<SortClause> sortClauses = searchRequest.getSortClauses();
        final Collection<ScriptInfo> scripts = searchRequest.getScripts();
        final BoolQuery.Builder queryBuilder = new BoolQuery.Builder();
        final SearchResponse<Map> searchResponse;

        requestBuilder.from(start);
        requestBuilder.size(fetchCount);

        if (Objects.nonNull(selectedFields) || Objects.nonNull(excludedFields)) {
            requestBuilder.source(buildSourceConfig(selectedFields, excludedFields));
        }

        if (!scripts.isEmpty()) {
            requestBuilder.scriptFields(getScriptFieldMap(scripts));
        }

        if (Objects.nonNull(sortClauses)) {
            requestBuilder.sort(buildSortOptions(sortClauses));
        }
        loadFilter(orgAppendedFilter, queryBuilder, orgAppendedFilter.getConnector());
        requestBuilder.query(new Query.Builder().bool(queryBuilder.build()).build());
        requestBuilder.index(indexes);

        try {
            searchResponse = client.search(requestBuilder.build(), Map.class);
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_SEARCH_FAILED, e);
        }

        return convertHitsToJson(searchResponse.hits().hits());
    }

    /**
     * <p>
     * Appends org criteria.
     * </p>
     *
     * @param requestModel {@link IndexRequestModel}.
     */
    private void appendOrgFilter(final IndexRequestModel requestModel) {
        final FilterClause existing = requestModel.getFilter();
        final FilterClause orgFilter = new FilterClause(new Criteria(orgId, Comparator.EQUAL, ORG_ID_FIELD));

        requestModel.setFilter(Objects.isNull(existing) ? orgFilter : orgFilter.and(existing));
    }

    /**
     * <p>
     * Gets the script field map.
     * </p>
     *
     * @param scripts Collection of {@link ScriptInfo}.
     * @return script field map.
     */
    private Map<String, ScriptField> getScriptFieldMap(final Collection<ScriptInfo> scripts) {
        final Map<String, ScriptField> scriptFieldMap = new HashMap<>();

        for (final ScriptInfo scriptInfo : scripts) {
            final String id = scriptInfo.getId();
            final Map<String, Object> paramsMap = scriptInfo.getParamsMap();
            final Map<String, JsonData> paramsJsonDataMap = new HashMap<>(paramsMap.size());

            paramsMap.forEach((key, value) -> paramsJsonDataMap.put(key, JsonData.of(value)));
            final ScriptField scriptField = ScriptField.of(scriptFieldFunction -> scriptFieldFunction.script(Script.of(
                    scriptFunction -> scriptFunction.stored(StoredScriptId.of(
                            stored -> stored.id(id).params(paramsJsonDataMap))))));
            scriptFieldMap.put(scriptInfo.getFieldName(), scriptField);
        }

        return scriptFieldMap;
    }

    /**
     * <p>
     * Builds the source config to select the required fields.
     * </p>
     *
     * @param selectedFields Fields to be selected.
     * @param excludedFields Fields to be excluded.
     * @return {@link SourceConfig} Holds the configurations of the fields includes.
     */
    private SourceConfig buildSourceConfig(final List<String> selectedFields, final List<String> excludedFields) {
        final SourceFilter.Builder builder = new SourceFilter.Builder();

        if (Objects.nonNull(selectedFields) && !selectedFields.isEmpty()) {
            builder.includes(selectedFields);
        }

        if (Objects.nonNull(excludedFields) && !excludedFields.isEmpty()) {
            builder.excludes(excludedFields);
        }

        return new SourceConfig.Builder().filter(builder.build()).build();
    }

    /**
     * <p>
     * Converts the multi search response into {@link JsonObject}.
     * </p>
     *
     * @param responses Holds the multi search response.
     * @return Multi search response as {@link JsonObject}.
     */
    private JsonObject convertMultiSearchResponseToJson(final List<MultiSearchResponseItem<Map>> responses) {
        final Map<String, List<Object>> responseMap = new HashMap<>();
        final Collection<JsonObject> indexesHitsJson = new ArrayList<>();
        final List<List<Hit<Map>>> indexesHits = responses.stream().map(response -> response.result().hits().hits()).toList();

        for (final List<Hit<Map>> indexHits : indexesHits) {
            indexesHitsJson.add(convertHitsToJson(indexHits));
        }

        for (final JsonObject indexHitsJson : indexesHitsJson) {
            indexHitsJson.forEach((index, hit) -> responseMap.computeIfAbsent(index, source -> new ArrayList<>()).addAll((Collection<?>) hit));
        }

        return Json.object(responseMap);
    }

    /**
     * <p>
     * Converts the multi search response into ordered document slots.
     * </p>
     *
     * @param responses Holds the multi search response.
     * @return One {@link JsonArray} of documents per msearch response item.
     */
    private JsonArray convertMultiSearchResponseToDocumentSlots(final List<MultiSearchResponseItem<Map>> responses) {
        final JsonArray documents = Json.array();
        final List<List<Hit<Map>>> indexesHits = responses.stream().map(response -> response.result().hits().hits()).toList();

        for (final List<Hit<Map>> indexHits : indexesHits) {
            documents.put(convertHitsToDocumentJsonArray(indexHits));
        }

        return documents;
    }

    /**
     * <p>
     * Converts the search response into {@link JsonObject}.
     * </p>
     *
     * @param indexHits Holds the list of search responses.
     * @return Search response as {@link JsonObject}.
     */
    private JsonObject convertHitsToJson(final List<Hit<Map>> indexHits) {
        final Map<String, List<Object>> indexHitMap = new LinkedHashMap<>();

        indexHits.forEach(hit -> {
            final Map hitResult = hit.source();

            if (Objects.nonNull(hitResult) && !hitResult.isEmpty()) {
                final Map<String, Object> results = new HashMap<>(hitResult);

                for (final Map.Entry<String, JsonData> fieldEntries : hit.fields().entrySet()) {
                    final String scriptFieldKey = fieldEntries.getKey();

                    if (Objects.equals(FIELDS, scriptFieldKey)) {
                        populateResultsWithMultipleFields(fieldEntries.getValue().to(Object.class), results);
                    } else if (results.containsKey(scriptFieldKey)) {
                        populateResultsWithAField(scriptFieldKey, fieldEntries.getValue().to(Object.class), results);
                    }
                }
                indexHitMap.computeIfAbsent(hit.index(), source -> new ArrayList<>()).add(results);
            }
        });

        return Json.object(indexHitMap);
    }

    /**
     * <p>
     * Converts search hits into a {@link JsonArray} of document {@link JsonObject}s.
     * </p>
     *
     * @param indexHits Holds the list of search hits.
     * @return documents in hit order.
     */
    private JsonArray convertHitsToDocumentJsonArray(final List<Hit<Map>> indexHits) {
        final JsonArray documents = Json.array();

        indexHits.forEach(hit -> {
            final Map<String, Object> results = convertHitToResultMap(hit);

            if (Objects.nonNull(results) && !results.isEmpty()) {
                documents.put(Json.object(results));
            }
        });

        return documents;
    }

    /**
     * <p>
     * Converts a single search hit into a result map including script fields.
     * </p>
     *
     * @param hit {@link Hit}.
     * @return result map, or {@code null} when the hit has no source.
     */
    private Map<String, Object> convertHitToResultMap(final Hit<Map> hit) {
        final Map hitResult = hit.source();

        if (Objects.isNull(hitResult) || hitResult.isEmpty()) {
            return null;
        }
        final Map<String, Object> results = new HashMap<>(hitResult);

        for (final Map.Entry<String, JsonData> fieldEntries : hit.fields().entrySet()) {
            final String scriptFieldKey = fieldEntries.getKey();

            if (Objects.equals(FIELDS, scriptFieldKey)) {
                populateResultsWithMultipleFields(fieldEntries.getValue().to(Object.class), results);
            } else if (results.containsKey(scriptFieldKey)) {
                populateResultsWithAField(scriptFieldKey, fieldEntries.getValue().to(Object.class), results);
            }
        }

        return results;
    }

    /**
     * <p>
     * Populates the results map with a field.
     * </p>
     *
     * @param scriptFieldKey Field's name.
     * @param results        Map holding fetched results.
     * @param value          Processed value.
     */
    private void populateResultsWithAField(final String scriptFieldKey, final Object value, final Map<String, Object> results) {

        if (value instanceof final jakarta.json.JsonObject jsonObject) {
            final Map<String, Object> processedMap = jsonObject.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, entry -> processJsonValue(entry.getValue())));

            results.put(scriptFieldKey, processedMap);
        } else if (value instanceof final jakarta.json.JsonArray jsonArray) {
            final List<Object> processedList = jsonArray.stream().map(this::processJsonValue).toList();

            processedList.forEach(object -> results.putAll((Map<String, Object>) object));
        }
    }

    /**
     * <p>
     * Populates the results map with multiple fields.
     * </p>
     *
     * @param results Map holding fetched results.
     * @param value   Processed value.
     */
    private void populateResultsWithMultipleFields(final Object value, final Map<String, Object> results) {

        if (value instanceof final jakarta.json.JsonObject jsonObject) {
            final Map<String, Object> processedMap = jsonObject.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, entry -> processJsonValue(entry.getValue())));

            for (final Map.Entry<String, Object> processedMapEntry : processedMap.entrySet()) {
                populateResults(processedMapEntry.getKey(), results, processedMapEntry.getValue());
            }
        } else if (value instanceof final jakarta.json.JsonArray jsonArray) {
            final List<Object> processedList = jsonArray.stream().map(this::processJsonValue).toList();

            processedList.forEach(object -> {

                if (object instanceof final Map<?, ?> processedMap) {
                    for (final Map.Entry<?, ?> processedMapEntry : processedMap.entrySet()) {
                        populateResults(processedMapEntry.getKey().toString(), results, processedMapEntry.getValue());
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Populates the results map.
     * </p>
     *
     * @param key     Field's name.
     * @param results Map holding fetched results.
     * @param value   Processed value.
     */
    private void populateResults(final String key, final Map<String, Object> results, final Object value) {

        if (results.containsKey(key)) {
            final Map<String, Object> fieldMap = new HashMap<>((Map<String, Object>) results.get(key));

            fieldMap.putAll((Map<String, Object>) value);
            results.put(key, fieldMap);
        } else {
            results.put(key, value);
        }
    }

    /**
     * <p>
     * Processes the {@link JsonValue}.
     * </p>
     *
     * @param value {@link JsonValue}.
     * @return processed value.
     */
    private Object processJsonValue(final JsonValue value) {

        if (Objects.nonNull(value)) {

            switch (value.getValueType()) {
                case STRING -> {
                    return ((jakarta.json.JsonString) value).getString();
                }
                case NUMBER -> {
                    return ((jakarta.json.JsonNumber) value).numberValue();
                }
                case TRUE, FALSE -> {
                    return Boolean.parseBoolean(value.toString());
                }
                case OBJECT -> {
                    final Map<String, Object> fieldMap = new HashMap<>();

                    value.asJsonObject().forEach((key, jsonValue) -> {

                        if (!Objects.equals(JsonValue.ValueType.NULL, jsonValue.getValueType())) {
                            fieldMap.put(key, processJsonValue(jsonValue));
                        }
                    });

                    return fieldMap;
                }
                case ARRAY -> {
                    final jakarta.json.JsonArray jsonArray = value.asJsonArray();

                    return jsonArray.stream().filter(jsonValue ->
                            !Objects.equals(JsonValue.ValueType.NULL, jsonValue.getValueType())).map(this::processJsonValue).toList();
                }
                default -> {
                    return null;
                }
            }
        }
        return null;
    }


    /**
     * <p>
     * Builds the sort options from the collection of {@link SortClause}.
     * </p>
     *
     * @param sortClauses Holds the collection of {@link SortClause}.
     * @return List of {@link SortOptions}.
     */
    private List<SortOptions> buildSortOptions(final Collection<SortClause> sortClauses) {
        final List<SortOptions> sortOptions = new ArrayList<>();

        for (final SortClause sortClause : sortClauses) {
            sortOptions.add(new SortOptions.Builder().field(new FieldSort.Builder().order(SortOrder.valueOf(sortClause.getType().getName()))
                    .field(sortClause.getField()).build()).build());
        }

        return sortOptions;
    }

    /**
     * <p>
     * Loads the {@link FilterClause} into {@link BoolQuery}.
     * </p>
     *
     * @param filter       {@link FilterClause} that holds the details of the retrieval criteria.
     * @param queryBuilder Holds the criteria to fetch data.
     * @param connector    For connects two {@link FilterClause}.
     */
    private void loadFilter(final FilterClause filter, final BoolQuery.Builder queryBuilder, final FilterClause.Connector connector) {
        final FilterClause.Connector orConnector = FilterClause.Connector.OR;

        if (filter.hasConnector()) {
            final FilterClause left = filter.getLeft();
            final FilterClause right = filter.getRight();
            final FilterClause.Connector filterConnector = filter.getConnector();
            final BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            loadFilter(left, boolQueryBuilder, filterConnector);
            loadFilter(right, boolQueryBuilder, filterConnector);
            final Query query = new Query.Builder().bool(boolQueryBuilder.build()).build();

            if (Objects.equals(orConnector, connector)) {
                queryBuilder.should(query);
            } else {
                queryBuilder.must(query);
            }
        } else {
            final Criteria criteria = filter.getCriteria();

            if (Objects.equals(orConnector, connector)) {
                queryBuilder.should(buildQuery(criteria));
            } else if (criteria.comparator().isInverted()) {
                queryBuilder.mustNot(buildQuery(criteria));
            } else {
                queryBuilder.must(buildQuery(criteria));
            }
        }
    }

    /**
     * <p>
     * Builds the {@link Query} using {@link Criteria}.
     * </p>
     *
     * @param criteria Holds the details of retrieval criteria.
     * @return {@link Query} that holds the retrieval criteria.
     */
    private Query buildQuery(final Criteria criteria) {
        final Object value = criteria.value();
        final String stringValue = value.toString();
        final String[] fields = criteria.fields();

        if (Objects.isNull(fields) || ZERO == fields.length) {
            return new Query.Builder().queryString(getWildCardAppliedQueryString(stringValue)).build();
        } else {
            if (value instanceof final Collection<?> collection) {
                return new Query.Builder().terms(getTermsQuery(fields[ZERO], collection)).build();
            } else if (value instanceof Number[] numbers) {
                return new Query.Builder().terms(getTermsQuery(fields[ZERO], List.of(numbers))).build();
            } else if (value instanceof Number) {
                return new Query.Builder().term(getTermQuery(fields[ZERO], value)).build();
            } else if (value instanceof String && StringUtil.containsWhitespace(stringValue)) {
                return new Query.Builder().multiMatch(getMultiMatchQuery(stringValue, stringValue)).build();
            } else {
                final String formattedString = switch (criteria.comparator()) {
                    case CONTAINS -> String.format(CONTAINS_FORMAT, stringValue);
                    case STARTS_WITH -> String.format(STARTS_WITH_FORMAT, stringValue);
                    case ENDS_WITH -> String.format(ENDS_WITH_FORMAT, stringValue);
                    default -> stringValue;
                };

                return new Query.Builder().queryString(getQueryString(formattedString, formattedString)).build();
            }
        }
    }

    /**
     * <p>
     * Fetches the {@link QueryStringQuery} for apply string matching criteria.
     * </p>
     *
     * @param fields Field names in an Array.
     * @param value  Value of the corresponding field.
     * @return {@link QueryStringQuery} Holds the string query.
     */
    private QueryStringQuery getQueryString(final String value, final String... fields) {
        final QueryStringQuery.Builder queryBuilder = new QueryStringQuery.Builder();

        if (Objects.isNull(fields) || ZERO == fields.length) {
            queryBuilder.allowLeadingWildcard(true).analyzeWildcard(true);
        } else {
            queryBuilder.fields(Arrays.asList(fields));
        }

        return queryBuilder.query(value).build();
    }

    /**
     * <p>
     * Fetches the {@link MultiMatchQuery} for apply string matching criteria.
     * </p>
     *
     * @param fields Field names in an Array.
     * @param value  Value of the corresponding field.
     * @return {@link MultiMatchQuery}.
     */
    private MultiMatchQuery getMultiMatchQuery(final String value, final String... fields) {
        final MultiMatchQuery.Builder queryBuilder = new MultiMatchQuery.Builder();

        if (Objects.isNull(fields) || ZERO == fields.length) {
            queryBuilder.fields("*");
        } else {
            queryBuilder.fields(Arrays.asList(fields));
        }
        queryBuilder.type(TextQueryType.PhrasePrefix).operator(Operator.And);

        return queryBuilder.query(value).build();
    }

    /**
     * <p>
     * Fetches the {@link QueryStringQuery} for apply string matching criteria.
     * </p>
     *
     * @param value Value of the corresponding field.
     * @return {@link QueryStringQuery} Holds the string query.
     */
    private QueryStringQuery getWildCardAppliedQueryString(final String value) {
        final QueryStringQuery.Builder queryBuilder = new QueryStringQuery.Builder();

        queryBuilder.allowLeadingWildcard(true).analyzeWildcard(true);

        return queryBuilder.query(value).build();
    }
    /**
     * <p>
     * Fetches the {@link TermsQuery} for apply string matching criteria.
     * </p>
     *
     * @param field  field name.
     * @param values Value of the corresponding field.
     * @return {@link TermsQuery} Holds the string query.
     */
    private TermsQuery getTermsQuery(final String field, final Collection<?> values) {
        final TermsQuery.Builder termsQuery = new TermsQuery.Builder();
        final TermsQueryField.Builder termsQueryField = new TermsQueryField.Builder();

        values.stream().findFirst().ifPresent(firstElement -> {
            final List<FieldValue> fieldValues = new ArrayList<>();

            if (firstElement instanceof Long) {
                fieldValues.addAll(values.stream().map(value -> FieldValue.of((long) value)).toList());
            } else if (firstElement instanceof Double) {
                fieldValues.addAll(values.stream().map(value -> FieldValue.of((Double) value)).toList());
            } else if (firstElement instanceof Integer) {
                fieldValues.addAll(values.stream().map(value -> FieldValue.of((int) value)).toList());
            } else if (firstElement instanceof Short) {
                fieldValues.addAll(values.stream().map(value -> FieldValue.of((short) value)).toList());
            }

            termsQueryField.value(fieldValues);
            termsQuery.field(field);
            termsQuery.terms(termsQueryField.build());
        });
        return termsQuery.build();
    }

    /**
     * <p>
     * Fetches the {@link TermQuery} for apply string matching criteria.
     * </p>
     *
     * @param field field name.
     * @param value Value of the corresponding field.
     * @return {@link TermQuery} Holds the string query.
     */
    private TermQuery getTermQuery(final String field, final Object value) {
        final TermQuery.Builder termQuery = new TermQuery.Builder();

        termQuery.field(field);

        if (value instanceof Long) {
            termQuery.value(FieldValue.of((long) value));
        } else if (value instanceof Integer) {
            termQuery.value(FieldValue.of((int) value));
        } else if (value instanceof Short) {
            termQuery.value(FieldValue.of((short) value));
        } else if (value instanceof Double) {
            termQuery.value(FieldValue.of((double) value));
        }

        return termQuery.build();
    }

    /**
     * <p>
     * Gets the document ids for the given field & index.
     * </p>
     *
     * @param fieldName Represents the field name.
     * @param index     Represents the index name.
     * @param exists    True to check if field exists, false to check if field does not exist.
     * @return Collection of document ids.
     */
    @Override
    public Collection<String> getDocIdsBasedOnFieldExistence(final boolean exists, final String fieldName, final String index) {
        final List<String> documentIds = new ArrayList<>();
        final co.elastic.clients.elasticsearch.core.SearchRequest.Builder requestBuilder =
                new co.elastic.clients.elasticsearch.core.SearchRequest.Builder();
        final Query existsQuery = new Query.Builder().exists(query -> query.field(fieldName)).build();
        final BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        requestBuilder.index(index);

        if (exists) {
            boolQueryBuilder.must(existsQuery);
        } else {
            boolQueryBuilder.mustNot(existsQuery);
        }

        requestBuilder.query(new Query.Builder().bool(boolQueryBuilder.build()).build());
        requestBuilder.source(SourceConfig.of(source -> source.fetch(false)));
        requestBuilder.size(10000);

        try {
            final SearchResponse<Map> response = client.search(requestBuilder.build(), Map.class);

            for (final Hit<Map> hit : response.hits().hits()) {
                final String id = hit.id();

                if (Objects.nonNull(id)) {
                    documentIds.add(id);
                }
            }
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_SEARCH_FAILED, e);
        }
        return documentIds;
    }

    /**
     * <p>
     * Builds msearch {@link RequestItem}s from {@link MultiSingleIndexRequest}.
     * </p>
     *
     * @param multiSearchRequest {@link MultiSingleIndexRequest}.
     * @return request items for sub-requests that have a filter.
     */
    private List<RequestItem> buildMultiSearchRequestItems(final MultiSingleIndexRequest multiSearchRequest) {
        final List<RequestItem> requestItems = new ArrayList<>();
        final Collection<SingleIndexSearchRequest> searchRequests = multiSearchRequest.get();

        for (final SingleIndexSearchRequest singleIndexSearchRequest : searchRequests) {
            final String index = singleIndexSearchRequest.getIndexName();
            final SearchRequest searchRequest = singleIndexSearchRequest.getSearchRequest();

            if (Objects.isNull(searchRequest.getFilter())) {
                continue;
            }
            appendOrgFilter(searchRequest);
            final FilterClause orgAppendedFilter = searchRequest.getFilter();
            final MultisearchBody.Builder multiSearchBuilder = new MultisearchBody.Builder();
            final MultisearchHeader.Builder multiSearchHeaderBuilder = new MultisearchHeader.Builder();
            final RangeClause rangeClause = searchRequest.getRangeClause();
            final List<String> selectedFields = searchRequest.getSelectFields();
            final List<String> excludedFields = searchRequest.getExcludedFields();
            final Collection<SortClause> sortClauses = searchRequest.getSortClauses();
            final Collection<ScriptInfo> scripts = searchRequest.getScripts();
            final BoolQuery.Builder queryBuilder = new BoolQuery.Builder();

            if (Objects.isNull(rangeClause)) {
                multiSearchBuilder.from(NumericalConstant.ZERO);
                multiSearchBuilder.size(count(SingleIndexCountRequest.get(index, new CountRequest(orgAppendedFilter))).intValue());
            } else {
                multiSearchBuilder.from(rangeClause.start());
                multiSearchBuilder.size(rangeClause.limit());
            }

            if (Objects.nonNull(selectedFields) || Objects.nonNull(excludedFields)) {
                multiSearchBuilder.source(buildSourceConfig(selectedFields, excludedFields));
            }

            if (!scripts.isEmpty()) {
                multiSearchBuilder.scriptFields(getScriptFieldMap(scripts));
            }

            if (Objects.nonNull(sortClauses)) {
                multiSearchBuilder.sort(buildSortOptions(sortClauses));
            }
            loadFilter(orgAppendedFilter, queryBuilder, orgAppendedFilter.getConnector());
            multiSearchHeaderBuilder.index(index);
            multiSearchBuilder.query(new Query.Builder().bool(queryBuilder.build()).build());
            requestItems.add(new RequestItem.Builder().header(multiSearchHeaderBuilder.build()).body(multiSearchBuilder.build()).build());
        }

        return requestItems;
    }
}