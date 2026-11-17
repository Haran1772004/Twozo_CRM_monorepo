package com.tlc.crm.entity.search.action.handler.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.security.search.SearchAccessController;
import com.tlc.crm.entity.internal.status.CommonSearchErrorCodes;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntriesContext;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntry;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.fields.SearchApiKeys;
import com.tlc.crm.entity.service.registry.ActivityStatusFetchServiceRegistry;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles mobile client requests for search api.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class SearchMobileClient extends AbstractSearchClient {

    private final SearchAccessController searchAccessController;
    private final ActivityStatusFetchServiceRegistry activityStatusFetchServiceRegistry;

    private static class Instance {
        private static final SearchMobileClient INSTANCE = new SearchMobileClient();
    }

    private SearchMobileClient() {
        searchAccessController = SearchAccessController.getMobileClientController();
        activityStatusFetchServiceRegistry = ActivityStatusFetchServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Gets {@link SearchMobileClient}.
     * </p>
     *
     * @return {@link SearchMobileClient}.
     */
    public static SearchMobileClient getInstance() {
        return SearchMobileClient.Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles search requests for mobile client.
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
     * Populates mobile recently viewed entry using mobile matched-results layout.
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
        loadRecentlyViewedResponse(isPermissible, indexName, documentJson, recentlyViewedJson);
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
     * @return {@link MultiSingleIndexRequest}.
     */
    protected RecentlyViewedEntriesContext build(final long orgId, final long userId) {

        return searchAccessController.build(RecentlyViewedEntry.toIndexOrderedIds(
                commonSearchManager.getRecentlyViewedEntriesInOrder(orgId, userId, true)));
    }

    /**
     * <p>
     * Constructs fetched docs into matched results doc for recently viewed response.
     * </p>
     *
     * @param index          Represents the index's name.
     * @param indexJson      Fetched docs.
     * @param matchedResults Json to put matched results.
     */
    protected void loadMatchedResultsForRecentlyViewedEntities(final String index, final JsonObject indexJson,
                                                               final JsonObject matchedResults) {
        final BaseCrmUser loggedInUserInfo = LoggedInUserInfo.getUser();
        final TimezoneInfo timezoneInfo = ((UserRegionalPreference) userService.getUser(loggedInUserInfo.orgId(),
                loggedInUserInfo.id()).getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE)).getTimezoneInfo();

        constructResults(index, indexJson, timezoneInfo, loggedInUserInfo.id());
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
    protected void loadResultsForSearchResponse(final String searchText, final JsonObject entities, final JsonArray results,
                                                final Counter counter) {
        final BaseCrmUser loggedInUserInfo = LoggedInUserInfo.getUser();
        final TimezoneInfo timezoneInfo = ((UserRegionalPreference) userService.getUser(loggedInUserInfo.orgId(),
                loggedInUserInfo.id()).getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE)).getTimezoneInfo();

        for (final Map.Entry<String, Object> entitiesEntry : entities.toMap().entrySet()) {
            final String index = entitiesEntry.getKey();
            final JsonArray indexArray = entities.getJsonArray(index);
            final int size = indexArray.size();

            for (int count = 0; count < size; count++) {
                final JsonObject indexJson = indexArray.getJsonObject(count);

                counter.increment();
                constructResults(index, indexJson, timezoneInfo, loggedInUserInfo.id());
                results.put(indexJson);
            }
        }
    }

    /**
     * <p>
     * Loads owner name into index json.
     * </p>
     *
     * @param indexJson      {@link JsonObject}.
     * @param matchedResults Json to put matched results.
     */
    protected void loadOwnerName(final JsonObject indexJson, final JsonObject matchedResults) {
        final Long orgId = indexJson.getLong(CommonSearchFields.ORG_ID);
        final Long ownerId = indexJson.containsKey(CommonSearchFields.OWNER_ID) ?
                indexJson.getLong(CommonSearchFields.OWNER_ID) : null;

        if (Objects.nonNull(ownerId)) {
            final CrmUser owner = userService.getUser(orgId, ownerId);

            if (Objects.nonNull(owner)) {
                indexJson.put(CommonSearchFields.OWNER, owner.getFullName());
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
     */
    private void loadRecentlyViewedResponse(final boolean isPermissible, final String index, final JsonObject indexJson,
                                            final JsonObject recentlyViewedJson) {
        final JsonObject matchedResults = Json.object();

        if (isPermissible) {
            loadMatchedResultsForRecentlyViewedEntities(index, indexJson, matchedResults);

            indexJson.toMap().forEach(recentlyViewedJson::put);
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
        final JsonArray indexes = data.getJsonArray(CommonSearchFields.INDEXES);
        final boolean isMultiIndexesReq = NumericalConstant.ONE < indexes.size();

        if (isMultiIndexesReq) {
            final Collection<String> indexNames = indexes.toList().stream().map(indexName -> (String) indexName).toList();

            validateSearchRequest(indexNames);
            final MultiSingleIndexRequest multiSearchRequest = build(indexNames, searchText);

            if (Objects.isNull(multiSearchRequest)) {
                crmResponse.put(CommonSearchFields.RESULTS, Json.object());
            } else {
                final Map<String, Integer> countMap = multiSearchRequest.get().stream().collect(Collectors.toMap(
                        SingleIndexSearchRequest::getIndexName, singleIndexSearchRequest ->
                                singleIndexSearchRequest.getSearchRequest().totalDocCount(), (a, b) -> a, LinkedHashMap::new));
                final JsonObject retrievedRecords = service.getReadableService(orgId).multiSearchGroupedByIndex(multiSearchRequest);

                if (NumericalConstant.ZERO == retrievedRecords.size()) {
                    crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest());
                } else {
                    crmResponse.put(CommonSearchFields.TABLE, buildResultsForMultiIndexSearchResponse(
                            retrievedRecords, countMap));
                }
            }
        } else {
            final String indexName = indexes.getString(NumericalConstant.ZERO);
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
            final boolean isValidSearchRequest = isValidSearchRequest(indexName, from, docsToFetch);

            if (isValidSearchRequest) {
                final SingleIndexSearchRequest singleIndexRequest = build(indexName, searchText, from, docsToFetch);

                if (Objects.isNull(singleIndexRequest)) {
                    crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest());
                } else {
                    final long totalRecords = singleIndexRequest.getSearchRequest().totalDocCount();
                    final JsonObject entities = service.getReadableService(orgId).search(singleIndexRequest);

                    crmResponse.put(CommonSearchFields.TABLE, buildResponseForSearchRequest(
                            from, indexName, totalRecords, entities, searchText));
                }
            }
        }
    }

    /**
     * <p>
     * Builds search requests for all indexes.
     * </p>
     *
     * @param indexes    Index names.
     * @param searchText Searched text.
     * @return {@link MultiSingleIndexRequest}.
     */
    private MultiSingleIndexRequest build(final Collection<String> indexes, final String searchText) {

        return searchAccessController.build(
                indexes, searchText, NumericalConstant.ZERO, NumericalConstant.FOUR, null);
    }

    /**
     * <p>
     * Checks whether the search request is valid.
     * </p>
     *
     * @param indexes Indexes name.
     */
    private void validateSearchRequest(final Collection<String> indexes) {
        final Collection<String> presetIndexNames = getIndexNames();

        for (final String index : indexes) {
            if (!presetIndexNames.contains(index)) {
                throw ErrorCode.get(CommonSearchErrorCodes.INVALID_INDEX, "i18n.invalid.index");
            }
        }
    }

    /**
     * <p>
     * Construct results from fetched docs for search response.
     * </p>
     *
     * @param retrievedDocs Fetched docs.
     * @param countMap      Index and it's count map.
     */
    private JsonObject buildResultsForMultiIndexSearchResponse(final JsonObject retrievedDocs, final Map<String, Integer> countMap) {
        final JsonObject results = Json.orderedObject();
        final BaseCrmUser loggedInUserInfo = LoggedInUserInfo.getUser();
        final TimezoneInfo timezoneInfo = ((UserRegionalPreference) userService.getUser(loggedInUserInfo.orgId(),
                loggedInUserInfo.id()).getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE)).getTimezoneInfo();

        for (final Map.Entry<String, Object> entitiesEntry : retrievedDocs.toMap().entrySet()) {
            final String index = entitiesEntry.getKey();
            final JsonObject indexJson = Json.object();
            final JsonArray indexArray = Json.array();
            final JsonArray docArray = retrievedDocs.getJsonArray(index);
            final int size = docArray.size();

            for (int count = 0; count < size; count++) {
                final JsonObject docJson = docArray.getJsonObject(count);

                constructResults(index, docJson, timezoneInfo, loggedInUserInfo.id());
                indexArray.put(docJson);
            }
            indexJson.put(CommonSearchFields.RESULTS, indexArray);
            indexJson.put(CommonSearchFields.HAS_MORE, NumericalConstant.FOUR < countMap.get(index));
            results.put(StringUtil.joinByCamelCase(index, CommonSearchFields.RESULTS), indexJson);
        }

        return results;
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
    private JsonObject buildResponseForSearchRequest(final int from, final String index, final long totalRecords,
                                                     final JsonObject entities, final String searchText) {
        final JsonObject table = Json.object();
        final JsonArray results = Json.array();
        final Counter entitiesCount = new Counter();

        loadResultsForSearchResponse(searchText, entities, results, entitiesCount);
        final int size = entitiesCount.getCount();

        table.put(CommonSearchFields.HAS_MORE, (from + size) <= totalRecords);
        table.put(CommonSearchFields.TOTAL_RECORDS, totalRecords);
        table.put(CommonSearchFields.SIZE, size);
        table.put(CommonSearchFields.RESULTS, results);

        return Json.object().put(StringUtil.joinByCamelCase(index, CommonSearchFields.RESULTS), table);
    }

    /**
     * <p>
     * Constructs response.
     * </p>
     *
     * @param index     Represents index name.
     * @param indexJson A json holding index documents.
     */
    private void constructResults(final String index, final JsonObject indexJson, final TimezoneInfo timezoneInfo, final long currentUser) {
        loadOwnerName(indexJson, null);

        switch (index) {
            case CommonSearchFields.CONTACT, CommonSearchFields.COMPANY -> loadProfileUrl(
                    indexJson.containsKey(CommonSearchFields.PROFILE_URL) ?
                            indexJson.getString(CommonSearchFields.PROFILE_URL) : null, indexJson);
            case CommonSearchFields.DEAL -> {
                final JsonObject dealValueJson = indexJson.optJsonObject(CommonSearchFields.DEAL_VALUE);
                final JsonObject pipelineStageJson = indexJson.optJsonObject(CommonSearchFields.PIPELINE_STAGE);

                if (Objects.nonNull(dealValueJson) && dealValueJson.containsKey(CommonSearchFields.CURRENCY)) {
                    dealValueJson.put(CommonSearchFields.CURRENCY, Currency.getSymbol(dealValueJson.getString(CommonSearchFields.CURRENCY)));
                }

                if (Objects.nonNull(pipelineStageJson)) {
                    indexJson.put(CommonSearchFields.PIPELINE_STAGE_ID, pipelineStageJson.getLong(CommonSearchFields.ID));
                }
                activityStatusFetchServiceRegistry.opt(FieldKeys.ACTIVITY_STATUS).ifPresent(activityStatusIconService ->
                        indexJson.put(CommonSearchFields.ACTIVITY_ICON, activityStatusIconService.getIconTypeName(
                                indexJson.getLong(CommonSearchFields.ORG_ID), indexJson.getLong(CommonSearchFields.ID), Source.DEAL, timezoneInfo)));
            }
            case CommonSearchFields.ACTIVITY -> {
                handleCheckInOut(indexJson, currentUser);
            }
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                final JsonObject entityJson = Json.object();

                entityJson.put(CommonSearchFields.ID, indexJson.getLong(CommonSearchFields.ENTITY_SOURCE_ID));
                entityJson.put(CommonSearchFields.NAME, indexJson.getString(CommonSearchFields.ENTITY_SOURCE_NAME));
                indexJson.put(indexJson.getString(CommonSearchFields.ENTITY_SOURCE), entityJson);
                indexJson.remove(CommonSearchFields.ENTITY_SOURCE_ID);
                indexJson.remove(CommonSearchFields.ENTITY_SOURCE_NAME);
                indexJson.remove(CommonSearchFields.ENTITY_SOURCE);
            }
        }
    }

    /**
     * <p>
     * Loads profile url.
     * </p>
     *
     * @param url       profile url.
     * @param indexJson {@link JsonObject}.
     */
    private void loadProfileUrl(final String url, final JsonObject indexJson) {

        if (Objects.nonNull(url)) {
            indexJson.put(CommonSearchFields.THUMBNAIL, Json.object().put(CommonSearchFields.URL, url));
        }
    }

    /**
     * <p>
     * Loads primary emails into the entity json.
     * </p>
     *
     * @param entityJson {@link JsonObject}.
     */
    private void loadPrimaryEmails(final JsonObject entityJson) {

        if (Objects.nonNull(entityJson) && entityJson.containsKey(CommonSearchFields.PRIMARY_EMAIL)) {
            final JsonObject emailJson = entityJson.optJsonObject(CommonSearchFields.PRIMARY_EMAIL);
            final JsonArray emailsArray = entityJson.optJsonArray(CommonSearchFields.EMAILS);

            if (Objects.nonNull(emailsArray) && emailsArray.size() > NumericalConstant.ZERO) {
                emailJson.put(CommonSearchFields.COUNT, emailsArray.size() - NumericalConstant.ONE);
            }

            if (!Json.isEmpty(emailJson)) {
                entityJson.put(CommonSearchFields.PRIMARY_EMAIL, emailJson);
            }
            entityJson.remove(CommonSearchFields.EMAILS);
        }
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

        for (final Index index : indexMap.values()) {
            final String name = index.name();

            switch (name) {
                case CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.DEAL,
                     CommonSearchFields.PRODUCT -> {
                    if (searchAccessController.isAccessible(orgId, name)) {
                        indexArray.put(getIndexJson(name, index, crmResponse));
                    }
                }
                case CommonSearchFields.ACTIVITY -> indexArray.put(getIndexJson(name, index, crmResponse));
            }
        }

        return indexArray;
    }

    /**
     * Handles check-in, check-out, time spent, and overdue status updates for an activity.
     */
    private void handleCheckInOut(JsonObject indexJson, Long currentUser) {

        final Long ownerId = indexJson.containsKey(CommonSearchFields.OWNER_ID) ? indexJson.getLong(CommonSearchFields.OWNER_ID) : null;
        final boolean isOwner = Objects.equals(currentUser, ownerId);
        final boolean isCheckedIn = indexJson.containsKey(CommonSearchFields.CHECK_IN_INFO);
        final boolean isCheckedOut = indexJson.containsKey(CommonSearchFields.CHECK_OUT_INFO);

        indexJson.put(CommonSearchFields.REQUIRE_CHECK_IN, false);
        indexJson.put(CommonSearchFields.REQUIRE_CHECK_OUT, false);

        if (isOwner) {
            if (!isCheckedIn) {
                indexJson.put(CommonSearchFields.REQUIRE_CHECK_IN, true);
            } else if (!isCheckedOut) {
                indexJson.put(CommonSearchFields.REQUIRE_CHECK_OUT, true);
            }
        }

        if (isCheckedIn && isCheckedOut) {
            final Long checkInTime = indexJson.getLong(CommonSearchFields.CHECK_IN_TIME);
            final Long checkOutTime = indexJson.getLong(CommonSearchFields.CHECK_OUT_TIME);

            if (Objects.nonNull(checkInTime) && Objects.nonNull(checkOutTime)) {
                indexJson.put(CommonSearchFields.TIME_SPENT, checkOutTime - checkInTime);
            }

            indexJson.remove(CommonSearchFields.CHECK_IN_INFO);
            indexJson.remove(CommonSearchFields.CHECK_IN_TIME);
            indexJson.remove(CommonSearchFields.CHECK_OUT_TIME);
        }

        indexJson.put(CommonSearchFields.IS_OVERDUE, Status.OVERDUE == Status.valueOf(indexJson.getString(CommonSearchFields.STATUS)));
        indexJson.remove(CommonSearchFields.STATUS);
    }
}
