package com.tlc.crm.entity.search.action.handler.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.internal.status.CommonSearchErrorCodes;
import com.tlc.crm.entity.search.action.handler.SearchClient;
import com.tlc.crm.entity.search.api.manager.CommonSearchManager;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntry;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntriesContext;
import com.tlc.crm.entity.search.api.model.ViewedEntries;
import com.tlc.crm.entity.search.api.model.converter.CommonSearchConverter;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.service.SearchIndexService;
import com.tlc.search.SearchService;
import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.provider.SearchServiceProvider;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * An abstract way to handle search client.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
abstract class AbstractSearchClient implements SearchClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSearchClient.class);

    protected final SearchIndexService searchIndexService;
    protected final CommonSearchManager commonSearchManager;
    protected final UserService userService;
    protected final SearchService service;

    private final CommonSearchConverter commonSearchConverter;
    private final ModelValidator modelValidator;

    AbstractSearchClient() {
        searchIndexService = SearchIndexService.getInstance();
        commonSearchManager = CommonSearchManager.getInstance();
        commonSearchConverter = CommonSearchConverter.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        service = SearchServiceProvider.get();
        userService = UserService.getInstance();
    }

    /**
     * <p>
     * Builds search requests for recently viewed docs.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param userId User's id.
     * @return {@link RecentlyViewedEntriesContext}.
     */
    protected abstract RecentlyViewedEntriesContext build(final long orgId, final long userId);

    /**
     * <p>
     * Populates a single recently viewed entry in the API response.
     * </p>
     *
     * @param indexName          Index name.
     * @param isPermissible      Whether full recently viewed details are permissible.
     * @param documentJson       Fetched document or empty object.
     * @param recentlyViewedJson Response entry to populate.
     */
    protected abstract void populateRecentlyViewedEntryResponse(final String indexName, final boolean isPermissible,
                                                                final JsonObject documentJson, final JsonObject recentlyViewedJson);

    /**
     * <p>
     * Builds {@link SingleIndexSearchRequest} for an index.
     * </p>
     *
     * @param indexValue  Represents the index key's value.
     * @param searchText  Searched text.
     * @param start       Indicates the point from where doc should be fetched.
     * @param docsToFetch Total amount of docs to be fetched.
     * @return {@link SingleIndexSearchRequest}.
     */
    protected abstract SingleIndexSearchRequest build(final String indexValue, final String searchText, final int start, final int docsToFetch);

    /**
     * <p>
     * Loads results from fetched docs for search response.
     * </p>
     *
     * @param searchText User searched text.
     * @param entities   Fetched docs.
     * @param results    Json to put modified fetched results.
     * @param counter    {@link Counter}.
     */
    protected abstract void loadResultsForSearchResponse(final String searchText, final JsonObject entities,
                                                         final JsonArray results, final Counter counter);

    /**
     * <p>
     * Loads fetched docs into matched results doc for recently viewed response.
     * </p>
     *
     * @param index          Represents the index's name.
     * @param indexJson      Fetched docs.
     * @param matchedResults Json to put matched results.
     */
    protected abstract void loadMatchedResultsForRecentlyViewedEntities(final String index, final JsonObject indexJson,
                                                                        final JsonObject matchedResults);

    /**
     * <p>
     * Whether the source's accessible.
     * </p>
     *
     * @param orgId  An organization's id.
     * @param source Source's name.
     * @return whether the source's accessible.
     */
    protected abstract boolean isAccessible(final Long orgId, final String source);

    /**
     * <p>
     * Loads owner name into matched results.
     * </p>
     *
     * @param indexJson      {@link JsonObject}.
     * @param matchedResults {@link JsonObject}.
     */
    protected abstract void loadOwnerName(final JsonObject indexJson, final JsonObject matchedResults);

    /**
     * <p>
     * Processes the recently viewed request using {@link ReadableDocumentService#multiSearchGroupedByOrder}
     * and preserves viewed-time order from {@link CommonSearchManager#getRecentlyViewedEntriesInOrder}.
     * </p>
     *
     * @param orgId       An organization's id.
     * @param userId      User's id.
     * @param crmResponse {@link CrmResponse}.
     */
    protected void processRecentlyViewedRequest(final long orgId, final long userId, final CrmResponse crmResponse) {
        final Collection<RecentlyViewedEntry> viewedEntries = commonSearchManager.getRecentlyViewedEntriesInOrder(
                orgId, userId, this instanceof SearchMobileClient);

        if (viewedEntries.isEmpty()) {
            crmResponse.put(CommonSearchFields.RECENTLY_VIEWED, Json.array());
        } else {
            final RecentlyViewedEntriesContext recentlyViewedEntriesContext = build(orgId, userId);
            final MultiSingleIndexRequest multiSearchRequest = recentlyViewedEntriesContext.multiSearchRequest();

            if (Objects.isNull(multiSearchRequest)) {
                crmResponse.put(CommonSearchFields.RECENTLY_VIEWED, Json.array());
            } else {
                final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
                final JsonArray multiSearchDocumentSlots = readableDocumentService.multiSearchGroupedByOrder(multiSearchRequest);

                crmResponse.put(CommonSearchFields.RECENTLY_VIEWED, buildResponseForRecentlyViewedDocsInOrder(
                        viewedEntries, multiSearchRequest, multiSearchDocumentSlots,
                        recentlyViewedEntriesContext.permissibleByIndex()));
            }
        }
        crmResponse.put(CommonSearchFields.RECENTLY_SEARCHED, commonSearchManager.getRecentlySearchedEntries(orgId, userId));
    }

    /**
     * <p>
     * Checks whether the search request is valid.
     * </p>
     *
     * @param index      Index name.
     * @param start      Indicates the point start where doc should be fetched.
     * @param fetchCount Total amount of docs to be fetched.
     * @return whether the search request is valid.
     */
    protected boolean isValidSearchRequest(final String index, final int start, final int fetchCount) {

        if (NumericalConstant.ZERO >= start) {
            throw ErrorCode.get(CommonSearchErrorCodes.FROM_VALUE_CANNOT_BE_ZERO_OR_NEGATIVE, "i18n.search.from.value.should.least.be.one");
        }

        if (NumericalConstant.ZERO >= fetchCount) {
            throw ErrorCode.get(CommonSearchErrorCodes.COUNT_VALUE_CANNOT_BE_ZERO_OR_NEGATIVE, "i18n.search.count.value.should.least.be.one");
        }

        if (Objects.isNull(index)) {
            throw ErrorCode.get(CommonSearchErrorCodes.INDEX_KEY_NOT_FOUND, "i18n.index.key.not.found");
        } else if (index.isEmpty()) {
            throw ErrorCode.get(CommonSearchErrorCodes.INDEX_VALUE_CANNOT_BE_EMPTY, "i18n.index.value.cannot.be.empty");
        } else {
            switch (index) {
                case CommonSearchFields.ALL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.ACTIVITY,
                        CommonSearchFields.DEAL, CommonSearchFields.PRODUCT, CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                    return true;
                }
                default -> throw ErrorCode.get(CommonSearchErrorCodes.INVALID_INDEX, "i18n.invalid.index");
            }
        }
    }

    /**
     * <p>
     * Gets list of index names.
     * </p>
     *
     * @return {@link List} of names.
     */
    protected List<String> getIndexNames() {
        return List.of(CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.COMPANY,
                CommonSearchFields.ACTIVITY, CommonSearchFields.PRODUCT, CommonSearchFields.FILE, CommonSearchFields.NOTE);
    }

    /**
     * <p>
     * Gets {@link Index} in a json.
     * </p>
     *
     * @param name        {@link Index} name.
     * @param index       {@link Index}.
     * @param crmResponse {@link CrmResponse}.
     * @return {@link JsonObject}.
     */
    protected JsonObject getIndexJson(final String name, final Index index, final CrmResponse crmResponse) {
        final JsonObject indexJson = Json.object();
        final String alternateName = index.alternateName();

        indexJson.put(CommonSearchFields.ID, index.id());
        indexJson.put(CommonSearchFields.NAME, name);
        indexJson.put(CommonSearchFields.DISPLAY_NAME, Objects.isNull(alternateName) ?
                crmResponse.resolveI18nString(index.displayName().getKey()) : alternateName);

        return indexJson;
    }

    /**
     * <p>
     * Creates the new entry for the data which is viewed
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents an user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected void saveViewedEntry(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final ViewedEntries viewedEntries = commonSearchConverter.convertDataIntoModel(orgId, userId, data);
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(viewedEntries), Group.Create.class);

        if (errors.isEmpty()) {
            final long entryId = viewedEntries.id();
            final String index = viewedEntries.getIndex();

            if (isValidData(orgId, entryId, index)) {
                final String trimmedSearchKeyword = StringUtil.trimInput(
                        viewedEntries.getSearchText(), NumericalConstant.ONE_HUNDRED_TWENTY_FIVE);

                commonSearchManager.saveViewedEntry(orgId, userId, entryId, index);
                commonSearchManager.saveSearchedEntry(orgId, userId, trimmedSearchKeyword);

                crmResponse.put(CommonSearchFields.MESSAGE, "i18n.search.viewed.data.saved.success");
            } else {
                throw ErrorCode.get(CommonSearchErrorCodes.INVALID_INDEX_DATA, "i18n.invalid.index.data");
            }
        } else {
            final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;
            final ErrorCode errorCode = ErrorCode.get(errorCodeProvider, "i18n.search.viewed.data.save.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, errorCodeProvider.name()), errors));
        }
    }

    /**
     * <p>
     * Builds a sub entity such as emails, phones, notes etc., containing search text.
     * </p>
     *
     * @param searchText Represents the search term.
     * @param fieldArray Holds a contact related field details.
     * @return {@link JsonArray}
     */
    protected JsonObject buildMatchingSubEntityJson(final String searchText, final JsonArray fieldArray,
                                                    final String fieldValueKey) {
        final JsonObject matchedFieldJson = Json.object();

        for (int count = NumericalConstant.ZERO; count < fieldArray.size(); count++) {
            final JsonObject fieldJson = fieldArray.getJsonObject(count);
            final String fieldValue = fieldJson.containsKey(fieldValueKey) ? fieldJson.getString(fieldValueKey) : null;

            if (Objects.nonNull(fieldValue) && fieldValue.toLowerCase().contains(searchText.toLowerCase())) {
                final int size = fieldArray.size();

                matchedFieldJson.put(fieldValueKey, fieldValue);

                if (size > NumericalConstant.ONE) {
                    final int balanceCount = size - NumericalConstant.ONE;

                    matchedFieldJson.put(CommonSearchFields.COUNT, balanceCount);
                }
                break;
            }
        }
        return NumericalConstant.ZERO < matchedFieldJson.size() ? matchedFieldJson : null;
    }

    /**
     * <p>
     * Loads sub entity fields.
     * </p>
     *
     * @param entityJson Holds entity related field details.
     */
    protected void loadSubEntityFields(final JsonObject entityJson, final Collection<String> fieldKeys) {

        if (Objects.nonNull(entityJson)) {
            final Consumer<String> fieldsBuilder = (fieldKey) -> {
                final JsonArray fieldArray = entityJson.optJsonArray(fieldKey);

                loadSubEntityFields(entityJson, fieldArray, fieldKey);
            };

            fieldKeys.forEach(fieldsBuilder);
        }
    }

    /**
     * <p>
     * Loads sub entity fields based on the inputs.
     * </p>
     *
     * @param entityJson Holds entity related field details.
     * @param fieldArray Holds a fields values.
     * @param fieldKey   Represents a field's key.
     */
    protected void loadSubEntityFields(final JsonObject entityJson, final JsonArray fieldArray,
                                       final String fieldKey) {

        if (!Json.isEmpty(fieldArray)) {
            final int size = fieldArray.size();
            final JsonObject fieldJson = fieldArray.getJsonObject(NumericalConstant.ZERO);

            if (NumericalConstant.ONE < size) {
                final int balanceCount = size - NumericalConstant.ONE;

                fieldJson.put(CommonSearchFields.COUNT, balanceCount);
            }
            entityJson.put(fieldKey, fieldJson);
        }
    }

    /**
     * <p>
     * Loads an entity's response based on matched results.
     * </p>
     *
     * @param entityJson     Holds contact related field details.
     * @param matchedResults Holds matched details.
     * @param fieldKeyMap    A map representing a field's name & value key.
     */
    protected void loadEntityDetailResponse(final JsonObject entityJson, final JsonObject matchedResults,
                                            final Map<String, String> fieldKeyMap) {

        for (final Map.Entry<String, String> fieldKeyMapEntry : fieldKeyMap.entrySet()) {
            final String fieldKey = fieldKeyMapEntry.getKey();
            final String fieldValueKey = fieldKeyMapEntry.getValue();
            final JsonObject matchedFieldJson = matchedResults.optJsonObject(fieldKey);
            final JsonArray fieldArray = entityJson.optJsonArray(fieldKey);

            if (Objects.isNull(matchedFieldJson)) {
                loadSubEntityFields(entityJson, fieldArray, fieldKey);
            } else {
                final int count = matchedFieldJson.optInt(CommonSearchFields.COUNT, NumericalConstant.ZERO);
                final String matchedFieldValue = matchedFieldJson.getString(fieldValueKey);

                for (int index = NumericalConstant.ZERO; index < fieldArray.size(); index++) {
                    final JsonObject fieldJson = fieldArray.getJsonObject(index);

                    if (Objects.equals(matchedFieldValue, fieldJson.getString(fieldValueKey))) {
                        fieldJson.put(CommonSearchFields.COUNT, NumericalConstant.ZERO == count ? null : count);
                        entityJson.put(fieldKey, fieldJson);
                        break;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Gets the trimmed search text.
     * </p>
     *
     * @param data A json containing request details.
     * @return trimmed search text.
     */
    protected String getTrimmedSearchText(final JsonObject data) {
        final String searchTextKey = CommonSearchFields.SEARCH_TEXT;

        if (!data.containsKey(searchTextKey)) {
            throw ErrorCode.get(CommonSearchErrorCodes.SEARCH_TEXT_NOT_FOUND, "i18n.search.text.not.found");
        }
        final String searchText = data.getString(searchTextKey);

        if (Objects.isNull(searchText)) {
            throw ErrorCode.get(CommonSearchErrorCodes.SEARCH_TEXT_IS_EMPTY, "i18n.search.text.should.not.be.empty");
        }
        final String trimmedSearchText = searchText.trim();

        if (NumericalConstant.ONE >= trimmedSearchText.length()) {
            throw ErrorCode.get(CommonSearchErrorCodes.TOO_SHORT_SEARCH_TEXT, "i18n.search.text.minimum.size.should.be.two");
        }

        return trimmedSearchText;
    }

    /**
     * <p>
     * Tailors response to fit the specifications.
     * </p>
     *
     * @return a json object.
     */
    protected JsonObject buildResponseForSearchRequest() {
        final JsonObject table = Json.object();

        table.put(CommonSearchFields.HAS_MORE, false);
        table.put(CommonSearchFields.TOTAL_RECORDS, NumericalConstant.ZERO);
        table.put(CommonSearchFields.SIZE, NumericalConstant.ZERO);
        table.put(CommonSearchFields.RESULTS, Json.array());

        return table;
    }

    /**
     * <p>
     * Loads matched results for non accessible docs.
     * </p>
     *
     * @param index          Represents index's name.
     * @param matchedResults Matched result's json.
     * @param indexJson      Json holding entity.
     */
    protected void loadMatchedResultsForNonAccessibleDoc(final String index, final JsonObject indexJson,
                                                         final JsonObject matchedResults) {
        final String name = indexJson.optString(CommonSearchFields.NAME, null);

        if (!StringUtil.isValidString(name)) {
            return;
        }

        matchedResults.put(CommonSearchFields.NAME, name);

        switch (index) {
            case CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.DEAL ->
                    loadOwnerName(indexJson, matchedResults);
            case CommonSearchFields.ACTIVITY -> {
                loadOwnerName(indexJson, matchedResults);
                matchedResults.put(CommonSearchFields.ACTIVITY_TYPE, indexJson.getJsonObject(CommonSearchFields.ACTIVITY_TYPE));
            }
        }
    }

    /**
     * <p>
     * Checks whether the data is present or not for given index
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the entry id
     * @param index Represents the index
     * @return count for data related to given index
     */
    private boolean isValidData(final long orgId, final long id, final String index) {

        return service.getReadableService(orgId).exists(String.valueOf(id), index);
    }

    /**
     * <p>
     * Builds the recently viewed API response in viewed-entry list order.
     * </p>
     *
     * @param viewedEntries            Entries in viewed-time order.
     * @param multiSearchRequest       Multi-index search request.
     * @param multiSearchDocumentSlots Document slots from {@link ReadableDocumentService#multiSearchGroupedByOrder}.
     * @param permissibleByIndex         Index name to whether a full permissible fetch was requested in CRM.
     * @return response in {@link JsonArray}.
     */
    private JsonArray buildResponseForRecentlyViewedDocsInOrder(final Collection<RecentlyViewedEntry> viewedEntries,
                                                                final MultiSingleIndexRequest multiSearchRequest,
                                                                final JsonArray multiSearchDocumentSlots,
                                                                final Map<String, Boolean> permissibleByIndex) {
        final Map<String, Map<Long, JsonObject>> documentsByIndexAndId = buildDocumentsByIndexAndId(
                multiSearchRequest, multiSearchDocumentSlots);
        final JsonArray recentlyViewedEntry = Json.array();

        for (final RecentlyViewedEntry viewedEntry : viewedEntries) {
            final String indexName = viewedEntry.index().name();
            final Long entryId = viewedEntry.entryId();
            final Map<Long, JsonObject> indexDocuments = documentsByIndexAndId.get(indexName);
            final JsonObject indexJson = Objects.isNull(indexDocuments) ? null : indexDocuments.get(entryId);
            final boolean isPermissible = permissibleByIndex.getOrDefault(indexName, false);
            final JsonObject recentlyViewedJson = Json.object();

            recentlyViewedJson.put(CommonSearchFields.INDEX, indexName);
            populateRecentlyViewedEntryResponse(indexName, isPermissible, indexJson, recentlyViewedJson);
            recentlyViewedEntry.put(recentlyViewedJson);
        }

        return recentlyViewedEntry;
    }

    /**
     * <p>
     * Indexes fetched documents by index name and entry id for ordered response assembly.
     * </p>
     *
     * @param multiSearchRequest       Multi-index search request.
     * @param multiSearchDocumentSlots Document slots aligned with executed sub-requests.
     * @return map of index name to entry id to document.
     */
    private Map<String, Map<Long, JsonObject>> buildDocumentsByIndexAndId(final MultiSingleIndexRequest multiSearchRequest,
                                                                          final JsonArray multiSearchDocumentSlots) {
        final Map<String, Map<Long, JsonObject>> documentsByIndexAndId = new HashMap<>();
        int slotIndex = NumericalConstant.ZERO;

        for (final SingleIndexSearchRequest singleIndexSearchRequest : multiSearchRequest.get()) {
            final SearchRequest searchRequest = singleIndexSearchRequest.getSearchRequest();

            if (Objects.isNull(searchRequest.getFilter())) {
                continue;
            }
            final String indexName = singleIndexSearchRequest.getIndexName();
            final JsonArray slotDocuments = multiSearchDocumentSlots.getJsonArray(slotIndex++);

            for (int count = NumericalConstant.ZERO; count < slotDocuments.size(); count++) {
                final JsonObject indexJson = slotDocuments.getJsonObject(count);
                final Long entryId = indexJson.getLong(CommonSearchFields.ID);

                documentsByIndexAndId.computeIfAbsent(indexName, key -> new HashMap<>()).put(entryId, indexJson);
            }
        }

        return documentsByIndexAndId;
    }
}