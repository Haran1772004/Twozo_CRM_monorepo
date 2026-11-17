package com.tlc.crm.entity.action.rest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.TypeChecker;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.secure.AbstractSecureRestAction;
import com.tlc.crm.common.fields.ApiKeys;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.filter.internal.DefaultFilter;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.list.filter.resolver.FilterValueResolver;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.common.list.sort.api.manager.SortManager;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.ComparatorService;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.sql.common.api.dml.JoinClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages the list related REST web actions.
 * </p>
 *
 * @author ThalaimalaiPanidyanT
 * @version 1.0
 */
public abstract class SecureRestListAction extends AbstractSecureRestAction {

    protected final FieldService fieldService;
    protected final com.tlc.iam.client.UserService userService;
    protected final UserService crmUserService;
    protected final OrganizationService organizationService;
    protected final ConverterConfigBuilder converterConfigBuilder;
    protected final FieldSourceService fieldSourceService;
    protected final ComparatorService comparatorService;
    protected final int maxFieldSelectionLimit;
    protected final int maxFilterCriteriaLimit;

    private final SortManager sortManager;
    private final FilterManager filterManager;
    private final FilterCriteriaBuilder filterCriteriaBuilder;

    protected SecureRestListAction() {
        this.fieldService = FieldService.getInstance();
        this.userService = com.tlc.iam.client.UserService.getInstance();
        this.crmUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.comparatorService = ComparatorService.getInstance();
        this.sortManager = SortManager.getInstance();
        this.filterManager = FilterManager.getInstance();
        this.filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();

        final Configurations listActionConfigurations = ConfHolder.getInstance().getProperties("list-action");
        this.maxFieldSelectionLimit = listActionConfigurations.getInt("field.selection.limit");
        this.maxFilterCriteriaLimit = listActionConfigurations.getInt("filter.limit");
    }

    /**
     * Processes the list action for the source.
     *
     * @param request     The request.
     * @param crmResponse The response to be sent.
     */
    protected void processList(final CrmRequest request, final CrmResponse crmResponse) {
        final String pageParam = request.getParameter("page");
        final String sizeParam = request.getParameter("size");
        final int page = Objects.isNull(pageParam) ? NumericalConstant.ONE : Integer.parseInt(pageParam);
        final int limit = Objects.isNull(sizeParam) ? NumericalConstant.ONE_HUNDRED : Integer.parseInt(sizeParam);
        final int start = ((page - 1) * limit) + 1;
        final Optional<String> sortParam = request.getParam("sort").findFirst();
        final JsonObject requestJson = Json.object();
        final JsonObject limitJson = Json.object();

        limitJson.put("start", start);
        limitJson.put("count", limit);
        requestJson.put("limit", limitJson);

        sortParam.ifPresent(sort -> applySort(request.orgId(), sort, requestJson));

        final Deque<String> filterParams = request.exchange().getQueryParams().get("filter");

        if (Objects.nonNull(filterParams) && !filterParams.isEmpty()) {
            formatToFilterJson(request.orgId(), requestJson, filterParams);
        }
        final Provider provider = getProvider();
        final JoinLoader joinLoader = new JoinLoader() {
            @Override
            public boolean isJoinExist() {
                return false;
            }

            @Override
            public List<JoinClause> getJoins() {
                return List.of();
            }

            @Override
            public void addJoinClause(final List<JoinClause> joinClauses) {
            }
        };
        final long orgId = request.orgId();
        final long userId = request.userId();
        final I18nResolutionAdapter i18NResolutionAdapter = request.getI18NCRMResolutionAdapter();
        final JsonObject response = Json.object();
        final Source source = provider.getSource();
        final EntityListInterpreter listInterpreter = getEntityListInterpreter(orgId, userId, requestJson, provider, joinLoader, i18NResolutionAdapter);

        if (Boolean.parseBoolean(request.getParam("fetchColumns").findFirst().orElseGet(Boolean.FALSE::toString))) {
            final JsonObject structureJson = fetchStructure(orgId, userId, source, listInterpreter, i18NResolutionAdapter);

            crmResponse.put(ListActionFields.STRUCTURE, structureJson);
        }

        if (requestJson.optBoolean(ListActionFields.FETCH_DATA, true)) {
            final JsonArray sortFields = requestJson.optJsonArray(ListActionFields.SORT);
            final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, fieldSourceService.getSource(source.getName()).id());
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
            applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
            applyLimitsAndLoadPageSummary(listInterpreter, requestJson.optJsonObject(ListActionFields.LIMIT), response);
            loadResponse(orgId, request, listInterpreter, response, crmResponse, provider);
        }

