package com.tlc.crm.entity.list;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.list.SecureListAction;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.ListStateProcessor;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListDataContainer;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.list.transformer.ModelTransformer;

import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.filter.internal.DefaultFilter;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.list.filter.resolver.FilterValueResolver;
import com.tlc.crm.entity.list.mobile.MobileSqlListInterpreter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.common.list.sort.api.manager.SortManager;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.entity.list.summary.model.ListSummary;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.cache.FieldSourceCache;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.web.Action;
import com.tlc.web.HttpUtil;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * That involve listing or retrieving data
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public abstract class EntityListAction extends SecureListAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityListAction.class);

    private final FilterManager filterManager;
    private final FilterCriteriaBuilder filterCriteriaBuilder;
    private final UserService userService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final FieldService fieldService;
    private final FieldSourceCache fieldSourceCache;
    private final SortManager sortManager;
    private final ListStateProcessor listStateProcessor;
    private final RolePermissionService rolePermissionService;

    protected EntityListAction() {
        filterManager = FilterManager.getInstance();
        filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        userService = UserService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        fieldService = FieldService.getSecureInstance();
        fieldSourceCache = FieldSourceCache.getInstance();
        sortManager = SortManager.getInstance();
        this.listStateProcessor = ListStateProcessor.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
    }

    /**
     * Gets the name of the provider for the list action.
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @return The {@link Provider}.
     */
    protected Provider getProvider(final ListRequestData listRequestData) {
        return getProvider();
    }

    /**
     * Gets the name of the provider for the list action.
     *
     * @return The {@link Provider}.
     */
    protected Provider getProvider() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return API type of the current {@link Action}.
     */
    @Override
    protected final Optional<String> fetchApiType() {
        return Optional.of(ListActionFields.LIST);
    }

    /**
     * Performs the list action process.
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Refers the {@link CrmResponse} object.
     */
    @Override
    protected void processListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
        final Provider provider = getProvider(listRequestData);
        final JoinLoader joinLoader = getJoinLoader();
        final long orgId = listRequestData.getOrgId();
        final Long userId = listRequestData.getUserId();
        final I18nResolutionAdapter i18NResolutionAdapter = listRequestData.getI18nPlaceholderResolver();
        final JsonObject requestJson = listRequestData.getRequestJson();
        final JsonObject response = Json.object();
        final Source source = provider.getSource();

        loadJoins(joinLoader);

        final EntityListInterpreter listInterpreter = getEntityListInterpreter(orgId, userId, requestJson, provider, joinLoader, i18NResolutionAdapter);

        preProcess(listInterpreter);

        if (requestJson.optBoolean(ListActionFields.FETCH_STRUCTURE)) {
            final long fetchStructureTime = System.currentTimeMillis();

            final JsonObject structureJson = fetchStructure(orgId, userId, source, listInterpreter, i18NResolutionAdapter);

            LOGGER.debug("List Action - [{}]: List fetch structure JSON is fetched in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - fetchStructureTime, orgId, userId);
            response.put(ListActionFields.STRUCTURE, structureJson);
        }
        final JsonObject limitJson = resolveLimitJson(orgId, userId, requestJson);

        LOGGER.debug("List Action - [{}]: Resolved limit: {}, ListType: {}, Request has limit: {}, Org ID: {}, User ID: {}", provider, limitJson, getListType(), requestJson.containsKey(ListActionFields.LIMIT), orgId, userId);

        if (requestJson.optBoolean(ListActionFields.FETCH_DATA, true)) {
            final Collection<ConfiguredField> defaultConfiguredFields = listInterpreter.getDefaultConfiguredFields();
            final Collection<String> defaultFieldNames = new ArrayList<>(defaultConfiguredFields.stream().map(ConfiguredField::fieldName).collect(Collectors.toUnmodifiableSet()));
            final JsonArray fields = requestJson.optJsonArray(ListActionFields.FIELDS);
            final Collection<ConfiguredField> configuredFields = new LinkedHashSet<>();
            final Collection<String> fieldNames = new LinkedHashSet<>();

            if (Objects.isNull(fields)) {
                listInterpreter.applyDefaultSelect();
            } else {
                if (0 == fields.size()) {
                    listInterpreter.applyMandatorySelect();
                } else {
                    final AtomicInteger order = new AtomicInteger(1);

                    for (int index = 0; index < fields.size(); index++) {
                        final JsonObject jsonObject = fields.getJsonObject(index);
                        final String fieldName = jsonObject.getString(ListActionFields.FIELD);
                        final ConfiguredField configuredField = new ConfiguredField(fieldName);
                        final boolean isEnabled = jsonObject.optBoolean(ListActionFields.IS_ENABLED, true);

                        configuredField.setOrder(order.getAndIncrement());
                        configuredFields.add(configuredField);

                        if (jsonObject.containsKey(ListActionFields.SUB_FIELDS)) {
                            final JsonArray subFieldsArray = jsonObject.optJsonArray(ListActionFields.SUB_FIELDS);
                            final List<String> subFieldsToBeSelected = new ArrayList<>();

                            for (int subFieldIndex = 0; subFieldIndex < subFieldsArray.size(); subFieldIndex++) {
                                final String subFieldName = subFieldsArray.getJsonObject(subFieldIndex).getString(ListActionFields.FIELD);

                                subFieldsToBeSelected.add(subFieldName);
                                listInterpreter.updateConfiguredSubField(subFieldName, fieldName, true);
                            }
                            final List<String> subFields = configuredField.subFields();

                            if (!subFields.isEmpty()) {
                                subFields.stream().filter(subField -> !subFieldsToBeSelected.contains(subField))
                                        .forEach(subField -> listInterpreter.updateConfiguredSubField(subField, fieldName, false));
                            }

                            subFieldsToBeSelected.stream().filter(subFieldToBeSelect -> !subFields.contains(subFieldToBeSelect))
                                    .forEach(configuredField::addSubField);
                        } else {
                            final List<String> subFields = configuredField.subFields();

                            if (!subFields.isEmpty()) {

                                for (final String subField : subFields) {
                                    listInterpreter.updateConfiguredSubField(subField, fieldName, false);
                                }
                            }
                            configuredField.addSubFields(Collections.emptyList());
                        }
                        fieldNames.add(fieldName);

                        //Enable the 'configuredField' if 'fieldName' exists in 'defaultFieldNames' or if the 'isEnabled' flag is true.
                        configuredField.setEnabled(defaultFieldNames.remove(fieldName) || isEnabled);
                        listInterpreter.selectClause(configuredField);

                        //To configured based on the 'isEnabled' flag from the request
                        configuredField.setEnabled(isEnabled);
                    }

                    listInterpreter.setFieldNames(fieldNames);
                    listInterpreter.updateConfiguredFields(configuredFields);

                    for (final String fieldName : defaultFieldNames) {
                        final ConfiguredField configuredField = new ConfiguredField(fieldName);

                        configuredField.setEnabled(true);
                        listInterpreter.selectClause(configuredField);
                    }
                }
            }
            applyOrderByClause(orgId, userId, source, requestJson.optJsonArray(ListActionFields.SORT), listInterpreter);
            final long filterCriteriaTime = System.currentTimeMillis();

            applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);

            LOGGER.debug("List Action - [{}]: Filter criteria applied in the query in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - filterCriteriaTime, orgId, userId);
            applyLimitsAndLoadPageSummary(orgId, userId, requestJson, listInterpreter, limitJson, response);
            loadResponse(listRequestData, listInterpreter, response, crmResponse, provider);
        }

        if (requestJson.optBoolean(ListActionFields.FETCH_PAGE_DATA) && Objects.nonNull(limitJson)) {
            response.put(ListActionFields.LIMIT, limitJson);
        }
        postProcess(provider, listRequestData, response);

        if (Objects.nonNull(crmResponse)) {
            crmResponse.put(ListActionFields.TABLE, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Refers the organisation ID.
     * @param userId          Refers the user ID.
     * @param requestJson     Represents the request json
     * @param listInterpreter Represents the {@link ListInterpreter}
     * @param limitInfo       Holds the information about the limit for pagination
     * @param response        Represents the response json
     */
    @Override
    protected void applyLimitsAndLoadPageSummary(final long orgId, final long userId, final JsonObject requestJson,
                                                 final ListInterpreter listInterpreter, final JsonObject limitInfo, final JsonObject response) {
        final int start = limitInfo.getInt(ListActionFields.START);
        final int count = limitInfo.getInt(ListActionFields.COUNT);
        final long endPosition = start + count;

        listInterpreter.setLimits(start, count);

        if (requestJson.optBoolean(ListActionFields.FETCH_HITS)) {
            final long totalHits = listInterpreter.fetchTotalHits();

            loadPageSummary(totalHits, endPosition <= totalHits, response);
        }
    }

    /**
     * <p>
     * Applies the order by clause
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param userId            Represents the user id
     * @param source            Represents the {@link Source}
     * @param sortFields        Represents the sorting fields and their sort types
     * @param listInterpreter   Represents the {@link EntityListInterpreter} to which the order by clause needs to be applied
     */
    private void applyOrderByClause(final long orgId, final Long userId, final Source source, final JsonArray sortFields,
                                    final EntityListInterpreter listInterpreter) {
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, fieldSourceCache.getSourceId(source.getName()));
        final ListType listType = getListType();

        listSortConfig.setListType(listType);
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);

        if (Objects.isNull(sortFields) || 0 == sortFields.size()) {
            listInterpreter.applyDefaultOrderBy();

            if (Objects.nonNull(listType)) {
                sortManager.remove(listSortConfig);
            }
        } else {
            for (int index = 0; index < sortFields.size(); index++) {
                final JsonObject fieldInfo = sortFields.getJsonObject(index);
                final String field = fieldInfo.getString(ListActionFields.FIELD);
                final Object sortType = fieldInfo.get(ListActionFields.TYPE);

                listInterpreter.orderByClause(field, sortType == null || !sortType.toString().equalsIgnoreCase(ListActionFields.SORT_DESC));
            }
            listSortConfig.setConfig(sortFields.toString());
            sortManager.update(listSortConfig);
        }
    }

    /**
     * <p>
     * Resolves the limit JSON for pagination based on the request and stored state.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param requestJson Represents the request JSON containing pagination data
     * @return The resolved limit JSON for pagination
     */
    protected JsonObject resolveLimitJson(final long orgId, final long userId,
                                          final JsonObject requestJson) {
        return listStateProcessor.processAndGetLimitJson(
                requestJson, new ListPaginationState(orgId, userId, getListType())
        );
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return null;
    }

    /**
     * <p>
     * Loads the response as asynchronously to the client.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     * @param crmResponse     Holds the {@link CrmResponse}.
     * @param provider        Refers the {@code Provider}.
     */
    protected void loadResponse(final ListRequestData listRequestData, final EntityListInterpreter listInterpreter,
                                final JsonObject response, final CrmResponse crmResponse, final Provider provider) {
        final ListDataContainer listDataContainer = fetchCrmListResponse(listRequestData, listInterpreter);
        final Collection<Long> userIds = listDataContainer.getUserIds();
        final Map<String, Map<String, Object>> rows = listDataContainer.groupRowsByPk(listInterpreter, getRowListener());
        final Collection<Map<String, Object>> entityRows = rows.values();

        loadSummaryIntoResponse(listInterpreter, response);
        loadDynamicFields(listInterpreter, rows);
        loadPreviewData(listInterpreter, rows);

        postProcess(provider, listRequestData, response);

        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            response.put(ListActionFields.ROWS, entityRows);
            crmResponse.put(ListActionFields.TABLE, response);
        } else {
            dispatchEntities(listRequestData, userIds, entityRows, response, crmResponse, provider);
        }
    }

    /**
     * <p>
     * Loads the required preview details into the provided entity rows.
     * </p>
     *
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param entityRows      The {@link Map} containing entity IDs to their associated data rows
     */
    protected void loadPreviewData(final EntityListInterpreter listInterpreter, final Map<String, Map<String, Object>> entityRows) {
        final Map<String, Map<String, Object>> previewDataMap = listInterpreter.fetchPreviewData();

        entityRows.forEach((id, entityRow) -> {
            final Map<String, Object> previewFieldData = previewDataMap.get(id);

            if (Objects.nonNull(previewFieldData) && !previewFieldData.isEmpty()) {
                entityRow.put(ListActionFields.PARTIAL_DATA, previewFieldData);
            }
        });
    }

    /**
     * <p>
     * Loads the summary details into the response JSON.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     */
    protected void loadSummaryIntoResponse(final EntityListInterpreter listInterpreter, final JsonObject response) {
        for (final ListSummary listSummary : listInterpreter.fetchSummaryDetails()) {
            response.put(listSummary.getType().name(), (Object) listSummary.getDetails());
        }
    }

    /**
     * <p>
     * Loads dynamic fields into the provided entity rows.
     * </p>
     *
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param entityRows      A {@link Map} of entity IDs to their associated data rows
     */
    protected void loadDynamicFields(final EntityListInterpreter listInterpreter, final Map<String, Map<String, Object>> entityRows) {
        final Map<String, Map<String, Object>> dynamicFieldsByEntityId = listInterpreter.fetchDynamicFields();

        entityRows.forEach((id, entityRow) -> {
            final Map<String, Object> dynamicFieldsRow = dynamicFieldsByEntityId.get(id);

            if (Objects.nonNull(dynamicFieldsRow) && !dynamicFieldsRow.isEmpty()) {
                entityRow.putAll(dynamicFieldsRow);
            }
        });
    }

    private void preProcess(final ListInterpreter listInterpreter) {

    }

    /**
     * <p>
     * Handles post-processing after executing the entity list action.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param responseJson    Represents the list response json
     */
    protected void postProcess(final Provider provider, final ListRequestData listRequestData, final JsonObject responseJson) {

    }

    /**
     * <p>
     * Fetches the user details by using {@link EventCallback} and then dispatch the response.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param userIds         Represents the user ids.
     * @param rows            Represents the details of the entities.
     * @param response        Holds the details of the response.
     * @param crmResponse     Holds the details of the {@link CrmResponse}.
     * @param provider        Refers the {@code Provider}.
     */
    protected void dispatchEntities(final ListRequestData listRequestData, final Collection<Long> userIds, final Collection<Map<String, Object>> rows,
                                    final JsonObject response, final CrmResponse crmResponse, final Provider provider) {
        final long orgId = listRequestData.getOrgId();
        final Collection<Long> iamUserIds = userService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());
        final OrganizationService organizationService = OrganizationService.getInstance();

        final long userFetchStartTime = System.currentTimeMillis();

        LOGGER.debug("List Action - [{}]: Going to fetch the users for Org ID: {}, User IDs: {}", provider, orgId, userIds);

        if (Objects.nonNull(crmResponse)) {
            crmResponse.convertIntoAsync();
        }
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {
            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    LOGGER.debug("List Action - [{}]: Users are fetched in {}ms, Org ID: {}", provider, System.currentTimeMillis() - userFetchStartTime, orgId);
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    processUserDetails(orgId, iamUserIds, userMap, rows);
                    response.put(ListActionFields.ROWS, rows.stream().filter(row -> !row.isEmpty()).toList());
                    postProcess(provider, listRequestData, response);
                    crmResponse.put(ListActionFields.TABLE, response);
                    LOGGER.debug("List Action - [{}]: Users are fetched and processed in {}ms, Org ID: {}", provider, System.currentTimeMillis() - userFetchStartTime, orgId);
                    crmResponse.send();
                } catch (Throwable e) {
                    LOGGER.error("List Action - [{}]: List response send action failed, Org ID: {}, Message: {}", provider, orgId, e.getMessage(), e);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(EntityErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }

    /**
     * Groups the array of fields and transforms the entities into the required forms.
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter {@link ListInterpreter}.
     * @return Transformed entities
     */
    @Override
    protected ListDataContainer fetchCrmListResponse(final ListRequestData listRequestData, final ListInterpreter listInterpreter) {
        final EntityListInterpreter entityListInterpreter = (EntityListInterpreter) listInterpreter;
        final Collection<Map<String, Object>> result = listInterpreter.getResult();
        final long orgId = listRequestData.getOrgId();
        final I18nResolutionAdapter i18NResolutionAdapter = listRequestData.getI18nPlaceholderResolver();
        final ListStructure listStructure = listInterpreter.structure();
        final ModelTransformer modelTransformer = listStructure.getProviderTransformer();
        final Map<String, String> multiSelectFields = entityListInterpreter.getMultiSelectFields();
        final Collection<Map<String, Object>> entities = multiSelectFields.isEmpty() ? result
                : groupEntitiesByMultiSelectFields(result, multiSelectFields);

        if (Objects.isNull(modelTransformer) || entities.isEmpty()) {
            return new ListDataContainer(orgId, i18NResolutionAdapter, result);
        } else {
            final long transformerTime = System.currentTimeMillis();
            final TransformerResponse transformerResponse = transform(listRequestData.getOrgId(), result, modelTransformer, listRequestData.getTimezoneInfo());

            LOGGER.debug("List Action - [{}]: Model transformer process completed in {}ms, Org ID: {}", getProvider(), System.currentTimeMillis() - transformerTime, orgId);
            return new ListDataContainer(orgId, i18NResolutionAdapter, transformerResponse.getResults(), transformerResponse.getUserIds());
        }
    }

    /**
     * <p>
     * Groups the multi select field data rows into the single entity.
     * </p>
     *
     * @param entities               Represents the total entities for grouping
     * @param multiSelectFieldGroups Represents the selected field groups
     * @return Grouped entities
     */
    private Collection<Map<String, Object>> groupEntitiesByMultiSelectFields(final Collection<Map<String, Object>> entities,
                                                                             final Map<String, String> multiSelectFieldGroups) {
        final Map<Long, Map<String, Collection<Object>>> multiSelectFieldEntries = new HashMap<>();

        for (final Map<String, Object> entity : entities) {

            for (final Map.Entry<String, String> multiSelectFieldGroup : multiSelectFieldGroups.entrySet()) {
                final String multiSelectFieldName = multiSelectFieldGroup.getKey();
                final String multiSelectField = multiSelectFieldGroup.getValue();
                final Object multiSelectFieldId = entity.remove(multiSelectFieldName);
                final Object multiSelectFieldValue = entity.remove(multiSelectField);
                final Long entityId = (Long) entity.get(ListActionFields.ID);

                if (Objects.nonNull(multiSelectFieldId) && Objects.nonNull(multiSelectFieldValue)) {
                    final Map<String, Object> multiSelectFieldEntry = Map.of(ListActionFields.ID, multiSelectFieldId,
                            multiSelectField.split("_")[1], multiSelectFieldValue);

                    entity.put(multiSelectFieldName, multiSelectFieldEntry);
                    multiSelectFieldEntries.computeIfAbsent(entityId, map -> new HashMap<>()).computeIfAbsent(multiSelectFieldName,
                            multipleSelectField -> new HashSet<>()).add(multiSelectFieldEntry);
                }

                if (Objects.nonNull(multiSelectFieldId) && !multiSelectFieldEntries.isEmpty()) {
                    entity.put(multiSelectFieldName, multiSelectFieldEntries.get(entityId).get(multiSelectFieldName));
                }
            }
        }

        return entities;
    }

    /**
     * <p>
     * Fetches the structure based on the configured fields in the {@link ListInterpreter}.
     * </p>
     *
     * @param orgId                   Represents an organization id
     * @param source                  Represents the {@link Source}
     * @param listInterpreter         Represents the {@link ListInterpreter}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @return {@link JsonObject} of structure
     */
    private JsonObject fetchStructure(final long orgId, final long userId, final Source source, final EntityListInterpreter listInterpreter, final I18nResolutionAdapter i18NResolutionAdapter) {
        final Collection<FieldConfig> configs = listInterpreter.getFieldConfigs();
        final JsonObject structureJson = Json.object();
        final JsonArray fieldsArray = Json.array();
        final String sourceName = source.getName();
        final Collection<String> configuredFieldNames = listInterpreter.getConfiguredFieldNames();
        final Collection<FieldConfig> fieldConfigs = Objects.isNull(configuredFieldNames) ? configs : configs.stream()
                .filter(fieldConfig -> configuredFieldNames.contains(fieldConfig.getName())).toList();
        final Map<String, Collection<FieldInfo>> sourceFields = Source.isPrimarySource(source) ? fieldService.groupAllFieldsBySource(orgId, List.of(sourceName)) : Collections.emptyMap();
        final Map<Long, FieldInfo> persistentFields = new HashMap<>();
        final Map<String, FieldInfo> transientFields = new HashMap<>();
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, fieldSourceCache.getSourceId(sourceName));

        listSortConfig.setListType(getListType());
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);
        final Collection<SortCriteria> sortCriteriaList = sortManager.fetchCriteriaList(listSortConfig);
        final boolean hasSortCriteria = !sortCriteriaList.isEmpty();
        final Optional<SortCriteria> optionalSortCriteria = hasSortCriteria ? sortCriteriaList.stream().findFirst() : Optional.empty();

        for (final FieldInfo fieldInfo : sourceFields.getOrDefault(sourceName, Collections.emptyList())) {
            if (fieldInfo.isPersistent()) {
                persistentFields.put(fieldInfo.id(), fieldInfo);
            } else {
                transientFields.put(fieldInfo.aliasName(), fieldInfo);
            }
        }

        for (final FieldConfig fieldConfig : fieldConfigs) {
            final JsonObject fieldInfoJson = Json.object();
            final FieldType fieldType = fieldConfig.getFieldType();
            final FieldConfig.Config config = fieldConfig.getConfig();
            final boolean configIsNotNull = Objects.nonNull(config);
            final String fieldName = fieldConfig.getName();
            final FieldInfo fieldInfo;
            final I18nKey displayName;

            if (fieldConfig.isPersistent()) {
                fieldInfo = persistentFields.get(fieldConfig.getFieldId());
                displayName = fieldInfo.i18nName();
            } else {
                fieldInfo = transientFields.get(fieldName);
                displayName = fieldConfig.getDisplayName();
            }

            if (configIsNotNull && config.isIgnoredField()) {
                continue;
            }

            if (!isFieldAccessible(orgId, fieldConfig)) {
                continue;
            }
            final String configuredFieldType = configIsNotNull ? config.getFieldType() : null;
            final String fieldTypeName;

            if (Objects.isNull(configuredFieldType)) {

                if (Objects.isNull(fieldType)) {
                    fieldTypeName = null;
                } else {
                    final String listFieldType = Objects.isNull(fieldInfo) ? null : fieldInfo.presetConfig().getListFieldType();

                    fieldTypeName = Objects.isNull(listFieldType) ? fieldType.name() : listFieldType;
                }
            } else {
                fieldTypeName = configuredFieldType;
            }
            final Collection<SubFieldConfig> subFieldConfigs = fieldConfig.getSubFieldsConfig();

            fieldInfoJson.put(ListActionFields.NAME, fieldName);
            fieldInfoJson.put(ListActionFields.DISPLAY_NAME, Objects.isNull(i18NResolutionAdapter) ? displayName.getKey() : i18NResolutionAdapter.get(displayName));
            fieldInfoJson.put(ListActionFields.SEARCH, fieldConfig.isSearch());
            fieldInfoJson.put(ListActionFields.HIDDEN, configIsNotNull && !isFieldEnabled(config, source));
            final boolean isSortable = fieldConfig.isSortable();

            fieldInfoJson.put(ListActionFields.IS_SORTABLE, isSortable);

            if (isSortable && hasSortCriteria) {

                optionalSortCriteria.ifPresent(sortCriteria -> {

                    if (fieldName.equals(sortCriteria.getFieldName())) {
                        fieldInfoJson.put(ListActionFields.SORT, sortCriteria.getSortType().getOrderTypeName());
                    }
                });
            }
            fieldInfoJson.put(ListActionFields.FIELD_TYPE, StoreType.exists(fieldTypeName)
                    ? StoreType.valueOf(fieldTypeName).getBaseType().getName() : fieldTypeName);
            fieldInfoJson.put(ListActionFields.IS_MANDATORY_FIELD, configIsNotNull && config.isMandatoryField());
            fieldInfoJson.put(ListActionFields.LINK_TYPE, configIsNotNull ? config.getLinkType() : null);

            if (configIsNotNull && config.isClickable()) {
                fieldInfoJson.put(ListActionFields.IS_CLICKABLE, true);
            }

            if (Objects.nonNull(fieldInfo)) {

                if(fieldConfig.isPersistent()) {
                    fieldInfoJson.put(ListActionFields.SOURCE_ID, fieldSourceCache.getSourceId(sourceName));
                    fieldInfoJson.put(ListActionFields.INTERNAL_NAME, fieldInfo.getInternalName());
                }
                fieldInfoJson.put(ListActionFields.CONFIG, fieldInfo.presetConfig().presetConfigAsJson());
                fieldInfoJson.put(ListActionFields.ID, fieldInfo.id());
            }

            if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {
                final JsonArray subTypesJsonArray = convertSubTypes(subFieldConfigs, i18NResolutionAdapter);

                if (subTypesJsonArray.size() > 0) {
                    fieldInfoJson.put(ListActionFields.SUB_TYPES, subTypesJsonArray);
                }
            }

            fieldsArray.put(fieldInfoJson);
        }
        structureJson.put(ListActionFields.COLUMNS, fieldsArray);
        structureJson.put(ListActionFields.PRIMARY, listInterpreter.primaryField());

        return structureJson;
    }

    /**
     * <p>
     * Checks whether the field is enabled or not
     * </p>
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isEnabledForWeb();
    }


    /**
     * <p>
     * Checks whether the given field is accessible, where a field referring to another module requires view permission for that module.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param fieldConfig Represents the {@link FieldConfig}.
     * @return True if the field does not refer to a module or the referred module is viewable, false otherwise.
     */
    private boolean isFieldAccessible(final long orgId, final FieldConfig fieldConfig) {
        final FieldType fieldType = fieldConfig.getFieldType();

        if (Objects.isNull(fieldType)) {
            return true;
        }
        final Long roleId = LoggedInUserInfo.getUser().getRoleId();

        return SubCategoryTypeUtils.findSubCategoryType(fieldType.storeType())
                .map(subCategoryType -> rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, subCategoryType, Type.VIEW))
                .map(RolePermission::isGranted)
                .orElse(true);
    }

    /**
     * Converts sub types details
     *
     * @param subFieldConfigs Refers the collection of {@link SubFieldConfig}
     * @param i18n            Refers the {@link I18nResolutionAdapter}
     * @return {@link JsonArray}
     */
    private JsonArray convertSubTypes(final Collection<SubFieldConfig> subFieldConfigs, final I18nResolutionAdapter i18n) {
        final JsonArray jsonArray = Json.array();

        subFieldConfigs.forEach(subFieldConfig -> {
            final JsonObject jsonObject = Json.object();
            final FieldSubType fieldSubType = subFieldConfig.getFieldSubType();
            final I18nKey displayName = fieldSubType.displayName();

            jsonObject.put(ListActionFields.DISPLAY_NAME, i18n == null ? displayName.getKey() : i18n.get(displayName));
            jsonObject.put(ListActionFields.NAME, fieldSubType.storeSubType().getAliasName());
            jsonObject.put(ListActionFields.FIELD_TYPE, fieldSubType.inputType());
            jsonObject.put(ListActionFields.HIDDEN, !subFieldConfig.isConfigured());
            jsonObject.put(ListActionFields.ID, subFieldConfig.getFieldSubType().id());

            jsonArray.put(jsonObject);
        });

        return jsonArray;
    }

    /**
     * <p>
     * Gets the {@link EntityListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link ListInterpreter}
     */
    protected EntityListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                             final Provider provider, final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new EntitySQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Processes the user details from the given iam user ids.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Represents the iam user ids
     * @param userMap    Represents the {@link User} along with its id
     * @param rows       Represents the details of the entities
     */
    protected final void processUserDetails(final long orgId, final Collection<Long> iamUserIds,
                                            final Map<Long, User> userMap, final Collection<Map<String, Object>> rows) {
        final long currentTime = System.currentTimeMillis();
        final Map<Long, Map<String, Object>> crmUsers = new HashMap<>();

        for (final Long iamUserId : iamUserIds) {
            final Map<String, Object> crmUser = new HashMap<>();
            final User user = userMap.get(iamUserId);
            final Long crmUserId = userService.getUserId(orgId, iamUserId);
            final String firstName = user.getFirstName();
            final String lastName = user.getLastName();
            final DisplayPicture thumbnail = user.getThumbnail();

            crmUser.put(ListActionFields.ID, crmUserId);
            crmUser.put(ListActionFields.NAME, Objects.isNull(lastName) ? firstName : String.format("%s %s", firstName, lastName));

            if (Objects.nonNull(thumbnail)) {
                final Map<String, Object> thumbnailValues = new HashMap<>();
                final JsonObject userJson = Json.object();
                final String extension = thumbnail.extension().getName();
                final String imageType = thumbnail.type().getName();

                userJson.put(ListActionFields.USER_ID, user.id());
                userJson.put(ListActionFields.ID, randomIdGenerator.generateUniqueId());
                thumbnailValues.put(ListActionFields.NAME, imageType);
                thumbnailValues.put(ListActionFields.SIZE, thumbnail.size());
                thumbnailValues.put(ListActionFields.EXTENSION, extension);
                thumbnailValues.put(ListActionFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                        Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension)));
                crmUser.put(ListActionFields.THUMBNAIL, thumbnailValues);
            }

            crmUsers.put(crmUserId, crmUser);
        }

        for (final Map<String, Object> row : rows) {

            if (row.containsKey(ListActionFields.USER_ID)) {
                final Object value = row.remove(ListActionFields.USER_ID);

                if (value instanceof Map) {

                    for (final Map.Entry<String, Object> valueEntry : ((Map<String, Object>) value).entrySet()) {
                        final Object user = valueEntry.getValue();

                        row.put(valueEntry.getKey(), user instanceof final Collection<?> userIds
                                ? userIds.stream().map(crmUsers::get).toList() : crmUsers.get(user));
                    }
                }
            }
        }
        LOGGER.debug("List Action - [{}]: User details processed in {}ms, Org ID: {}, IAM User IDs: {}", getProvider(), System.currentTimeMillis() - currentTime, orgId, iamUserIds);
    }

    /**
     * <p>
     * Performs the mobile list action process
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Refers the {@link CrmResponse} object.
     */
    @Override
    protected final void processMobileListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
        final Provider provider = getProvider(listRequestData);
        final JoinLoader joinLoader = getJoinLoader();
        final long orgId = listRequestData.getOrgId();
        final Long userId = listRequestData.getUserId();
        final I18nResolutionAdapter i18NResolutionAdapter = listRequestData.getI18nPlaceholderResolver();
        final JsonObject requestJson = listRequestData.getRequestJson();
        final JsonObject response = Json.object();

        loadJoins(joinLoader);
        final MobileSqlListInterpreter listInterpreter = getMobileSqlListInterpreter(orgId, userId, provider, joinLoader, i18NResolutionAdapter);

        preProcess(listInterpreter);
        listInterpreter.applyDefaultSelect();
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, fieldSourceCache.getSourceId(provider.getSource().getName()));

        listSortConfig.setListType(getListType());
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);

        if (requestJson.containsKey(ListActionFields.SORT)) {
            final JsonArray sortArray = requestJson.optJsonArray(ListActionFields.SORT);
            final int size = sortArray.size();

            if (Objects.equals(sortArray.size(), NumericalConstant.ZERO)) {
                listInterpreter.applyDefaultOrderBy();
            } else {
                for (int index = 0; index < size; index++) {
                    final JsonObject sortField = sortArray.getJsonObject(index);
                    final String fieldName = sortField.getString(ListActionFields.FIELD);
                    final ConfiguredField configuredField = new ConfiguredField(fieldName);

                    configuredField.setEnabled(true);

                    if (!listInterpreter.isSelectedBefore(configuredField)) {
                        listInterpreter.selectClause(configuredField);
                    }
                    listInterpreter.orderByClause(fieldName, sortField.getString(ListActionFields.TYPE).equalsIgnoreCase(ListActionFields.SORT_ASC));
                }
            }
            listSortConfig.setConfig(sortArray.toString());
            sortManager.update(listSortConfig);
        } else {

            if (requestJson.containsKey(ListActionFields.IS_SORT_ENABLED) && !requestJson.getBoolean(ListActionFields.IS_SORT_ENABLED)) {
                listInterpreter.applyDefaultOrderBy();
                sortManager.remove(listSortConfig);
            } else {
                final Collection<SortCriteria> sortCriteriaList = sortManager.fetchCriteriaList(listSortConfig);
                final Optional<SortCriteria> optionalSortCriteria = sortCriteriaList.isEmpty() ? Optional.empty() : sortCriteriaList.stream().findFirst();

                if (optionalSortCriteria.isPresent()) {
                    final SortCriteria sortCriteria = optionalSortCriteria.get();
                    final String fieldName = sortCriteria.getFieldName();
                    final SortType sortType = sortCriteria.getSortType();
                    final ConfiguredField configuredField = new ConfiguredField(fieldName);
                    final JsonObject sortJson = Json.object();

                    sortJson.put(ListActionFields.FIELD, fieldName);
                    sortJson.put(ListActionFields.TYPE, sortType.getOrderTypeName());

                    requestJson.put(ListActionFields.SORT, sortJson);

                    configuredField.setEnabled(true);

                    if (!listInterpreter.isSelectedBefore(configuredField)) {
                        listInterpreter.selectClause(configuredField);
                    }
                    listInterpreter.orderByClause(fieldName, sortType.equals(SortType.ASCENDING));
                } else {
                    listInterpreter.applyDefaultOrderBy();
                }
            }
        }
        applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
        super.applyLimitsAndLoadPageSummary(orgId, userId, requestJson, listInterpreter, requestJson.optJsonObject(ListActionFields.LIMIT), response);

        if (requestJson.optBoolean(ListActionFields.FETCH_SORTABLE_FIELDS)) {
            response.put(ListActionFields.SORTABLE_FIELDS, fetchSortableFields(listInterpreter, i18NResolutionAdapter));
        }

        loadMobileResponse(listRequestData, listInterpreter, response, requestJson.optJsonObject(ListActionFields.SORT), crmResponse);

        if (Objects.nonNull(crmResponse)) {
            crmResponse.put(ListActionFields.TABLE, response);
        }
    }

    /**
     * <p>
     * Get the {@link JsonArray} containing the fields that are sortable
     * </p>
     *
     * @param listInterpreter         Represents the {@link EntityListInterpreter}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @return {@link JsonArray} containing the sortable fields
     */
    private JsonArray fetchSortableFields(final MobileSqlListInterpreter listInterpreter, final I18nResolutionAdapter i18NResolutionAdapter) {
        final Collection<FieldConfig> fieldConfigs = listInterpreter.getFieldConfigs();
        final JsonArray fieldsArray = Json.array();
        final ListSortConfig listSortConfig = new ListSortConfig(listInterpreter.orgId, listInterpreter.userId, fieldSourceCache.getSourceId(listInterpreter.provider.getSource().getName()));

        listSortConfig.setListType(getListType());
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);
        final Collection<SortCriteria> sortCriteriaList = sortManager.fetchCriteriaList(listSortConfig);
        final Optional<SortCriteria> optionalSortCriteria = !sortCriteriaList.isEmpty() ? sortCriteriaList.stream().findFirst() : Optional.empty();

        for (final FieldConfig fieldConfig : fieldConfigs) {

            if (fieldConfig.isSortable() && isFieldAccessible(listInterpreter.orgId, fieldConfig)) {
                final JsonObject fieldInfoJson = Json.object();
                final I18nKey displayName = fieldConfig.getDisplayName();
                final String fieldName = fieldConfig.getName();

                fieldInfoJson.put(ListActionFields.NAME, fieldName);
                fieldInfoJson.put(ListActionFields.DISPLAY_NAME, Objects.isNull(i18NResolutionAdapter) ? displayName.getKey()
                        : i18NResolutionAdapter.get(displayName));

                optionalSortCriteria.ifPresent(sortCriteria -> {

                    if (fieldName.equals(sortCriteria.getFieldName())) {
                        fieldInfoJson.put(ListActionFields.SORT, sortCriteria.getSortType().getOrderTypeName());
                    }
                });

                fieldsArray.put(fieldInfoJson);
            }
        }

        return fieldsArray;
    }

    /**
     * <p>
     * Apply the filter criteria for the given request
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param requestJson     Represents the request json
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param provider        Represents the {@link Provider}
     */
    protected void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson, final EntityListInterpreter listInterpreter, final Provider provider) {
        final JsonObject filterCriteria = requestJson.optJsonObject(ListActionFields.FILTER_CRITERIA);
        final String filterIdKey = FilterFields.FILTER_ID.getName();

        if (Objects.nonNull(filterCriteria)) {
            applyCustomCriteria(listInterpreter, filterCriteria);
        }

        if (requestJson.containsKey(filterIdKey)) {
            final long filterId = requestJson.getLong(filterIdKey);
            final Filter filter = filterManager.get(orgId, filterId);
            final Filter.Category category = filter.getCategory();
            final Collection<FilterCriteria> criteriaList = filter.getCriteriaList();

            if (filter.isDefault() && category.needValueResolution()) {
                final FilterValueResolver valueResolver = new FilterValueResolver(orgId, userId, DefaultFilter.valueOf(filter.getAliasName()));

                criteriaList.forEach(valueResolver::resolve);
            }
            listInterpreter.applyCriteria(orgId, provider, criteriaList);

            if (category.isEligibleForRecentFilterUpdate()) {
                filterManager.updateRecentlyAppliedFilter(orgId, userId, filter.getFilterProvider().getId(), filterId);
            }
        } else if (requestJson.containsKey(ListActionFields.CRITERIA)) {
            applyCriteria(orgId, provider, listInterpreter, requestJson.getJsonArray(ListActionFields.CRITERIA));
        } else if (requestJson.containsKey(ListActionFields.OWNER_ID)) {
            final Collection<FilterCriteria> criteriaList = filterCriteriaBuilder.constructFilterCriteria(orgId, userId, provider, requestJson);

            listInterpreter.applyCriteria(orgId, provider, criteriaList);
        }

        if (requestJson.containsKey(ListActionFields.ADDITIONAL_CRITERIA)) {
            applyCriteria(orgId, provider, listInterpreter, requestJson.getJsonArray(ListActionFields.ADDITIONAL_CRITERIA));
        }

        applyDefaultCriteria(listInterpreter);
    }

    /**
     * <p>
     * Applies the default criteria for the entity list action
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     */
    protected void applyDefaultCriteria(final ListInterpreter listInterpreter) {
    }

    /**
     * <p>
     * Applies the criteria in the {@link EntityListInterpreter}
     * </p>
     *
     * @param orgId           Represents the org id
     * @param provider        Represents the {@link Provider}
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param criteria        Represents the filter criteria
     */
    protected void applyCriteria(final long orgId, final Provider provider,
                                 final EntityListInterpreter listInterpreter, final JsonArray criteria) {
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();
        final int criteriaSize = criteria.size();

        for (int index = 0; index < criteriaSize; index++) {
            final JsonObject criteriaJson = criteria.getJsonObject(index);

            criteriaJson.put(CriteriaFields.SOURCE.getName(), provider.getSource().getName());
            filterCriteriaList.add(filterCriteriaBuilder.build(orgId, criteriaJson));
        }
        listInterpreter.applyCriteria(orgId, provider, filterCriteriaList);
    }

    /**
     * <p>
     * Loads the response as asynchronously to the mobile client
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}
     * @param sortJson        Represents the sort {@link JsonObject}                      .
     * @param crmResponse     Holds the {@link CrmResponse}.
     */
    private void loadMobileResponse(final ListRequestData listRequestData, final MobileSqlListInterpreter listInterpreter,
                                    final JsonObject response, final JsonObject sortJson, final CrmResponse crmResponse) {
        final ListDataContainer listDataContainer = fetchMobileListResponse(listRequestData, listInterpreter);
        final Collection<Long> userIds = listDataContainer.getUserIds();
        final Collection<Map<String, Object>> entityRows = listDataContainer.getData(listInterpreter, getRowListener());

        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            loadListResponse(entityRows, listRequestData.getTimezoneInfo(), sortJson, response);
            crmResponse.put(ListActionFields.TABLE, response);
        } else {
            dispatchEntities(userIds, listRequestData, sortJson, response, entityRows, crmResponse);
        }
    }

    /**
     * <p>
     * Loads the list response into json
     * </p>
     *
     * @param entityRows   Represents the entity rows
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @param sortJson     Represents the sort {@link JsonObject}
     * @param responseJson Represents the response {@link JsonObject}
     */
    protected void loadListResponse(final Collection<Map<String, Object>> entityRows, final TimezoneInfo timezoneInfo,
                                    final JsonObject sortJson, final JsonObject responseJson) {
        responseJson.put(ListActionFields.ROWS, entityRows);
    }

    /**
     * <p>
     * Fetches the {@link ListDataContainer} for the given list request data
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter Represents the {@link MobileSqlListInterpreter} details
     * @return A map containing converted content data
     */
    private ListDataContainer fetchMobileListResponse(final ListRequestData listRequestData, final MobileSqlListInterpreter listInterpreter) {
        final ModelTransformer modelTransformer = listInterpreter.structure().getProviderTransformer();
        final Collection<Map<String, Object>> result = listInterpreter.getResult();
        final long orgId = listRequestData.getOrgId();
        final I18nResolutionAdapter i18NResolutionAdapter = listRequestData.getI18nPlaceholderResolver();

        if (Objects.isNull(modelTransformer) || result.isEmpty()) {
            return new ListDataContainer(orgId, i18NResolutionAdapter, result);
        } else {
            final TransformerResponse transformerResponse = modelTransformer.transformDataForMobile(new TransformerRequest(orgId, result, listRequestData.getTimezoneInfo(), CrmRequest.ClientType.MOBILE));

            return new ListDataContainer(orgId, i18NResolutionAdapter, transformerResponse.getResults(), transformerResponse.getUserIds());
        }
    }

    /**
     * <p>
     * Gets the {@link MobileSqlListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents an organization id
     * @param userId                  Represents the user id
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link ListInterpreter}
     */
    protected MobileSqlListInterpreter getMobileSqlListInterpreter(final long orgId, final Long userId, final Provider provider,
                                                                   final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new MobileSqlListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Fetches the user details by using {@link EventCallback} and then dispatch the response.
     * </p>
     *
     * @param userIds         Represents the user ids.
     * @param listRequestData Represents the {@link ListRequestData}.
     * @param sortJson        Represents the sort json.
     * @param rows            Represents the details of the entities.
     * @param response        Holds the details of the response..
     * @param crmResponse     Holds the details of the {@link CrmResponse}.
     */
    private void dispatchEntities(final Collection<Long> userIds, final ListRequestData listRequestData, final JsonObject sortJson,
                                  final JsonObject response, final Collection<Map<String, Object>> rows, final CrmResponse crmResponse) {
        final long orgId = listRequestData.getOrgId();
        final Collection<Long> iamUserIds = userService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());
        final OrganizationService organizationService = OrganizationService.getInstance();

        if (Objects.nonNull(crmResponse)) {
            crmResponse.convertIntoAsync();
        }
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param users Response from the consumer
             */
            @Override
            public void onSuccess(final Collection<User> users) {

                try {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    processUserDetails(orgId, iamUserIds, userMap, rows);
                    loadListResponse(rows.stream().filter(row -> !row.isEmpty()).toList(), listRequestData.getTimezoneInfo(), sortJson, response);
                    crmResponse.put(ListActionFields.TABLE, response);
                } catch (final Exception exception) {
                    sendError(crmResponse, null, exception);
                } finally {
                    crmResponse.send();
                }
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, throwable);
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }
}
