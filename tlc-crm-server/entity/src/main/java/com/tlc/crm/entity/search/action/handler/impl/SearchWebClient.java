package com.tlc.crm.entity.search.action.handler.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.security.search.SearchAccessController;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntry;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntriesContext;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.fields.SearchApiKeys;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.service.ReadableDocumentService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>
 * Handles web client's global search api.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class SearchWebClient extends AbstractSearchClient {

    private static final String CONTACT_SOURCE = Source.CONTACT.getName();
    private static final String COMPANY_SOURCE = Source.COMPANY.getName();
    private static final String DEAL_SOURCE = Source.DEAL.getName();
    private static final String PRODUCT_SOURCE = Source.PRODUCT.getName();

    private final SearchAccessController searchAccessController;
    private final FieldService fieldService;

    private static class Instance {
        private static final SearchWebClient INSTANCE = new SearchWebClient();
    }

    private SearchWebClient() {
        searchAccessController = SearchAccessController.getWebClientController();
        fieldService = FieldService.getInstance();
    }

    /**
     * <p>
     * Gets {@link SearchWebClient}.
     * </p>
     *
     * @return {@link SearchWebClient}.
     */
    public static SearchWebClient getInstance() {
        return SearchWebClient.Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles search requests.
     * </p>
     *
     * @param request     {@link CrmRequest}.
     * @param crmResponse {@link CrmResponse}.
     */
    @Override
    public void process(final CrmRequest request, final CrmResponse crmResponse) {
        final long userId = request.userId();
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());

        switch (type) {
            case SearchApiKeys.GET_ALL_INDEXES -> crmResponse.put(CommonSearchFields.LIST, getIndexArray(orgId, crmResponse));
            case SearchApiKeys.SEARCH -> processSearchRequest(orgId, data, crmResponse);
            case SearchApiKeys.RETRIEVE_ENTRY -> saveViewedEntry(orgId, userId, data, crmResponse);
            case SearchApiKeys.GET_VIEWED_ENTRIES -> processRecentlyViewedRequest(orgId, userId, crmResponse);
        }
    }

    /**
     * <p>
     * Whether the source's accessible.
     * </p>
     *
     * @param orgId  An organization's id.
     * @param source Source's name.
     * @return whether the source's accessible.
     */
    @Override
    protected boolean isAccessible(final Long orgId, final String source) {
        return searchAccessController.isAccessible(orgId, source);
    }

    /**
     * <p>
     * Builds {@link SingleIndexSearchRequest} for an index.
     * </p>
     *
     * @param indexValue  Represents the index key's value.
     * @param searchText  Searched text.
     * @param start       Indicates the point start where doc should be fetched.
     * @param docsToFetch Total amount of docs to be fetched.
     * @return {@link SingleIndexSearchRequest}.
     */
    @Override
    protected SingleIndexSearchRequest build(final String indexValue, final String searchText, final int start, final int docsToFetch) {

        return searchAccessController.build(indexValue, searchText, start, docsToFetch);
    }

    /**
     * <p>
     * Builds search requests for recently viewed docs from viewed-time ordered entries.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param userId User's id.
     * @return {@link RecentlyViewedEntriesContext}.
     */
    @Override
    protected RecentlyViewedEntriesContext build(final long orgId, final long userId) {

        return searchAccessController.build(
                RecentlyViewedEntry.toIndexOrderedIds(commonSearchManager.getRecentlyViewedEntriesInOrder(orgId, userId, false)));
    }

    /**
     * <p>
     * Populates web recently viewed entry with matched results and details.
     * </p>
     *
     * @param indexName          Index name.
     * @param isPermissible      Whether full recently viewed details are permissible.
     * @param documentJson       Fetched document or empty object.
     * @param recentlyViewedJson Response entry to populate.
     */
    @Override
    protected void populateRecentlyViewedEntryResponse(final String indexName, final boolean isPermissible,
                                                       final JsonObject documentJson, final JsonObject recentlyViewedJson) {
        recentlyViewedJson.put(CommonSearchFields.MATCHED_RESULTS, buildRecentlyViewedResponse(
                isPermissible, indexName, documentJson, recentlyViewedJson));
    }

    /**
     * <p>
     * Loads fetched docs into matched results doc for recently viewed response.
     * </p>
     *
     * @param index          Represents the index's name.
     * @param indexJson      Fetched docs.
     * @param matchedResults Json to put matched results.
     */
    protected void loadMatchedResultsForRecentlyViewedEntities(final String index, final JsonObject indexJson,
                                                               final JsonObject matchedResults) {
        final Collection<String> contactFieldKeys = List.of(CommonSearchFields.EMAILS, CommonSearchFields.PHONES);

        switch (index) {
            case CommonSearchFields.CONTACT -> {
                loadEntityDetailResponse(indexJson, matchedResults, Map.of(CommonSearchFields.EMAILS, CommonSearchFields.VALUE, CommonSearchFields.PHONES, CommonSearchFields.VALUE));
                loadAddress(null, indexJson, null);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                addEntityJsonForIndexCards(indexJson, matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.COMPANY -> {
                loadAddress(null, indexJson, null);
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.DEAL -> {
                loadSubEntityFields(indexJson.optJsonObject(CommonSearchFields.CONTACT), contactFieldKeys);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                addEntityJsonForIndexCards(indexJson, matchedResults);
                setDealArchiveStateInMatchedResults(indexJson, matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.PRODUCT -> loadOwnerName(indexJson, matchedResults);
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                loadSubEntityFields(indexJson.optJsonObject(CommonSearchFields.CONTACT), contactFieldKeys);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                addEntityJsonForIndexCards(indexJson, matchedResults);
            }
            case CommonSearchFields.ACTIVITY -> {
                loadActivitySubFieldsJson(indexJson, null, matchedResults);
                loadSubEntityFields(indexJson.optJsonObject(CommonSearchFields.CONTACT), contactFieldKeys);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                addEntityJsonForIndexCards(indexJson, matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
        }
        matchedResults.put(CommonSearchFields.ID, indexJson.getLong(CommonSearchFields.ID));
        matchedResults.put(CommonSearchFields.NAME, indexJson.getString(CommonSearchFields.NAME));
    }

    /**
     * <p>
     * Construct results from fetched docs for search response.
     * </p>
     *
     * @param searchText User searched text.
     * @param entities   Fetched docs.
     * @param results    Json to put modified fetched results.
     * @param counter    {@link Counter}.
     */
    protected void loadResultsForSearchResponse(final String searchText, final JsonObject entities,
                                                final JsonArray results, final Counter counter) {

        for (final Map.Entry<String, Object> entitiesEntry : entities.toMap().entrySet()) {
            final String index = entitiesEntry.getKey();
            final JsonArray indexArray = entities.getJsonArray(index);
            final int size = indexArray.size();

            for (int count = 0; count < size; count++) {
                final JsonObject indexJson = indexArray.getJsonObject(count);
                final JsonObject dataJson = Json.object();

                counter.increment();
                dataJson.put(CommonSearchFields.INDEX, index);
                constructResults(searchText, index, indexJson, dataJson);
                indexJson.remove(CommonSearchFields.NOTES);
                indexJson.remove(CommonSearchFields.CUSTOM_FIELDS);
                dataJson.put(CommonSearchFields.DETAILS, indexJson);
                results.put(dataJson);
            }
        }
    }

    /**
     * <p>
     * Loads owner name into matched results.
     * </p>
     *
     * @param indexJson      {@link JsonObject}.
     * @param matchedResults {@link JsonObject}.
     */
    protected void loadOwnerName(final JsonObject indexJson, final JsonObject matchedResults) {
        final Long orgId = indexJson.getLong(CommonSearchFields.ORG_ID);
        final Long ownerId = indexJson.containsKey(CommonSearchFields.OWNER_ID) ?
                indexJson.getLong(CommonSearchFields.OWNER_ID) : null;

        if (Objects.nonNull(ownerId)) {
            final CrmUser owner = userService.getUser(orgId, ownerId);

            if (Objects.nonNull(owner)) {
                matchedResults.put(CommonSearchFields.OWNER, owner.getFullName());
            }
        }
    }

    /**
     * <p>
     * Processes the search request.
     * </p>
     *
     * @param orgId       An organization's id.
     * @param data        Request's data.
     * @param crmResponse {@link CrmResponse}.
     */
    private void processSearchRequest(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final String searchText = getTrimmedSearchText(data);
        final String indexValue = data.getString(CommonSearchFields.INDEX);
        final JsonObject rangeJson = data.optJsonObject(CommonSearchFields.RANGE);
        final int from;
        final int docsToFetch;

        if (Objects.isNull(rangeJson)) {
            from = NumericalConstant.ONE;
            docsToFetch = NumericalConstant.TWENTY_FIVE;
        } else {
            from = rangeJson.containsKey(CommonSearchFields.FROM) ? rangeJson.getInt(CommonSearchFields.FROM) : NumericalConstant.ONE;
            docsToFetch = rangeJson.containsKey(CommonSearchFields.COUNT) ? rangeJson.getInt(CommonSearchFields.COUNT) : NumericalConstant.TWENTY_FIVE;
        }
        final boolean isValidSearchRequest = isValidSearchRequest(indexValue, from, docsToFetch);

        if (isValidSearchRequest) {
            final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
            final JsonObject entities;
            final long totalRecords;

            if (CommonSearchFields.ALL.equalsIgnoreCase(indexValue)) {
                final Counter totalDocCounter = new Counter();
                final MultiSingleIndexRequest multiSearchRequest = buildSearchRequests(
                        searchText, from, docsToFetch, totalDocCounter);

                if (Objects.isNull(multiSearchRequest)) {
                    crmResponse.put(CommonSearchFields.ASSOCIATED_FIELDS, getAssociatedFields(orgId, indexValue, true));
                    crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest());
                } else {
                    totalRecords = totalDocCounter.getCount();
                    entities = readableDocumentService.multiSearchGroupedByIndex(multiSearchRequest);

                    if (NumericalConstant.ZERO == entities.size()) {
                        crmResponse.put(CommonSearchFields.ASSOCIATED_FIELDS, getAssociatedFields(orgId, indexValue, true));
                        crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest());
                    } else {
                        crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest(from, totalRecords, entities, searchText));
                    }
                }
            } else {
                final SingleIndexSearchRequest singleIndexSearchRequest = build(indexValue, searchText, from, docsToFetch);

                if (Objects.isNull(singleIndexSearchRequest)) {
                    crmResponse.put(CommonSearchFields.ASSOCIATED_FIELDS, getAssociatedFields(orgId, indexValue, false));
                    crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest());
                } else {
                    totalRecords = singleIndexSearchRequest.getSearchRequest().totalDocCount();
                    entities = readableDocumentService.search(singleIndexSearchRequest);
                    crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest(
                            from, totalRecords, entities, searchText));
                }
            }
        }
    }

    /**
     * <p>
     * Builds recently viewed Json.
     * </p>
     *
     * @param isPermissible      whether full details are permissible.
     * @param index              Represents index's name.
     * @param recentlyViewedJson Json holding recently viewed json.
     * @param indexJson          Json holding entity.
     * @return @{@link JsonObject}.
     */
    private JsonObject buildRecentlyViewedResponse(final boolean isPermissible, final String index, final JsonObject indexJson,
                                                   final JsonObject recentlyViewedJson) {
        final JsonObject matchedResults = Json.object();

        if (isPermissible) {
            loadMatchedResultsForRecentlyViewedEntities(index, indexJson, matchedResults);
            recentlyViewedJson.put(CommonSearchFields.DETAILS, indexJson);
        } else {
            recentlyViewedJson.put(CommonSearchFields.DETAILS, Json.object());
            indexJson.remove(CommonSearchFields.ID);
            loadMatchedResultsForNonAccessibleDoc(index, indexJson, matchedResults);
        }
        recentlyViewedJson.put(CommonSearchFields.IS_PERMISSIBLE, isPermissible);

        return matchedResults;
    }

    /**
     * <p>
     * Builds search requests for all indexes.
     * </p>
     *
     * @param searchText  Searched text.
     * @param start       Indicates the point start where doc should be fetched.
     * @param docsToFetch Total amount of docs to be fetched.
     * @return {@link MultiSingleIndexRequest}.
     */
    private MultiSingleIndexRequest buildSearchRequests(final String searchText, final Integer start,
                                                        final Integer docsToFetch, final Counter totalDocCounter) {

        return searchAccessController.build(null, searchText, start, docsToFetch, totalDocCounter);
    }

    /**
     * <p>
     * Fetches the associated fields for the given indexes
     * </p>
     *
     * @param indexValue Which contains index value.
     * @return A {@link JsonArray} containing associated fields
     */
    private JsonArray getAssociatedFields(final Long orgId, final String indexValue, final boolean areAllIndexesSelected) {
        final JsonArray jsonArray = Json.array();
        final String sourceKey = CommonSearchFields.ENTITY_SOURCE;
        final String associatedFieldKey = CommonSearchFields.ASSOCIATED_FIELD;
        final String nameKey = CommonSearchFields.NAME;
        final BiConsumer<String, String> associatedFieldHandler = (source, aliasName) -> {

            if (isAccessible(orgId, source)) {
                final JsonObject associatedFieldJson = Json.object();

                associatedFieldJson.put(sourceKey, source);
                associatedFieldJson.put(associatedFieldKey, fieldService.groupDefaultFieldsByAliasName(source,
                        aliasName).get(aliasName).id());
                jsonArray.put(associatedFieldJson);
            }
        };

        if (areAllIndexesSelected) {
            associatedFieldHandler.accept(CONTACT_SOURCE, CommonSearchFields.FIRST_NAME);
            associatedFieldHandler.accept(COMPANY_SOURCE, nameKey);
            associatedFieldHandler.accept(DEAL_SOURCE, CommonSearchFields.TITLE);
            associatedFieldHandler.accept(PRODUCT_SOURCE, CommonSearchFields.NAME);
        } else {
            final String sourceName = StringUtil.capitalizeFirstLetter(indexValue).orElseGet(() -> null);
            final SearchIndex searchIndex = SearchIndex.getSearchIndex(indexValue);

            switch (Objects.requireNonNull(searchIndex)) {
                case CONTACT -> associatedFieldHandler.accept(sourceName, CommonSearchFields.FIRST_NAME);
                case COMPANY, PRODUCT -> associatedFieldHandler.accept(sourceName, nameKey);
                case DEAL -> associatedFieldHandler.accept(sourceName, CommonSearchFields.TITLE);
            }
        }
        return jsonArray;
    }

    /**
     * <p>
     * Builds response for the search request..
     * </p>
     *
     * @param totalRecords Total records that's present cumulatively.
     * @param entities     A json that holds all retrieved documents.
     * @param searchText   Represents the search term.
     * @return response in {@link JsonObject}.
     */
    private JsonObject buildResponseForSearchRequest(final int from, final long totalRecords, final JsonObject entities,
                                                       final String searchText) {
        final JsonObject table = Json.object();
        final JsonArray results = Json.array();
        final Counter entitiesCount = new Counter();

        loadResultsForSearchResponse(searchText, entities, results, entitiesCount);
        final int size = entitiesCount.getCount();

        table.put(CommonSearchFields.HAS_MORE, (from + size) <= totalRecords);
        table.put(CommonSearchFields.TOTAL_RECORDS, totalRecords);
        table.put(CommonSearchFields.SIZE, size);
        table.put(CommonSearchFields.RESULTS, results);

        return table;
    }

    /**
     * <p>
     * Constructs data & matched results.
     * </p>
     *
     * @param searchText Represents the search text.
     * @param index      Represents index name.
     * @param indexJson  A json holding index documents.
     * @param dataJson   A json holding data.
     */
    private void constructResults(final String searchText, final String index, final JsonObject indexJson, final JsonObject dataJson) {
        final JsonObject matchedResults = Json.object();
        final String lowerCaseSearchText = Objects.isNull(searchText) ? null : searchText.toLowerCase();
        final Collection<String> contactFieldKeys = List.of(CommonSearchFields.EMAILS, CommonSearchFields.PHONES);
        final BiConsumer<String, String> matchingResultsBuilder = (fieldKey, fieldValueKey) -> {

            if (indexJson.containsKey(fieldKey)) {
                final JsonArray fieldArray = indexJson.getJsonArray(fieldKey);
                final JsonObject matchedSubEntityJson = buildMatchingSubEntityJson(lowerCaseSearchText, fieldArray, fieldValueKey);

                if (Objects.nonNull(matchedSubEntityJson)) {
                    matchedResults.put(fieldKey, matchedSubEntityJson);
                }
            }
        };

        switch (index) {
            case CommonSearchFields.CONTACT -> {
                loadAddress(searchText, indexJson, matchedResults);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                matchingResultsBuilder.accept(CommonSearchFields.NOTES, CommonSearchFields.NOTE);
                matchingResultsBuilder.accept(CommonSearchFields.EMAILS, CommonSearchFields.VALUE);
                matchingResultsBuilder.accept(CommonSearchFields.PHONES, CommonSearchFields.NUMBER);
                matchingResultsBuilder.accept(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME);
                loadEntityDetailResponse(indexJson, matchedResults, Map.of(
                        CommonSearchFields.EMAILS, CommonSearchFields.VALUE, CommonSearchFields.PHONES, CommonSearchFields.NUMBER));
                addEntityJsonForIndexCards(indexJson, matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.COMPANY -> {
                loadAddress(lowerCaseSearchText, indexJson, matchedResults);
                matchingResultsBuilder.accept(CommonSearchFields.NOTES, CommonSearchFields.NOTE);
                matchingResultsBuilder.accept(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME);
                loadSingleMatchingField(CommonSearchFields.WEBSITE, lowerCaseSearchText, indexJson.optString(CommonSearchFields.COMPANY, null), matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.DEAL -> {
                final JsonObject contactJson = indexJson.optJsonObject(CommonSearchFields.CONTACT);

                loadSubEntityFields(contactJson, contactFieldKeys);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                matchingResultsBuilder.accept(CommonSearchFields.NOTES, CommonSearchFields.NOTE);
                matchingResultsBuilder.accept(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME);
                addEntityJsonForIndexCards(indexJson, matchedResults);
                setDealStateInMatchedResults(indexJson.getJsonObject(CommonSearchFields.DEAL_STATE), matchedResults);
                setDealArchiveStateInMatchedResults(indexJson, matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.PRODUCT -> {
                matchingResultsBuilder.accept(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME);
                matchedResults.put(CommonSearchFields.PRODUCT_CODE, indexJson.optString(CommonSearchFields.PRODUCT_CODE, null));
                loadOwnerName(indexJson, matchedResults);
            }
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                loadSubEntityFields(indexJson.optJsonObject(CommonSearchFields.CONTACT), contactFieldKeys);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                addEntityJsonForIndexCards(indexJson, matchedResults);
            }
            case CommonSearchFields.ACTIVITY -> {
                loadSubEntityFields(indexJson.optJsonObject(CommonSearchFields.CONTACT), contactFieldKeys);
                loadAddress(null, indexJson.optJsonObject(CommonSearchFields.COMPANY), null);
                loadActivitySubFieldsJson(indexJson, lowerCaseSearchText, matchedResults);
                addEntityJsonForIndexCards(indexJson, matchedResults);
                loadOwnerName(indexJson, matchedResults);
            }
        }
        dataJson.put(CommonSearchFields.MATCHED_RESULTS, matchedResults);
        matchedResults.put(CommonSearchFields.NAME, indexJson.getString(CommonSearchFields.NAME));
        matchedResults.put(CommonSearchFields.ID, indexJson.getLong(CommonSearchFields.ID));
    }

    /**
     * <p>
     * Gets {@link Index} in an array.
     * </p>
     *
     * @param orgId       An organization's id.
     * @param crmResponse {@link CrmResponse}.
     * @return {@link JsonArray}.
     */
    private JsonArray getIndexArray(final Long orgId, final CrmResponse crmResponse) {
        final Map<Long, Index> indexMap = searchIndexService.getAll();
        final JsonArray indexArray = Json.array();
        final Counter accessibilityCount = new Counter();

        for (final Index index : indexMap.values()) {
            final String name = index.name();

            switch (name) {
                case CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.DEAL -> {
                    if (searchAccessController.isAccessible(orgId, name)) {
                        accessibilityCount.increment();
                        indexArray.put(getIndexJson(name, index, crmResponse));
                    }
                }
                case CommonSearchFields.PRODUCT -> {
                    if (searchAccessController.isAccessible(orgId, name)) {
                        indexArray.put(getIndexJson(name, index, crmResponse));
                    }
                }
                case CommonSearchFields.ACTIVITY -> indexArray.put(getIndexJson(name, index, crmResponse));
                case CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                    if (NumericalConstant.ZERO < accessibilityCount.getCount()) {
                        indexArray.put(getIndexJson(name, index, crmResponse));
                    }
                }
            }
        }

        return indexArray;
    }

    /**
     * <p>
     * Loads activity sub fields such as activity type & note in matched results.
     * </p>
     *
     * @param activityJson   A json holding activity related details.
     * @param searchText     Represents the search term.
     * @param matchedResults A json to hold matched results.
     */
    private void loadActivitySubFieldsJson(final JsonObject activityJson, final String searchText,
                                             final JsonObject matchedResults) {
        final JsonObject note = activityJson.optJsonObject(CommonSearchFields.NOTE);

        if (Objects.nonNull(searchText) && Objects.nonNull(note)) {
            final String activityNote = note.getString(CommonSearchFields.NAME);

            if (activityNote.toLowerCase().contains(searchText)) {
                matchedResults.put(CommonSearchFields.NOTE, activityNote);
            }
        }
        matchedResults.put(CommonSearchFields.NAME, activityJson.getString(CommonSearchFields.NAME));
        matchedResults.put(CommonSearchFields.ACTIVITY_TYPE, activityJson.optJsonObject(CommonSearchFields.ACTIVITY_TYPE));
        matchedResults.put(CommonSearchFields.IS_OVERDUE, Status.OVERDUE == Status.valueOf(activityJson.getString(CommonSearchFields.STATUS)));
        matchedResults.put(CommonSearchFields.IS_DONE, activityJson.getBoolean(CommonSearchFields.IS_DONE));
        activityJson.remove(CommonSearchFields.IS_DONE);
        activityJson.remove(CommonSearchFields.STATUS);
    }

    /**
     * <p>
     * Load single matching fields into matched fields json.
     * </p>
     *
     * @param fieldKey       Represents the field key.
     * @param searchText     Represents the search text.
     * @param value          Represents the field value.
     * @param matchedResults A json to hold matched results.
     */
    private void loadSingleMatchingField(final String fieldKey, final String searchText, final String value,
                                           final JsonObject matchedResults) {

        if (Objects.nonNull(value) && value.toLowerCase().contains(searchText)) {
            matchedResults.put(fieldKey, value);
        }
    }

    /**
     * <p>
     * Adds Entity Json.
     * </p>
     *
     * @param indexJson      A json holding index related values.
     * @param matchedResults A json to hold matched results.
     */
    private void addEntityJsonForIndexCards(final JsonObject indexJson, final JsonObject matchedResults) {
        final Consumer<String> entityAdder = (key) -> {
            final Object entity = indexJson.opt(key);

            if (Objects.nonNull(entity) && entity instanceof Map<?, ?> map && NumericalConstant.ZERO < map.size()) {
                final JsonObject entityJson = indexJson.optJsonObject(key);

                matchedResults.put(key, Map.of(CommonSearchFields.ID, entityJson.getLong(CommonSearchFields.ID),
                        CommonSearchFields.NAME, entityJson.getString(CommonSearchFields.NAME)));
            }
        };

        entityAdder.accept(CommonSearchFields.CONTACT);
        entityAdder.accept(CommonSearchFields.COMPANY);
        entityAdder.accept(CommonSearchFields.DEAL);
    }

    /**
     * <p>
     * Sets state in matched results.
     * </p>
     *
     * @param dealStateJson  A json to hold deal state details.
     * @param matchedResults A json to hold matched results.
     */
    private void setDealStateInMatchedResults(final JsonObject dealStateJson, final JsonObject matchedResults) {
        final String state = dealStateJson.getString(CommonSearchFields.NAME);

        if (!Objects.equals(CommonSearchFields.OPEN, state)) {
            matchedResults.put(CommonSearchFields.STATE, state);
        }
    }

    /**
     * <p>
     * Sets archive state in matched results.
     * </p>
     *
     * @param indexJson      A json to hold deal index details.
     * @param matchedResults A json to hold matched results.
     */
    private void setDealArchiveStateInMatchedResults(final JsonObject indexJson, final JsonObject matchedResults) {
        final boolean isArchived = indexJson.optBoolean(CommonSearchFields.IS_ARCHIVED);

        matchedResults.put(CommonSearchFields.IS_ARCHIVED, isArchived);
        indexJson.remove(CommonSearchFields.IS_ARCHIVED);
    }

    /**
     * <p>
     * Load address field into index & matched results json.
     * </p>
     *
     * @param searchText     Represents the search text.
     * @param entityJson     A json to hold index details.
     * @param matchedResults A json to hold matched results.
     */
    private void loadAddress(final String searchText, final JsonObject entityJson, final JsonObject matchedResults) {

        if (Objects.nonNull(entityJson)) {
            final JsonObject addressJson = entityJson.optJsonObject(CommonSearchFields.ADDRESS);

            if (!Json.isEmpty(addressJson)) {
                entityJson.put(CommonSearchFields.ADDRESS, buildAddressForSearchResults(searchText, addressJson, matchedResults));
            }
        }
    }

    /**
     * <p>
     * Builds the address for search results such as details & matching fields.
     * </p>
     *
     * @param searchText     Represents the search text.
     * @param addressJson    A json to hold address details.
     * @param matchedResults A json to hold matched results.
     * @return String formatted address.
     */
    private String buildAddressForSearchResults(final String searchText, final JsonObject addressJson,
                                                  final JsonObject matchedResults) {
        final StringBuilder addressBuilder = new StringBuilder();

        final Consumer<String> addressBuilderConsumer = (fieldKey) -> {
            if (addressJson.containsKey(fieldKey)) {
                final String value = addressJson.getString(fieldKey);

                if (Objects.nonNull(value)) {
                    if (!addressBuilder.isEmpty()) {
                        addressBuilder.append(", ");
                    }
                    addressBuilder.append(value);
                }
            }
        };

        addressBuilderConsumer.accept(CommonSearchFields.ADDRESS_LINE_1);
        addressBuilderConsumer.accept(CommonSearchFields.ADDRESS_LINE_2);
        addressBuilderConsumer.accept(CommonSearchFields.CITY);
        addressBuilderConsumer.accept(CommonSearchFields.STATE);
        addressBuilderConsumer.accept(CommonSearchFields.COUNTRY);
        addressBuilderConsumer.accept(CommonSearchFields.ZIPCODE);
        final String address = addressBuilder.toString();

        if (Objects.nonNull(matchedResults) && Objects.nonNull(searchText) && address.toLowerCase().contains(searchText)) {
            matchedResults.put(CommonSearchFields.ADDRESS, address);
        }

        return address;
    }
}