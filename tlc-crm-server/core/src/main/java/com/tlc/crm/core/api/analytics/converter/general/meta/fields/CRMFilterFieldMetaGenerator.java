package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.config.model.view.filter.OperatorType;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;
import com.tlc.commons.util.TypeChecker;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.api.analytics.entity.config.fields.filter.FilterValueTypes;

import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.fields.ComparatorFields;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Objects;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides an abstract way for retrieve the filter related fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
class CRMFilterFieldMetaGenerator extends AbstractFieldMetaGenerator implements ReportsMetaResponse<JsonObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMFilterFieldMetaGenerator.class);

    private final ReportModularFieldService reportModularFieldService;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final AnalyticsFieldService analyticsFieldService;
    private static final String MULTI_DROPDOWN = "MULTI_DROPDOWN";
    private static final String PIPELINE_STAGE = "Stage";
    private static final String LIFE_CYCLE_STAGE = "Lifecycle Status";

    private static final List<String> CONFIG_KEYS = List.of(
            PresetConfig.Configuration.MIN.getName(),
            PresetConfig.Configuration.MAX.getName(),
            PresetConfig.Configuration.MIN_LENGTH.getName(),
            PresetConfig.Configuration.MAX_LENGTH.getName(),
            PresetConfig.Configuration.MAX_LIMIT.getName()
    );

    /**
     * <p>
     * Private constructor that initializes all required service instances
     * </p>
     */
    private CRMFilterFieldMetaGenerator() {
        reportModularFieldService = ReportModularFieldService.getInstance();
        fieldService = FieldService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
    }

    private static class Instance {
        private static final CRMFilterFieldMetaGenerator INSTANCE = new CRMFilterFieldMetaGenerator();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link CRMFilterFieldMetaGenerator}
     * </p>
     *
     * @return the singleton {@link CRMFilterFieldMetaGenerator} instance
     */
    public static CRMFilterFieldMetaGenerator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Fetches report modular fields via callback, processes filter field metadata,
     * and sends response asynchronously via {@code crmResponse}.
     *
     * <p>Returns empty JSON — actual response is sent from the callback.</p>
     *
     * @param analyticsRequest the analytics request containing orgId and response handle
     * @param jsonObject       the request data containing {@code moduleId}
     * @param i18nResolver     resolver for internationalized display names
     * @return empty {@link JsonObject} (response sent asynchronously)
     */
    @Override
    public @NonNull JsonObject getResponse(final @NonNull AnalyticsRequest analyticsRequest,
                                           final @NonNull JsonObject jsonObject,
                                           final @NonNull I18nResolver i18nResolver) {
        final Long orgId = analyticsRequest.orgId();
        final Integer moduleId = jsonObject.getInt(CRMReportKeys.MODULE_ID);
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                try {
                    final Collection<ReportModularField> activeFields = reportFieldMap.values()
                            .stream().filter(ReportModularField::getIsActive).collect(Collectors.toList());

                    processFilterFields(orgId, moduleId, activeFields, analyticsRequest, i18nResolver);
                } catch (final Exception exception) {
                    LOGGER.error("Failed to process filter fields for orgId: {}, moduleId: {}", orgId, moduleId, exception);
                    crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.FILTER_FIELD_FAILED, "i18n.analytics.filter.fields.get.failed"));
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("Failed to fetch report fields for filter meta, orgId: {}", orgId, cause);
                crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.FILTER_FIELD_FAILED, "i18n.analytics.filter.fields.get.failed"));
                crmResponse.send();
            }
        });

        return Json.object();
    }

    /**
     * <p>
     * Processes and constructs the filter field metadata response for the given module,
     * including comparators, functions, periodic filters, and dropdown configurations
     * </p>
     *
     * @param orgId            the organization ID
     * @param moduleId         the module ID to filter fields by
     * @param allReportFields  the collection of all active {@link ReportModularField} entries
     * @param analyticsRequest the {@link AnalyticsRequest} containing request context
     * @param i18nResolver     an instance of {@link I18nResolver} for resolving internationalization details
     * @throws Exception if any error occurs during filter field processing
     */
    @SuppressWarnings("java:S107")
    private void processFilterFields(final Long orgId, final int moduleId,
                                     final Collection<ReportModularField> allReportFields,
                                     final AnalyticsRequest analyticsRequest,
                                     final I18nResolver i18nResolver) throws Exception {
        final CrmResponse crmResponse = analyticsRequest.crmResponse();
        final Map<Long, StoreType> fieldTypeMap = new HashMap<>();
        final Map<Long, Object> dropDownMap = new HashMap<>();
        final Map<Long, JsonObject> fieldConfigMap = new HashMap<>();
        final Collection<Long> crmFieldIds = new HashSet<>();
        final Collection<Long> anaFieldIds = new HashSet<>();
        final JsonObject filterFieldResponse = Json.object();
        final JsonArray periodicFilterArray = Json.array();
        final JsonArray filterArray = Json.array();
        final Collection<ReportModularField> moduleReportFields = getAuthorizedReportModularField(analyticsRequest,
                ModularFieldUtil.filterByModule(moduleId, allReportFields));
        final Collection<CRMFunction> functions = List.of(CRMFunction.values());

        for (final ReportModularField reportModularField : moduleReportFields) {
            final ModularField modularField = reportModularField.getModularField();
            if (Objects.nonNull(modularField)) {
                if (modularField instanceof CRMSystemModularField systemField) {
                    Optional.ofNullable(systemField.getAnaFieldId()).ifPresent(anaFieldIds::add);
                }
                Optional.ofNullable(modularField.getCrmFieldId()).ifPresent(crmFieldIds::add);
            }
        }

        if (!anaFieldIds.isEmpty()) {
            populateAnalyticsFieldTypes(anaFieldIds, fieldTypeMap);
        }

        if (!crmFieldIds.isEmpty()) {
            populateCrmFieldDetails(orgId, crmFieldIds, fieldTypeMap, fieldConfigMap, dropDownMap);
        }

        final JsonObject defaultPeriodicFilterJson = Json.object();

        CodeBlockExecutor.forEachSafely(moduleReportFields, reportModularField ->
        {
            final ModularField modularField = reportModularField.getModularField();

            if (Objects.nonNull(modularField)) {
                final JsonObject fieldJson = setFilterFields(reportModularField, fieldTypeMap, fieldConfigMap,
                        dropDownMap, analyticsRequest, false, i18nResolver);

                if (Objects.nonNull(fieldJson)) {

                    if (modularField.getModularFieldConfig().isFilterAllowed()) {
                        filterArray.put(fieldJson);
                    }

                    if (modularField.getModularFieldConfig().isPeriodicFilterAllowed()) {

                        if (modularField.getModularFieldConfig().isDefaultFilter() && !reportModularField.isCustom()) {
                            buildDefaultPeriodicFilter(defaultPeriodicFilterJson, reportModularField, functions);
                        }

                        periodicFilterArray.put(setFilterFields(reportModularField, fieldTypeMap, fieldConfigMap,
                                dropDownMap, analyticsRequest, true, i18nResolver));
                    }
                }
            }
        });

        final JsonArray sortedFilterArray = toSortedJsonArray(filterArray, CRMReportKeys.FIELD_ID);
        final JsonArray sortedPeriodicFilterArray = toSortedJsonArray(periodicFilterArray, CRMReportKeys.FIELD_ID);

        setComparators(filterFieldResponse, i18nResolver);
        setFunctions(functions, filterFieldResponse, i18nResolver);
        filterFieldResponse.put(FilterFieldKeys.PERIODIC_FILTERS, sortedPeriodicFilterArray);
        filterFieldResponse.put(FilterFieldKeys.FILTERS, sortedFilterArray);
        filterFieldResponse.put(FilterFieldKeys.DEFAULT_PERIODIC_FILTER_VALUE, defaultPeriodicFilterJson);

        filterFieldResponse.forEach(crmResponse::put);
        crmResponse.send();
    }

    /**
     * <p>
     * Populates the field type and store type maps from analytics field IDs
     * </p>
     *
     * @param anaFieldIds  the collection of analytics field IDs to look up
     * @param fieldTypeMap the map to populate with field ID to {@link StoreType} mappings
     */
    private void populateAnalyticsFieldTypes(final Collection<Long> anaFieldIds,
                                             final Map<Long, StoreType> fieldTypeMap) throws Exception {
        final Collection<AnalyticsField> analyticsFields = analyticsFieldService.getAnalyticsFieldsByIds(anaFieldIds).values();
        final Map<Long, Collection<Long>> anaFieldTypeMap = new HashMap<>();

        for (final AnalyticsField analyticsField : analyticsFields) {
            anaFieldTypeMap.computeIfAbsent(analyticsField.getFieldTypeId(), value -> new HashSet<>()).add(analyticsField.id());
        }

        if (!anaFieldTypeMap.isEmpty()) {
            final Collection<Long> fieldTypeIds = anaFieldTypeMap.keySet();
            final Collection<FieldType> fieldTypes = fieldTypeService.getFieldTypesByIds(fieldTypeIds);

            CodeBlockExecutor.forEachSafely(fieldTypes, fieldType ->
            {
                final Long fieldTypeId = fieldType.id();
                final StoreType storeType = fieldType.storeType();

                if (Objects.nonNull(storeType)) {
                    final Collection<Long> fieldIds = anaFieldTypeMap.get(fieldTypeId);

                    fieldIds.forEach(fieldId -> fieldTypeMap.put(fieldId, storeType));
                }
            });
        }
    }

    /**
     * <p>
     * Populates CRM field details including field types, configurations, dropdowns, and store types
     * </p>
     *
     * @param orgId        the organization ID
     * @param crmFieldIds  the collection of CRM field IDs to look up
     * @param fieldTypeMap the map to populate with field ID to {@link StoreType} mappings
     * @param fieldConfigMap the map to populate with field ID to preset config JSON mappings
     * @param dropDownMap  the map to populate with field ID to dropdown value mappings
     */
    private void populateCrmFieldDetails(final Long orgId,
                                         final Collection<Long> crmFieldIds,
                                         final Map<Long, StoreType> fieldTypeMap,
                                         final Map<Long, JsonObject> fieldConfigMap,
                                         final Map<Long, Object> dropDownMap) {
        fieldService.getFieldsById(orgId, crmFieldIds).values().forEach(fieldInfo ->
        {
            final FieldStore fieldStore = fieldInfo.fieldStore();
            final Long fieldId = fieldInfo.id();

            if (Objects.nonNull(fieldStore)) {
                final StoreType storeType = fieldStore.storeType();

                if (Objects.nonNull(storeType)) {
                    final PresetConfig presetConfig = fieldInfo.presetConfig();

                    if (Objects.nonNull(presetConfig)) {
                        extractFieldConfig(fieldId, presetConfig, fieldConfigMap);
                    }
                    fieldTypeMap.put(fieldId, storeType);

                    switch (storeType) {
                        case OWNER -> dropDownMap.put(fieldId, FilterFieldKeys.USER_LIST);
                        case COMPANY, COMPANY_ONBOARD -> dropDownMap.put(fieldId, FilterFieldKeys.COMPANY_LIST);
                        case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                                dropDownMap.put(fieldId, FilterFieldKeys.CONTACT_LIST);
                        default -> populateDropdownFromPreset(fieldId, storeType, presetConfig, dropDownMap);
                    }
                }
            }
        });
    }

    /**
     * <p>
     * Extracts preset configuration values from a field's preset config and stores them in the config map
     * </p>
     *
     * @param fieldId        the field ID to associate with the extracted config
     * @param presetConfig   the {@link PresetConfig} containing the field's configuration
     * @param fieldConfigMap the map to populate with field ID to config JSON mappings
     */
    private void extractFieldConfig(final Long fieldId,
                                    final PresetConfig presetConfig,
                                    final Map<Long, JsonObject> fieldConfigMap) {
        final JsonObject fieldConfig = presetConfig.presetConfigAsJson();
        final JsonObject configJson = Json.object();
        final String validationKey = PresetConfig.Configuration.VALIDATION.getName();

        for (final String key : CONFIG_KEYS) {
            if (fieldConfig.containsKey(key)) {
                configJson.put(key, fieldConfig.get(key));
            }
        }

        if (fieldConfig.containsKey(validationKey)) {
            configJson.put(validationKey, fieldConfig.getJsonObject(validationKey));
        }

        fieldConfigMap.put(fieldId, configJson);
    }

    /**
     * <p>
     * Populates the dropdown map for a field based on its preset configuration
     * </p>
     *
     * @param fieldId      the field ID
     * @param storeType    the {@link StoreType} of the field
     * @param presetConfig the {@link PresetConfig} of the field (may be null)
     * @param dropDownMap  the map to populate with field ID to dropdown value mappings
     */
    private void populateDropdownFromPreset(final Long fieldId,
                                            final StoreType storeType,
                                            final PresetConfig presetConfig,
                                            final Map<Long, Object> dropDownMap) {
        if (Objects.nonNull(presetConfig)) {
            if (StoreType.isSelectorType(storeType) && presetConfig.dropdownType() != DropdownType.HIDDEN_OPTION) {
                dropDownMap.put(fieldId, fieldId);
            } else if (presetConfig.contains(PresetConfig.Configuration.FILTER_DROPDOWN)) {
                dropDownMap.put(fieldId, presetConfig.filterDropdown());
            } else {
                dropDownMap.put(fieldId, presetConfig.dropdown());
            }
        }
    }

    /**
     * <p>
     * Builds the default periodic filter JSON with preset values for days-based filtering
     * </p>
     *
     * @param defaultPeriodicFilterJson the JSON object to populate with default periodic filter values
     * @param reportModularField        the {@link ReportModularField} to set as default filter field
     * @param functions                 the collection of available {@link CRMFunction} entries
     */
    private void buildDefaultPeriodicFilter(final JsonObject defaultPeriodicFilterJson,
                                            final ReportModularField reportModularField,
                                            final Collection<CRMFunction> functions) {
        final JsonArray valueArray = Json.array();
        final JsonObject defaultJson = Json.object();

        defaultJson.put(FilterFieldKeys.VALUE, NumericalConstant.NINETY);
        valueArray.put(defaultJson);
        defaultPeriodicFilterJson.put(FilterFieldKeys.VALUES, valueArray);
        defaultPeriodicFilterJson.put(FilterFieldKeys.COMPARATOR_ID, OperatorType.IN_THE_LAST.getTypeCode());
        defaultPeriodicFilterJson.put(FilterFieldKeys.FIELD_ID, reportModularField.id());

        functions.stream()
                .filter(CRMFunction::isPeriodicFilter)
                .filter(function -> Objects.equals(FilterFieldKeys.DAYS, function.name()))
                .findFirst()
                .ifPresent(function -> defaultPeriodicFilterJson.put(FilterFieldKeys.FUNCTION_ID, function.getTypeCode()));
    }

    /**
     * <p>
     * Converts a {@link JsonArray} into a sorted {@link JsonArray} based on the specified Long field key
     * </p>
     *
     * @param jsonArray the source JSON array to sort
     * @param sortKey   the key whose Long value is used for sorting
     * @return a new sorted {@link JsonArray}
     */
    private JsonArray toSortedJsonArray(final JsonArray jsonArray, final String sortKey) {
        final List<JsonObject> list = new ArrayList<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            list.add(jsonArray.getJsonObject(index));
        }

        list.sort(java.util.Comparator.comparingLong(json -> json.getLong(sortKey)));

        final JsonArray sortedArray = Json.array();
        list.forEach(sortedArray::put);
        return sortedArray;
    }

    /**
     * <p>
     * Sets the comparator details for the filter fields
     * </p>
     *
     * @param filterFieldResponse    the JSON object to which the comparator details will be added
     * @param i18nResolver           an instance of {@link I18nResolver} for resolving internationalization (i18n) details
     */
    private void setComparators(final JsonObject filterFieldResponse, final I18nResolver i18nResolver) {
        final JsonObject comparatorJson = Json.object();

        try {
            final List<OperatorType> sortedComparators = Stream.of(OperatorType.values()).filter(compare -> compare != null && compare.isEnabled())
                    .sorted(java.util.Comparator.comparingLong(OperatorType::getTypeCode)).toList();
            CodeBlockExecutor.forEachSafely(sortedComparators, comparator ->
            {
                final JsonObject jsonObject = Json.object();
                final Integer comparatorId = comparator.getTypeCode();
                final Collection<CRMFunction> comparatorFunctions = CRMFunction.getSupportedFunctions(comparatorId);
                final FilterValueTypes filterValueTypes = FilterValueTypes.getByOperatorType(comparatorId);

                jsonObject.put(ComparatorFields.NAME.getName(), i18nResolver.get(comparator.getI18nKeyOfDisplayName()));
                jsonObject.put(ComparatorFields.HAS_VALUE.getName(), comparator.isHasValue());
                jsonObject.put(ComparatorFields.HAS_FUNCTION.getName(), comparator.isHasFunction());

                if (Objects.nonNull(comparatorFunctions) && !comparatorFunctions.isEmpty()) {
                    final Collection<Integer> functionIds = new HashSet<>();

                    CodeBlockExecutor.forEachSafely(comparatorFunctions, comparatorFunction ->
                            functionIds.add(comparatorFunction.getTypeCode()));

                    jsonObject.put(FilterFieldKeys.FUNCTION_IDS, functionIds);
                }

                if (Objects.nonNull(filterValueTypes)) {
                    jsonObject.put(FilterFieldKeys.TYPE, i18nResolver.get(filterValueTypes.getI18nKey()));

                    if (Objects.equals(filterValueTypes, FilterValueTypes.MULTI_SELECT)) {
                        jsonObject.put(FilterFieldKeys.CONFIG, Json.object().put(PresetConfig.Configuration.MAX_LIMIT.getName(), NumericalConstant.TEN));
                    }
                }

                comparatorJson.put(String.valueOf(comparatorId), jsonObject);
            });
        } catch (final Exception exception) {
            LOGGER.error("Failed to fetch filter fields comparator", exception);
            throw ErrorCode.get(AnalyticsErrorCodes.FILTER_FIELD_FAILED,
                    "i18n.analytics.filter.fields.get.failed");
        }

        filterFieldResponse.put(FilterFieldKeys.COMPARATORS, comparatorJson);
    }

    /**
     * <p>
     * Sets the function details for the filter fields
     * </p>
     *
     * @param functions           the collection of {@link CRMFunction} containing the filter function details
     * @param filterFieldResponse the JSON object to which the function details will be added
     * @param i18nResolver        an instance of {@link I18nResolver} for resolving internationalization (i18n) details
     */
    private void setFunctions(final Collection<CRMFunction> functions,
                              final @NonNull JsonObject filterFieldResponse,
                              final @NonNull I18nResolver i18nResolver) {
        final JsonObject functionJson = Json.object();

        try {
            final List<CRMFunction> sortedFunctions = functions.stream()
                    .filter(crmFunction -> crmFunction != null && crmFunction.isPeriodicFilter())
                    .sorted(java.util.Comparator.comparingLong(CRMFunction::getTypeCode)).toList();
            CodeBlockExecutor.forEachSafely(sortedFunctions, function ->
            {
                final JsonObject jsonObject = Json.object();

                if (Objects.equals(CommonKeys.CUSTOM, function.getFunctionName()
                        .toUpperCase())) {
                    jsonObject.put(CommonKeys.IS_CUSTOM, true);
                }
                jsonObject.put(CommonKeys.NAME, i18nResolver.get(function.getI18nKeyOfDisplayName()));
                functionJson.put(String.valueOf(function.getTypeCode()), jsonObject);
            });
        } catch (final Exception exception) {
            LOGGER.error("Failed to fetch filter fields function", exception);
            throw ErrorCode.get(AnalyticsErrorCodes.FILTER_FIELD_FAILED,
                    "i18n.analytics.filter.fields.get.failed");
        }

        filterFieldResponse.put(FilterFieldKeys.FUNCTIONS, functionJson);
    }

    /**
     * <p>
     * Creates a JsonObject representing the {@link ModularField} and its associated filter properties
     * </p>
     *
     * @param reportModularField    the {@link ReportModularField} containing the field details
     * @param fieldTypeMap          a map that associates field IDs with their respective {@link StoreType}
     * @param fieldConfigMap        a map that associates field IDs with their preset configuration as {@link JsonObject}
     * @param dropDownMap           a map that associates field IDs with dropdown values
     * @param analyticsRequest      the {@link AnalyticsRequest} containing request context
     * @param isPeriodicFilterField whether the field is being processed as a periodic filter field
     * @param i18nResolver          an instance of {@link I18nResolver} for resolving internationalization (i18n) details
     * @return a {@link JsonObject} representing the filter field metadata
     */
    private JsonObject setFilterFields(final ReportModularField reportModularField,
                                       final Map<Long, StoreType> fieldTypeMap,
                                       final Map<Long, JsonObject> fieldConfigMap,
                                       final Map<Long, Object> dropDownMap,
                                       final AnalyticsRequest analyticsRequest,
                                       final boolean isPeriodicFilterField,
                                       final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final ModularField modularField = reportModularField.getModularField();
        final Long crmFieldId = modularField.getCrmFieldId();
        Long anaFieldId = null;

        if (modularField instanceof CRMSystemModularField systemModularField) {
            anaFieldId = systemModularField.getAnaFieldId();
        }

        if (Objects.nonNull(crmFieldId)) {
            final StoreType storeType = fieldTypeMap.get(crmFieldId);

            if (Objects.nonNull(storeType)) {
                final Object dropdownValue = dropDownMap.get(crmFieldId);
                final Collection<OperatorType> fieldComparators = OperatorType.getSupportedOperators(storeType);
                final JsonObject configJson = fieldConfigMap.get(crmFieldId);

                if (Objects.nonNull(dropdownValue)) {
                    final JsonObject dropdownJson = Json.object();
                    final String key = TypeChecker.isNumber(dropdownValue) ? CRMReportKeys.ID : CommonKeys.NAME;

                    dropdownJson.put(key, dropdownValue);
                    jsonObject.put(FilterFieldKeys.DROPDOWN, dropdownJson);
                }

                jsonObject.put(FilterFieldKeys.COMPARATORS, getComparatorIds(fieldComparators, isPeriodicFilterField));

                if (Objects.nonNull(configJson) && NumericalConstant.ZERO < configJson.size()) {
                    jsonObject.put(FilterFieldKeys.CONFIG, configJson);
                }
                jsonObject.put(FilterFieldKeys.FIELD_TYPE, getFieldTypeName(modularField, storeType, i18nResolver));
            }
        }

        if (Objects.nonNull(anaFieldId)) {
            final StoreType storeType = fieldTypeMap.get(anaFieldId);
            final Collection<OperatorType> fieldComparators = OperatorType.getSupportedOperators(storeType);

            jsonObject.put(FilterFieldKeys.FIELD_TYPE, storeType);
            jsonObject.put(FilterFieldKeys.COMPARATORS, getComparatorIds(fieldComparators, isPeriodicFilterField));
        }

        jsonObject.put(ComparatorFields.NAME.getName(), analyticsRequest.crmResponse().resolveI18nPlaceholder(modularField.getFieldName()));
        jsonObject.put(CRMReportKeys.FIELD_ID, reportModularField.id());

        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final ModularFieldConfig modularFieldConfig = modularField.getModularFieldConfig();

        if (Objects.nonNull(modularFieldConfig) && modularFieldConfig.isFutureDateRestricted()) {
            final JsonObject maxDateValidation = Json.object();
            final JsonObject maxDateInfo = Json.object();
            final JsonObject config = Json.object();
            final Long maxDate = TemporalServiceProvider.get().getEndTimeOfDay(userRegionalPreference.getTimezoneInfo());

            maxDateInfo.put(FieldKeys.MESSAGE.getName(), i18nResolver.get("i18n.analytics.config.invalid.future.date"));
            maxDateInfo.put(FieldKeys.VALUE.getName(), maxDate);
            maxDateValidation.put(FilterFieldKeys.MAX_DATE, maxDateInfo);
            jsonObject.put(FieldKeys.VALIDATIONS.getName(), maxDateValidation);
            config.put(FilterFieldKeys.MAX_DATE, maxDate);
            jsonObject.put(FilterFieldKeys.CONFIG, config);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Gets the field type name of the {@link ModularField}
     * </p>
     *
     * @param modularField the {@link ModularField} whose type name is to be resolved
     * @param storeType    the {@link StoreType} associated with the field
     * @param i18nResolver the resolver used for internationalized strings
     * @return the field type name; returns {@code MULTI_DROPDOWN} for pipeline/lifecycle stage fields,
     *         otherwise the store type name
     */
    private String getFieldTypeName(final ModularField modularField, final StoreType storeType, final I18nResolver i18nResolver) {
        final String resolvedName = i18nResolver.get(modularField.getFieldName());

        return Objects.equals(PIPELINE_STAGE, resolvedName)
                || Objects.equals(LIFE_CYCLE_STAGE, resolvedName) ? MULTI_DROPDOWN
                : Objects.equals(StoreType.PRODUCT, storeType) ? StoreType.SINGLE_NUMBER.getName() : storeType.getName();
    }

    /**
     * <p>
     * Retrieves sorted comparator IDs, optionally filtered for periodic filters
     * </p>
     *
     * @param comparators           the comparator collection to extract IDs from
     * @param isPeriodicFilterField whether to filter for periodic-filter-enabled comparators only
     * @return a sorted collection of comparator IDs, or an empty list if comparators is null or empty
     */
    private Collection<Integer> getComparatorIds(final Collection<OperatorType> comparators, final boolean isPeriodicFilterField) {
        if (Objects.isNull(comparators) || comparators.isEmpty()) {
            return Collections.emptyList();
        }

        return comparators.stream()
                .filter(Objects::nonNull)
                .filter(comparator -> !isPeriodicFilterField || comparator.isPeriodicFilterEnabled())
                .map(OperatorType::getTypeCode)
                .sorted()
                .collect(Collectors.toList());
    }
}
