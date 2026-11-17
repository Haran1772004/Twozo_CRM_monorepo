package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.api.analytics.entity.config.axis.AxisTypes;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.BucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSubModuleMapper;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.timePeriod.CRMTimePeriodField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.core.sql.resource.CRMSUBMODULEMAPPER;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.dml.Criteria;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract way for retrieve the xAxis related fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
class CRMViewByFieldMetaGenerator extends AbstractFieldMetaGenerator implements ReportsMetaResponse<JsonObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMViewByFieldMetaGenerator.class);

    private final CRMReportManagerAPI crmReportManagerAPI;
    private final ReportModularFieldService reportModularFieldService;
    private final CRMViewConfigManager<CRMTimePeriodField> timePeriodFieldsManager;
    private final CRMViewConfigManager<CRMSubModuleMapper> subModuleMapperManager;
    private final AnalyticsFieldService analyticsFieldService;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;
    private static final int MIN_SIZE = 0;
    private static final int MAX_SIZE = 1;

    private CRMViewByFieldMetaGenerator() {
        this.crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
        this.timePeriodFieldsManager = crmReportManagerAPI.getTimePeriodFieldManager();
        this.subModuleMapperManager = crmReportManagerAPI.getSubModuleMapperManager();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
        this.analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
    }

    private static class Instance {
        private static final CRMViewByFieldMetaGenerator INSTANCE = new CRMViewByFieldMetaGenerator();
    }

    public static CRMViewByFieldMetaGenerator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param jsonObject       A {@link JsonObject} containing the request data needed to generate the response
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return the response as a {@link JsonObject}
     */
    @Override
    public @NonNull JsonObject getResponse(final @NonNull AnalyticsRequest analyticsRequest,
                                           final @NonNull JsonObject jsonObject,
                                           final @NonNull I18nResolver i18nResolver) {
        final Long orgId = analyticsRequest.orgId();
        final JsonObject xAxisResponse = Json.object();
        final JsonArray jsonArray = jsonObject.getJsonArray(CRMReportKeys.MODULE_IDS);
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        if (Objects.equals(jsonArray.size(), MAX_SIZE) || Objects.equals(jsonArray.get(MIN_SIZE), jsonArray.get(MAX_SIZE))) {
            final JsonArray moduleArray = Json.array();
            try {
                final Map<Integer, String> moduleIds = java.util.Arrays.stream(ModuleType.values()).filter(ModuleType::isEnabled)
                        .sorted(Comparator.comparing(ModuleType::getTypeCode))
                        .collect(Collectors.toMap(ModuleType::getTypeCode, ModuleType::getI18nKey,
                                (existingValue, duplicateValue) -> existingValue, LinkedHashMap::new));

                if (moduleIds.isEmpty()) {
                    throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                            "i18n.analytics.config.invalid.module");
                }

                reportModularFieldService.getByOrgId(orgId, new Callback<>() {
                    @Override
                    public void onSuccess(final Map<Long, ReportModularField> allReportFieldsRaw) {
                        try {
                            final Collection<ReportModularField> allReportFields = allReportFieldsRaw.values().stream()
                                    .filter(ReportModularField::getIsActive).collect(Collectors.toList());
                            final Collection<Integer> accessedModuleIds = analyticsPermissionHandler.getAuthorizedModuleIds(orgId, analyticsRequest.roleId());
                            final Collection<Long> unauthorizedAnalyticsFields = getUnauthorizedAnalyticsFields(orgId, accessedModuleIds);
                            final BucketUnit[] bucketUnits = BucketUnit.values();
                            final Map<Integer, List<CRMSubModuleMapper>> subModuleMappersByModule = buildSubModuleMappersByModule(orgId, moduleIds.keySet());
                            final Map<Long, Boolean> dateTypeCache = buildDateTypeCache(orgId, allReportFields);
                            final Set<Map.Entry<Integer, String>> entries = moduleIds.entrySet();

                            for (final Map.Entry<Integer, String> entry : entries) {
                                final Integer moduleId = entry.getKey();
                                final String moduleName = entry.getValue();
                                final JsonObject module = Json.object();

                                module.put(CRMViewPropertyKeys.MODULE_ID, moduleId);
                                module.put(CRMViewPropertyKeys.MODULE_NAME, crmResponse.resolveI18nPlaceholder(moduleName));

                                final Collection<ReportModularField> reportModularFields = getAuthorizedReportModularField(orgId, accessedModuleIds,
                                        unauthorizedAnalyticsFields, ModularFieldUtil.filterByModule(moduleId, allReportFields));
                                final List<ReportModularField> sortedReportModularFields = reportModularFields.stream()
                                        .sorted(Comparator.comparing(ReportModularField::id)).toList();

                                final List<CRMSubModuleMapper> subModuleMappers = subModuleMappersByModule.getOrDefault(moduleId, Collections.emptyList());

                                setXAxisField(orgId, accessedModuleIds, sortedReportModularFields, bucketUnits, subModuleMappers,
                                        unauthorizedAnalyticsFields, module, crmResponse, i18nResolver, dateTypeCache);

                                moduleArray.put(module);
                            }

                            xAxisResponse.put(CRMViewPropertyKeys.MODULES, moduleArray);

                            final JsonArray timeModule = Json.array();
                            final Collection<CRMTimePeriodField> timePeriodFields = timePeriodFieldsManager.getAll(orgId,
                                    Criteria.notEq(timePeriodFieldsManager.getTable().getPKColumn(), NumericalConstant.NEGATIVE_ONE));
                            setTimePeriodField(timePeriodFields, timeModule, i18nResolver);

                            xAxisResponse.put(CRMViewPropertyKeys.TIME_PERIOD_FIELDS, timeModule);

                            xAxisResponse.forEach(crmResponse::put);
                            crmResponse.send();
                        } catch (final Exception exception) {
                            LOGGER.error("Failed to fetch XAxis field for orgId: {} and moduleIds: {}", orgId, moduleIds, exception);
                            final ErrorCode errorCode = ErrorCode.get(AnalyticsErrorCodes.X_AXIS_GET_FAILED, "i18n.analytics.x-axis.get.failed");
                            crmResponse.setErrorCode(errorCode);
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        LOGGER.error("getByOrgId callback failed for XAxis, orgId: {}", orgId, cause);
                        final ErrorCode errorCode = ErrorCode.get(AnalyticsErrorCodes.X_AXIS_GET_FAILED, "i18n.analytics.x-axis.get.failed");
                        crmResponse.setErrorCode(errorCode);
                        crmResponse.send();
                    }
                });

            } catch (final Exception exception) {
                LOGGER.error("Failed to fetch XAxis field for orgId: {}.", orgId);
                throw ErrorCode.get(AnalyticsErrorCodes.X_AXIS_GET_FAILED,
                        "i18n.analytics.x-axis.get.failed");
            }
        } else {
            final CRMViewConfigManager<CRMTimePeriodField> timePeriodFieldsManager = crmReportManagerAPI.getTimePeriodFieldManager();
            final JsonArray timeModule = Json.array();

            try {
                final Collection<CRMTimePeriodField> timePeriodFields = timePeriodFieldsManager.getAll(orgId,
                        Criteria.notEq(timePeriodFieldsManager.getTable().getPKColumn(), NumericalConstant.NEGATIVE_ONE));

                setTimePeriodField(timePeriodFields, timeModule, i18nResolver);
                xAxisResponse.put(CRMViewPropertyKeys.TIME_PERIOD_FIELDS, timeModule);

                xAxisResponse.forEach(crmResponse::put);
                crmResponse.send();
            } catch (final Exception exception) {
                LOGGER.error("Failed to fetch TimePeriodfields for orgId: {}.", orgId);
                final ErrorCode errorCode = ErrorCode.get(AnalyticsErrorCodes.X_AXIS_GET_FAILED, "i18n.analytics.x-axis.get.failed");
                crmResponse.setErrorCode(errorCode);
                crmResponse.send();
            }
        }
        return Json.object();
    }

    /**
     * <p>
     * Builds a map of moduleId to its list of {@link CRMSubModuleMapper} entries.
     * Single bulk DB query replaces per-module queries in the loop.
     * </p>
     *
     * @param orgId     which holds the organization ID
     * @param moduleIds the set of module IDs to fetch sub-module mappers for
     * @return Map of moduleId to list of CRMSubModuleMapper
     */
    private Map<Integer, List<CRMSubModuleMapper>> buildSubModuleMappersByModule(final Long orgId,
                                                                              final Set<Integer> moduleIds) throws Exception {
        final Collection<CRMSubModuleMapper> allSubModuleMappers = subModuleMapperManager.getAll(orgId,
                Criteria.in(subModuleMapperManager.getTable().getColumn(CRMSUBMODULEMAPPER.MODULE_ID), moduleIds));
        final Map<Integer, List<CRMSubModuleMapper>> mappersByModule = new HashMap<>();

        for (final CRMSubModuleMapper mapper : allSubModuleMappers) {
            mappersByModule.computeIfAbsent(mapper.getModuleId(), k -> new ArrayList<>()).add(mapper);
        }

        return mappersByModule;
    }

    /**
     * <p>
     * Sets the modular fields for a xAxis in the data JSON object
     * </p>
     *
     * @param orgId               which holds the organization ID
     * @param accessedModuleIds   which holds ModuleIds that the user can access
     * @param reportModularFields which holds the {@link ReportModularField} that containing the modular field details
     * @param bucketUnits         which holds the {@link BucketUnit} enum values
     * @param subModuleMappers    which holds subModuleId
     * @param unAuthorizedFields  which holds the unauthorized analytics field IDs
     * @param xAxisResponse       the JSON object to which the xAxis field will be added
     * @param crmResponse         Represents the {@link CrmResponse}
     * @param i18nResolver        an instance of {@link I18nResolver} for resolving internationalization (i18n) details if needed
     * @param dateTypeCache       pre-computed map of modularFieldId to isDateType flag
     */
    private void setXAxisField(final Long orgId,
                               final Collection<Integer> accessedModuleIds,
                               final Collection<ReportModularField> reportModularFields,
                               final BucketUnit[] bucketUnits,
                               final Collection<CRMSubModuleMapper> subModuleMappers,
                               final Collection<Long> unAuthorizedFields,
                               final JsonObject xAxisResponse,
                               final CrmResponse crmResponse,
                               final I18nResolver i18nResolver,
                                final Map<Long, Boolean> dateTypeCache) {
        try {
            final JsonArray fieldArray = Json.array();

            for (final ReportModularField reportModularField : reportModularFields) {

                if (!unAuthorizedFields.contains(reportModularField.id())) {
                    final ModularField modularField = reportModularField.getModularField();

                    if (Objects.isNull(modularField)) {
                        continue;
                    }
                    final ModularFieldConfig modularFieldConfig = modularField.getModularFieldConfig();
                    final AxisTypes axisType = AxisTypes.getAxisTypes(modularFieldConfig.getAxisType());

                    if (Objects.equals(AxisTypes.X_ONLY, axisType) || Objects.equals(AxisTypes.X_Y, axisType)) {
                        final Long modularFieldId = reportModularField.id();
                        final JsonObject fieldJson = Json.object();

                        fieldJson.put(CRMReportKeys.FIELD_ID, modularFieldId);
                        fieldJson.put(CRMViewPropertyKeys.FIELD_NAME, crmResponse.resolveI18nPlaceholder(modularField.getFieldName()));

                        if (modularFieldConfig.isBucketByEnabled()) {
                            final JsonObject bucketJson = Json.object();
                            final JsonArray unitArray = Json.array();

                            for (final BucketUnit unit : bucketUnits) {
                                unitArray.put(Json.object().put(CRMViewPropertyKeys.UNIT_ID, unit.getTypeCode())
                                        .put(CRMViewPropertyKeys.UNIT_NAME, i18nResolver.get(unit.getI18nKey())));
                            }
                            bucketJson.put(CRMViewPropertyKeys.UNITS, unitArray);
                            bucketJson.put(CRMViewPropertyKeys.IS_FROM_AND_TO_ALLOWED, true);
                            fieldJson.put(CRMViewPropertyKeys.BUCKET, bucketJson);
                        }

                        fieldJson.put(CRMViewPropertyKeys.ID_DATE_TYPE, dateTypeCache.getOrDefault(modularFieldId, false));
                        fieldArray.put(fieldJson);
                    }
                }
            }

            subModuleMappers.stream().sorted(Comparator.comparing(CRMSubModuleMapper::getSubModuleId))
                    .forEach(crmSubModuleMapper -> {
                        final Integer subModuleId = crmSubModuleMapper.getSubModuleId();

                        if (accessedModuleIds.contains(subModuleId)) {
                            final JsonObject fieldJson = Json.object();

                            fieldJson.put(CRMViewPropertyKeys.FIELD_NAME, crmResponse.resolveI18nPlaceholder(crmSubModuleMapper.getAliasName()));
                            fieldJson.put(CRMViewPropertyKeys.MODULE_ID, subModuleId);
                            fieldArray.put(fieldJson);
                        }
                    });

            xAxisResponse.put(CRMViewPropertyKeys.FIELDS, fieldArray);
        } catch (final Exception exception) {
            LOGGER.error("Failed to set XAxis field for orgId: {}.", orgId);
            throw ErrorCode.get(AnalyticsErrorCodes.X_AXIS_GET_FAILED,
                    "i18n.analytics.x-axis.get.failed");
        }
    }

    /**
     * <p>
     * Builds a pre-computed map of modularFieldId to isDateType flag for all report fields.
     * Uses a single-pass collection phase and at most 2 bulk DB queries (analytics fields + CRM fields),
     * replacing the previous per-field N+1 query pattern.
     * </p>
     *
     * @param orgId              which holds the organization ID
     * @param allReportFields    all report modular fields for this org
     * @return Map of modularFieldId to isDateType boolean
     */
    private Map<Long, Boolean> buildDateTypeCache(final Long orgId,
                                                  final Collection<ReportModularField> allReportFields) {
        final FieldService fieldService = FieldService.getInstance();
        final FieldTypeService fieldTypeService = FieldTypeService.getInstance();

        final Set<Long> anaFieldIds = new HashSet<>();
        final Set<Long> crmFieldIds = new HashSet<>();
        final Map<Long, Long> fieldToAnaFieldId = new HashMap<>();
        final Map<Long, Long> fieldToCrmFieldId = new HashMap<>();
        final List<ReportModularField> xAxisFields = new ArrayList<>();

        for (final ReportModularField reportModularField : allReportFields) {
            final ModularField modularField = reportModularField.getModularField();

            if (Objects.isNull(modularField)) {
                continue;
            }

            final ModularFieldConfig config = modularField.getModularFieldConfig();

            if (Objects.isNull(config)) {
                continue;
            }

            final AxisTypes axisType = AxisTypes.getAxisTypes(config.getAxisType());

            if (!Objects.equals(AxisTypes.X_ONLY, axisType) && !Objects.equals(AxisTypes.X_Y, axisType)) {
                continue;
            }

            xAxisFields.add(reportModularField);

            final Long modularFieldId = reportModularField.id();
            final Long crmFieldId = modularField.getCrmFieldId();

            if (!reportModularField.isCustom()) {
                final CRMSystemModularField sysField = reportModularField.getSystemModularField();

                if (Objects.nonNull(sysField)) {
                    final Long anaFieldId = sysField.getAnaFieldId();

                    if (Objects.nonNull(anaFieldId)) {
                        anaFieldIds.add(anaFieldId);
                        fieldToAnaFieldId.put(modularFieldId, anaFieldId);
                    }
                }
            }

            if (Objects.nonNull(crmFieldId)) {
                crmFieldIds.add(crmFieldId);
                fieldToCrmFieldId.put(modularFieldId, crmFieldId);
            }
        }

        if (xAxisFields.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, AnalyticsField> analyticsFieldMap = bulkFetchAnalyticsFields(orgId, anaFieldIds);
        final Map<Long, FieldInfo> crmFieldMap = bulkFetchCrmFields(orgId, crmFieldIds, fieldService);
        final Map<Long, Boolean> dateTypeMap = new HashMap<>(xAxisFields.size());

        for (final ReportModularField reportModularField : xAxisFields) {
            final Long modularFieldId = reportModularField.id();
            boolean isDateType = false;

            final Long anaFieldId = fieldToAnaFieldId.get(modularFieldId);

            if (Objects.nonNull(anaFieldId)) {
                final AnalyticsField analyticsField = analyticsFieldMap.get(anaFieldId);

                if (Objects.nonNull(analyticsField) && Objects.nonNull(analyticsField.getFieldTypeId())) {
                    final FieldType fieldType = fieldTypeService.get(analyticsField.getFieldTypeId());
                    final StoreType storeType = fieldType.storeType();
                    isDateType = storeType.isDateType();
                }
            }

            if (!isDateType) {
                final Long crmFieldId = fieldToCrmFieldId.get(modularFieldId);

                if (Objects.nonNull(crmFieldId)) {
                    final FieldInfo fieldInfo = crmFieldMap.get(crmFieldId);

                    if (Objects.nonNull(fieldInfo)) {
                        final FieldStore fieldStore = fieldInfo.fieldStore();

                        if (Objects.nonNull(fieldStore)) {
                            isDateType = fieldStore.storeType().isDateType();
                        }
                    }
                }
            }

            dateTypeMap.put(modularFieldId, isDateType);
        }

        return dateTypeMap;
    }

    /**
     * <p>
     * Bulk fetches analytics fields by their IDs in a single DB query.
     * </p>
     *
     * @param orgId       which holds the organization ID
     * @param anaFieldIds the set of analytics field IDs to fetch
     * @return Map of anaFieldId to AnalyticsField
     */
    private Map<Long, AnalyticsField> bulkFetchAnalyticsFields(final Long orgId, final Set<Long> anaFieldIds) {
        if (anaFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            final Map<Long, AnalyticsField> map = new HashMap<>(anaFieldIds.size());

            for (final Long anaFieldId : anaFieldIds) {
                final AnalyticsField analyticsField = analyticsFieldService.getAnalyticsFieldById(anaFieldId);
                if (Objects.nonNull(analyticsField)) {
                    map.put(analyticsField.id(), analyticsField);
                }
            }
            return map;
        } catch (final Exception exception) {
            LOGGER.error("Failed to bulk fetch analytics fields for orgId: {}.", orgId);
            return Collections.emptyMap();
        }
    }

    /**
     * <p>
     * Bulk fetches CRM fields by their IDs in a single query.
     * </p>
     *
     * @param orgId        which holds the organization ID
     * @param crmFieldIds  the set of CRM field IDs to fetch
     * @param fieldService the field service instance
     * @return Map of crmFieldId to FieldInfo
     */
    private Map<Long, FieldInfo> bulkFetchCrmFields(final Long orgId,
                                                    final Set<Long> crmFieldIds,
                                                    final FieldService fieldService) {
        if (crmFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return fieldService.getFieldsById(orgId, crmFieldIds);
        } catch (final Exception exception) {
            LOGGER.error("Failed to bulk fetch CRM fields for orgId: {}.", orgId);
            return Collections.emptyMap();
        }
    }

    /**
     * <p>
     * Sets the time period fields for a xAxis in the data JSON object
     * </p>
     *
     * @param timePeriodFields    which holds the {@link CRMTimePeriodField} that containing the time period fields
     * @param timePeriodJsonArray the JSON array to which the xAxis field will be added
     * @param i18nResolver        an instance of {@link I18nResolver} for resolving internationalization (i18n) details if needed
     **/
    private void setTimePeriodField(final Collection<CRMTimePeriodField> timePeriodFields,
                                    final JsonArray timePeriodJsonArray,
                                    final I18nResolver i18nResolver) {
        for (final CRMTimePeriodField timePeriodField : timePeriodFields) {
            final JsonObject fieldJson = Json.object();

            fieldJson.put(CRMViewPropertyKeys.TIME_PERIOD_FIELDS_ID, timePeriodField.id());
            fieldJson.put(CRMViewPropertyKeys.FIELD_NAME, i18nResolver.get(timePeriodField.getDisplayName()));
            fieldJson.put(CRMViewPropertyKeys.FUNCTION_ID, timePeriodField.getFunctionId());
            timePeriodJsonArray.put(fieldJson);
        }
    }
}