        if (Objects.nonNull(crmResponse)) {
            response.forEach(crmResponse::put);
        }
    }

    /**
     * Applies the sort details to the request json.
     *
     * @param orgId       Refers the organisation ID.
     * @param sort        Represents the sort details in the format "field:sortType,field:sortType".
     * @param requestJson Represents the request json to which the sort details need to be applied.
     */
    protected void applySort(final long orgId, final String sort, final JsonObject requestJson) {
        final JsonArray sortArray = Json.array();
        final String[] sortList = sort.split(",");

        for (final String sortData : sortList) {
            final String[] sortField = sortData.trim().split(":");
            final JsonObject jsonObject = Json.object();

            jsonObject.put(ApiKeys.FIELD, sortField[0].trim());
            jsonObject.put(ApiKeys.TYPE, sortField[1].trim());

            sortArray.put(jsonObject);
        }
        requestJson.put(ApiKeys.SORT, sortArray);
    }

    /**
     * <p>
     * Loads the response as asynchronously to the client.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     * @param crmResponse     Holds the {@link CrmResponse}.
     * @param provider        Refers the {@code Provider}.
     */
    protected void loadResponse(final Long orgId, final CrmRequest request, final EntityListInterpreter listInterpreter, final JsonObject response, final CrmResponse crmResponse, final Provider provider) {
        final EntitySQLListInterpreter entityListInterpreter = (EntitySQLListInterpreter) listInterpreter;
        final Collection<Long> entityIds = new LinkedHashSet<>(entityListInterpreter.getPkValues());

        if (!entityIds.isEmpty()) {
            fetchAndDispatchEntities(orgId, request, entityIds, crmResponse);
        } else {
            crmResponse.put("rows", Json.array());
            crmResponse.send();
        }
    }

    /**
     * Fetches the entities for the given organization and dispatches
     * them to the appropriate processors based on the request context.
     *
     * @param orgId       The ID of the organization for which entities are being fetched; must not be null.
     * @param request     The CRM request containing context and parameters; must not be null.
     * @param entityIds   The collection of entity IDs to retrieve and process; must not be null or empty.
     * @param crmResponse The response object to which the processed entity data should be written; must not be null.
     */
    protected abstract void fetchAndDispatchEntities(final Long orgId, final CrmRequest request, final Collection<Long> entityIds, final CrmResponse crmResponse);

    /**
     * <p>
     * Applies limits and loads the pagination summary details into response
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     * @param limitInfo       Holds the information about the limit for pagination
     * @param response        Represents the response json
     */
    protected void applyLimitsAndLoadPageSummary(final ListInterpreter listInterpreter, final JsonObject limitInfo, final JsonObject response) {
        final long endPosition;

        if (Objects.isNull(limitInfo)) {
            listInterpreter.applyDefaultLimit();
            endPosition = NumericalConstant.FIFTY;
        } else {
            final int start = limitInfo.getInt(ListActionFields.START);
            final int count = limitInfo.getInt(ListActionFields.COUNT);
            endPosition = start + count;

            listInterpreter.setLimits(start, count);
        }
        final long totalHits = listInterpreter.fetchTotalHits();

        response.put(ListActionFields.TOTAL_RECORDS, totalHits);
        response.put(ListActionFields.HAS_MORE, endPosition <= totalHits);
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
    private void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson, final EntityListInterpreter listInterpreter, final Provider provider) {
        final String filterIdKey = FilterFields.FILTER_ID.getName();

        if (requestJson.containsKey(filterIdKey)) {
            final long filterId = requestJson.getLong(filterIdKey);
            final Filter filter = filterManager.get(orgId, filterId);
            final Collection<FilterCriteria> criteriaList = filter.getCriteriaList();

            if (filter.isDefault() && Filter.Category.COMMON == filter.getCategory()) {
                final FilterValueResolver valueResolver = new FilterValueResolver(orgId, userId, DefaultFilter.valueOf(filter.getAliasName()));

                criteriaList.forEach(valueResolver::resolve);
            }
            listInterpreter.applyCriteria(orgId, provider, criteriaList);

            filterManager.updateRecentlyAppliedFilter(orgId, userId, filter.getFilterProvider().getId(), filterId);
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
     * Applies the criteria in the {@link EntityListInterpreter}
     * </p>
     *
     * @param orgId           Represents the org id
     * @param provider        Represents the {@link Provider}
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param criteria        Represents the filter criteria
     */
    private void applyCriteria(final long orgId, final Provider provider,
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
     * Gets the {@link EntityListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param request               Represents the request {@link JsonObject}
     * @param provider              Represents the {@link Provider}
     * @param joinLoader            Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link ListInterpreter}
     */
    protected EntityListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request, final Provider provider,
                                                             final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new EntitySQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * Formats the given filter parameters into a structured JSON representation and appends the result to the provided request JSON.
     *
     * @param orgId        The ID of the organization for which the filtering is being performed; must not be null
     * @param requestJson  The JSON object to which the formatted filter structure will be added; must not be null
     * @param filterParams The deque representing filter tokens or expressions to be transformed into JSON format;
     */
    protected void formatToFilterJson(final Long orgId, final JsonObject requestJson, final Deque<String> filterParams) {
        final JsonArray array = Json.array();

        for (final String filterParam : filterParams) {
            final JsonObject jsonObject = Json.object();
            final String[] split = filterParam.split(":");
            final String field = split[0].trim();
            final String typeWithValue = split[1].trim();
            final int openBracketIndex = typeWithValue.indexOf('(');
            final String comparatorKey = typeWithValue.substring(0, openBracketIndex);
            final Source source = getSource();
            final Optional<FieldInfo> optionalFieldInfo = TypeChecker.isNumber(field)
                    ? Optional.of(fieldService.getField(orgId, NumberUtils.parseNumber(field).longValue()))
                    : fieldService.getDefaultFieldByAliasName(source, field);

            if (optionalFieldInfo.isPresent()) {
                final FieldInfo fieldInfo = optionalFieldInfo.get();
                final Comparator comparator = comparatorService.getByName(getComparatorName(comparatorKey));
                final String[] values = typeWithValue.substring(openBracketIndex + 1, typeWithValue.indexOf(')')).split("\\s*,\\s*");

                jsonObject.put("field", Json.object().put("fieldId", fieldInfo.id()));
                jsonObject.put("sourceId", fieldSourceService.getSource(getSource().getName()).id());
                jsonObject.put("connectorId", FilterConnector.AND.getId());
                jsonObject.put("comparatorId", comparator.getId());

                if (values.length >= 1) {
                    final JsonArray valueArray = Json.array();

                    for (final String value : values) {
                        if (!value.equalsIgnoreCase("null")) {
                            final JsonObject valueJson = Json.object();

                            switch (fieldInfo.storeType()) {
                                case DROPDOWN, MULTI_SELECT -> valueJson.put("valueEntityId", value);
                                default -> valueJson.put("value", value);
                            }

                            valueArray.put(valueJson);
                        }
                    }

                    if (valueArray.size() > 0) {
                        jsonObject.put("values", valueArray);
                    }
                }
                array.put(jsonObject);
            }
        }

        requestJson.put("criteria", array);
    }

    /**
     * Gets the {@code Source}.
     *
     * @return The {@code Source}.
     */
    protected abstract Source getSource();

    /**
     * The comparator name for the given comparator.
     *
     * @param comparator The comparator string.
     * @return
     */
    protected final String getComparatorName(final String comparator) {
        return switch (comparator) {
            case "eq", "between" -> "equal";
            case "neq" -> "notEqual";
            case "in" -> "contains";
            case "nin" -> "notContains";
            case "lt" -> "lt";
            case "lte" -> "lte";
            case "gt" -> "gt";
            case "gte" -> "gte";
            case "like" -> "contains";
            default -> "";
        };
    }

    /**
     * Gets the {@code Provider}.
     *
     * @return The {@code Provider}.
     */
    protected abstract Provider getProvider();

    /**
     * <p>
     * Fetches the structure based on the configured fields in the {@link ListInterpreter}.
     * </p>
     *
     * @param orgId                 Represents an organization id
     * @param source                Represents the {@link Source}
     * @param listInterpreter       Represents the {@link ListInterpreter}
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
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, userId, fieldSourceService.getSource(source.getName()).id());

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
            fieldInfoJson.put(ListActionFields.HIDDEN, configIsNotNull && !config.isEnabledForWeb());
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
     * Applies the default criteria for the entity list action
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     */
    protected void applyDefaultCriteria(final ListInterpreter listInterpreter) {
    }
}
