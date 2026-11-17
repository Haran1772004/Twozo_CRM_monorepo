package com.tlc.crm.core.features.action.quota.list;

import com.tlc.commons.Counter;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.list.SecureListAction;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.common.list.transformer.model.ListResponseStructureType;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.action.quota.list.model.QuotaListDataContainer;
import com.tlc.crm.core.features.api.quota.util.DateIntervalResolver;
import com.tlc.crm.core.features.api.quota.manager.QuotaListViewUserFilterManager;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.sql.common.api.dml.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Manages the quota list related web actions
 *
 * @author kishore J
 * @version 1.0
 */
public abstract class QuotaListAction extends SecureListAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaListAction.class);
    private static final String FORECAST_CATEGORY = "FORECAST_CATEGORY";
    private static final String LESS_EQUAL = "lte";
    private static final String GREATER_EQUAL = "gte";

    private final QuotaListViewUserFilterManager quotaListViewUserFilterManager;
    private final ComparatorService comparatorService;

    protected QuotaListAction() {
        quotaListViewUserFilterManager = QuotaListViewUserFilterManager.getInstance();
        comparatorService = ComparatorService.getInstance();
    }

    /**
     * Performs the list action process.
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Represents the {@link CrmResponse} object.
     */
    @Override
    protected void processListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
        final Provider provider = getProvider();
        final I18nResolutionAdapter i18NResolutionAdapter = listRequestData.getI18nPlaceholderResolver();
        final JsonObject requestJson = listRequestData.getRequestJson();
        final JsonObject response = Json.object();
        final QuotaListInterpreter quotaListInterpreter = getListInterpreter(listRequestData);

        if (requestJson.optBoolean(ListActionFields.FETCH_STRUCTURE)) {
            response.put(ListActionFields.STRUCTURE, fetchStructure(quotaListInterpreter, i18NResolutionAdapter, listRequestData));
        }

        if (requestJson.optBoolean(ListActionFields.FETCH_DATA)) {
            final JsonArray fields = requestJson.optJsonArray(ListActionFields.FIELDS);
            final Collection<ConfiguredField> configuredFields = new LinkedHashSet<>();
            final Counter order = new Counter(1);
            final int fieldSize = fields.size();

            for (int index = 0; index < fieldSize; index++) {
                final JsonObject jsonObject = fields.getJsonObject(index);
                final String fieldName = jsonObject.getString(ListActionFields.FIELD);
                final ConfiguredField configuredField = new ConfiguredField(fieldName);
                final boolean isEnabled = jsonObject.optBoolean(ListActionFields.IS_ENABLED, true);

                configuredField.setOrder(order.getAndIncrement());
                configuredFields.add(configuredField);
                configuredField.setEnabled(isEnabled);
                quotaListInterpreter.selectClause(configuredField);
            }
            quotaListInterpreter.updateConfiguredFields(configuredFields);
            updateRecentlyAppliedFilter(listRequestData);
            applyFilter(requestJson, quotaListInterpreter, listRequestData);
            applyLimitsAndLoadPageSummary(listRequestData.getOrgId(), listRequestData.getUserId(), requestJson, quotaListInterpreter, requestJson.optJsonObject(ListActionFields.LIMIT), response);

            loadResponse(listRequestData, quotaListInterpreter, response, crmResponse, provider);
        }
        crmResponse.put(ListActionFields.TABLE, response);
    }

    /**
     * {@inheritDoc}
     *
     * @return API type of the current {@link com.tlc.web.Action}.
     */
    @Override
    protected final Optional<String> fetchApiType() {
        return Optional.of(ListActionFields.LIST);
    }

    /**
     * <p>
     * Updates the recently applied filter criteria for the given user within the organization.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     */
    private void updateRecentlyAppliedFilter(final ListRequestData listRequestData) {
        final JsonObject requestJson = listRequestData.getRequestJson();

        if (requestJson.containsKey(ListActionFields.CRITERIA)) {
            quotaListViewUserFilterManager.updateRecentlyAppliedFilter(listRequestData.getOrgId(), listRequestData.getUserId(),
                    requestJson.getJsonObject(ListActionFields.CRITERIA).put(QuotaListFields.LIST_TYPE, getName()));
        }
    }

    /**
     * <p>
     * Applies filter criteria from the request JSON to the quota list interpreter.
     * </p>
     *
     * @param requestJson          The request {@link JsonObject} containing the criteria
     * @param quotaListInterpreter The {@link QuotaListInterpreter} to apply criteria to
     * @param listRequestData      The {@link ListRequestData} containing organization and logged-in user details
     */
    private void applyFilter(final JsonObject requestJson, final QuotaListInterpreter quotaListInterpreter, final ListRequestData listRequestData) {
        if (requestJson.containsKey(ListActionFields.CRITERIA)) {
            final JsonObject criteriaJson = requestJson.getJsonObject(ListActionFields.CRITERIA);
            final Set<String> fields = criteriaJson.keySet();
            final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>(fields.size());

            fields.forEach(field -> {
                switch (field) {
                    case QuotaListFields.USER_IDS, QuotaListFields.USER_ID, QuotaListFields.TEAM_IDS ->
                            filterCriteriaList.add(build(field, Operator.IN.name().toLowerCase(), QuotaListFields.USER_ID.equals(field) ? criteriaJson.getLong(QuotaListFields.USER_ID) :
                                    criteriaJson.getJsonArray(field).toList()));
                    case QuotaListFields.DATE_INTERVAL -> {
                        final Interval<Long, Long> interval = DateIntervalResolver.resolve(criteriaJson.getJsonObject(QuotaListFields.DATE_INTERVAL));

                        filterCriteriaList.add(build(QuotaListFields.START_TIME, LESS_EQUAL, interval.end()));
                        filterCriteriaList.add(build(QuotaListFields.END_TIME, GREATER_EQUAL, interval.start()));
                    }
                    case QuotaListFields.METRIC ->
                            filterCriteriaList.add(build(QuotaListFields.METRIC, Operator.EQUAL.name().toLowerCase(), criteriaJson.getLong(QuotaListFields.METRIC)));
                    default -> {
                    }
                }
            });
            quotaListInterpreter.applyCriteria(filterCriteriaList);

            // USER_IDS are persisted in the common recently-applied filter so that
            // Team View and Hierarchy View share the same selected users.
            updateCommonUserList(listRequestData, criteriaJson);
        }
    }

    /**
     * <p>
     * Synchronizes the selected users between Team View and Hierarchy View by persisting
     * USER_IDS through the common quota filter manager.
     * </p>
     *
     * @param listRequestData Represents the current {@link ListRequestData}
     * @param criteriaJson    Represents the criteria {@link JsonObject}
     */
    private void updateCommonUserList(final ListRequestData listRequestData, final JsonObject criteriaJson) {
        if (criteriaJson.containsKey(QuotaListFields.USER_IDS)) {
            final JsonObject userFilterJson = Json.object();

            userFilterJson.put(QuotaListFields.USER_IDS, criteriaJson.getJsonArray(QuotaListFields.USER_IDS));
            quotaListViewUserFilterManager.updateRecentlyAppliedFilter(listRequestData.getOrgId(), listRequestData.getUserId(), userFilterJson);
        }
    }


    /**
     * Builds a {@link FilterCriteria} with the given field name, operator, and a single value.
     *
     * @param fieldName      The alias name for the criteria field
     * @param comparatorName Represents the comparator name
     * @param value          The value to set on the criteria
     * @return The constructed {@link FilterCriteria}
     */
    private FilterCriteria build(final String fieldName, final String comparatorName, final Object value) {
        final FilterCriteria filterCriteria = new FilterCriteria();
        final CriteriaField criteriaField = new CriteriaField();
        final Comparator comparator = comparatorService.getByName(comparatorName);

        criteriaField.setAliasName(fieldName);
        filterCriteria.setCriteriaField(criteriaField);
        filterCriteria.setComparator(comparator);
        filterCriteria.setConnector(FilterConnector.AND);

        if (value instanceof Collection<?> values) {

            values.forEach(criteriaItem -> {
                final CriteriaValue criteriaValue = new CriteriaValue();

                criteriaValue.setValue(criteriaItem);
                filterCriteria.addCriteriaValue(criteriaValue);
            });
        } else {
            final CriteriaValue criteriaValue = new CriteriaValue();

            criteriaValue.setValue(value);
            filterCriteria.addCriteriaValue(criteriaValue);
        }
        return filterCriteria;
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
    private void loadResponse(final ListRequestData listRequestData, final QuotaListInterpreter listInterpreter,
                              final JsonObject response, final CrmResponse crmResponse, final Provider provider) {
        final Collection<Map<String, Object>> results = listInterpreter.getResult();

        if (results.isEmpty()) {
            response.put(ListActionFields.ROWS, results);
            crmResponse.put(ListActionFields.TABLE, response);
            return;
        }
        final ListStructure listStructure = listInterpreter.structure();
        final ModelTransformer modelTransformer = listStructure.getProviderTransformer();
        final long orgId = listRequestData.getOrgId();
        final I18nResolutionAdapter i18nResolutionAdapter = listRequestData.getI18nPlaceholderResolver();
        final JsonObject requestJson = listRequestData.getRequestJson();
        final QuotaListDataContainer quotaListDataContainer;

        if (Objects.isNull(modelTransformer) || results.isEmpty()) {
            quotaListDataContainer = new QuotaListDataContainer(orgId, i18nResolutionAdapter, results);
        } else {
            final long transformerTime = System.currentTimeMillis();
            final TimezoneInfo timezoneInfo = listRequestData.getTimezoneInfo();
            final TransformerRequest transformerRequest;
            final JsonObject criteriaJson = requestJson.getJsonObject(ListActionFields.CRITERIA);

            if (criteriaJson.containsKey(QuotaListFields.VIEW_TYPE)) {
                transformerRequest = new TransformerRequest(orgId, results, timezoneInfo, CrmRequest.ClientType.WEB, ListResponseStructureType.get(criteriaJson.getString(QuotaListFields.VIEW_TYPE)));
            } else {
                transformerRequest = new TransformerRequest(orgId, results, timezoneInfo);
            }
            final TransformerResponse transformerResponse = modelTransformer.transform(transformerRequest);

            LOGGER.debug("Quota list action - [{}]: Model transformer process completed in {}ms, Org ID: {}", getProvider(), System.currentTimeMillis() - transformerTime, orgId);
            quotaListDataContainer = new QuotaListDataContainer(orgId, i18nResolutionAdapter, transformerResponse.getResults(), requestJson, transformerResponse.getUserIds());
        }
        final Collection<Long> userIds = quotaListDataContainer.getUserIds();
        final Collection<Map<String, Object>> entityRows = quotaListDataContainer.getData(listInterpreter);

        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            response.put(ListActionFields.ROWS, entityRows);
            crmResponse.put(ListActionFields.TABLE, response);
        } else {
            dispatchEntities(listRequestData.getOrgId(), userIds, entityRows, response, crmResponse);
        }
    }

    /**
     * <p>
     * Fetches the structure based on the configured fields in the {@link QuotaListInterpreter}.
     * </p>
     *
     * @param listInterpreter       Represents the {@link QuotaListInterpreter}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @param listRequestData       Represents the {@link ListRequestData}
     * @return {@link JsonObject} of structure
     */
    private JsonObject fetchStructure(final QuotaListInterpreter listInterpreter, final I18nResolutionAdapter i18NResolutionAdapter,
                                      final ListRequestData listRequestData) {
        final Collection<FieldConfig> fieldConfigs = listInterpreter.fetchConfiguredFields();
        final ForecastType forecastType = ForecastType.getById(listRequestData.getRequestJson().optShort(QuotaListFields.FORECAST_TYPE, ForecastType.WON.getId()));
        final JsonObject structureJson = Json.object();
        final JsonArray fieldArray = Json.array();
        final String tooltipName = StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, i18NResolutionAdapter.get(forecastType.isWon() ? "i18n.quota.forecast.include" :
                "i18n.quota.forecast.not.include"), i18NResolutionAdapter.get("i18n.quota.won.deals.placeholder").toLowerCase(listRequestData.getLocale()));

        for (final FieldConfig fieldConfig : fieldConfigs) {
            final JsonObject fieldJson = Json.object();
            final FieldConfig.Config config = fieldConfig.getConfig();
            final boolean isPersistent = fieldConfig.isPersistent();
            final String fieldName = fieldConfig.getName();
            final String displayName = i18NResolutionAdapter.get(fieldConfig.getDisplayName());
            final boolean isMandatoryField = config.isMandatoryField();

            fieldJson.put(ListActionFields.NAME, isPersistent ? fieldConfig.getIdAsString() : fieldName);
            fieldJson.put(ListActionFields.DISPLAY_NAME, isPersistent ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, displayName, QuotaListFields.FORECAST) : displayName);
            fieldJson.put(QuotaListFields.IS_DRAG_AND_DROP_ENABLED, !isMandatoryField);
            fieldJson.put(ListActionFields.IS_MANDATORY_FIELD, isMandatoryField);
            fieldJson.put(ListActionFields.FIELD_TYPE, isPersistent ? FORECAST_CATEGORY : fieldName.toUpperCase());
            fieldJson.put(ListActionFields.IS_ENABLED, config.isEnabledForWeb());

            if (isPersistent) {
                final JsonObject configJson = Json.object();

                configJson.put(QuotaListFields.TOOLTIP_LABEL, tooltipName);
                fieldJson.put(ListActionFields.CONFIG, configJson);
            }
            fieldArray.put(fieldJson);
        }
        structureJson.put(ListActionFields.COLUMNS, fieldArray);

        return structureJson;
    }

    /**
     * <p>
     * Gets the {@link EntityListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @return {@link ListInterpreter}
     */
    protected abstract QuotaListInterpreter getListInterpreter(final ListRequestData listRequestData);

    /**
     * Gets the name of the provider for the list action.
     *
     * @return The {@link Provider}.
     */
    protected abstract Provider getProvider();

    /**
     * <p>
     * Gets the list view name for the quota list action
     * </p>
     *
     * @return the list view name
     */
    protected abstract String getName();
}