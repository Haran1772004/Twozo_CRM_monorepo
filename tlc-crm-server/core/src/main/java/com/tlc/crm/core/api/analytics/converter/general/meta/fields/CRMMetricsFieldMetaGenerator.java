package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.OperationType;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract way for retrieve the metrics related fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
class CRMMetricsFieldMetaGenerator extends AbstractFieldMetaGenerator implements ReportsMetaResponse<JsonObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMMetricsFieldMetaGenerator.class);

    private final ReportModularFieldService reportModularFieldService;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;

    /**
     * <p>
     * Private constructor that initializes all required service instances
     * </p>
     */
    private CRMMetricsFieldMetaGenerator() {
        this.reportModularFieldService = ReportModularFieldService.getInstance();
        this.analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
    }

    private static class Instance {
        private static final CRMMetricsFieldMetaGenerator INSTANCE = new CRMMetricsFieldMetaGenerator();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link CRMMetricsFieldMetaGenerator}
     * </p>
     *
     * @return the singleton {@link CRMMetricsFieldMetaGenerator} instance
     */
    public static CRMMetricsFieldMetaGenerator INSTANCE() {
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
        final JsonObject metricsResponse = Json.object();
        final int viewTypeId = jsonObject.getInt(CRMReportKeys.VIEW_TYPE_ID);
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        try {
            reportModularFieldService.getByOrgId(orgId, new Callback<>() {
                @Override
                public void onSuccess(final Map<Long, ReportModularField> allReportFieldsRaw) {
                    try {
                        final Collection<ReportModularField> allReportFields = allReportFieldsRaw.values().stream()
                                .filter(ReportModularField::getIsActive).collect(Collectors.toList());

                        final Collection<Integer> accessedModuleIds = analyticsPermissionHandler.getAuthorizedModuleIds(orgId, analyticsRequest.roleId());
                        final Collection<Long> unauthorizedAnalyticsFields = getUnauthorizedAnalyticsFields(orgId, accessedModuleIds);

                        final Collection<ReportModularField> authorizedReportFields = getAuthorizedReportModularField(orgId, accessedModuleIds,
                                unauthorizedAnalyticsFields, allReportFields);

                        final Map<Long, Collection<ReportModularField>> measureToModularFieldMap = new HashMap<>();

                        for (final ReportModularField reportField : authorizedReportFields) {
                            final ModularField modularField = reportField.getModularField();

                            if (Objects.isNull(modularField)) {
                                continue;
                            }

                            final ModularFieldConfig config = modularField.getModularFieldConfig();

                            if (Objects.isNull(config) || Objects.isNull(config.getMeasureIds()) || config.getMeasureIds().isEmpty()) {
                                continue;
                            }

                            for (final Integer measureId : config.getMeasureIds()) {
                                measureToModularFieldMap.computeIfAbsent(Long.valueOf(measureId), key -> new ArrayList<>()).add(reportField);
                            }
                        }

                        setMeasureField(measureToModularFieldMap, metricsResponse, crmResponse, i18nResolver);

                        metricsResponse.forEach(crmResponse::put);
                        crmResponse.send();
                    } catch (final Exception exception) {
                        LOGGER.error("Failed to fetch metrics fields for orgId: {} and viewTypeId: {}", orgId, viewTypeId, exception);
                        crmResponse.setErrorCode( ErrorCode.get(AnalyticsErrorCodes.METRICS_GET_FAILED, "i18n.analytics.metrics.get.failed"));
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("getByOrgId callback failed for metrics, orgId: {} and viewTypeId: {}", orgId, viewTypeId, cause);
                    crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.METRICS_GET_FAILED, "i18n.analytics.metrics.get.failed"));
                    crmResponse.send();
                }
            });
        } catch (final Exception exception) {
            LOGGER.error("Failed to initiate metrics field fetch for orgId: {} and viewTypeId: {}", orgId, viewTypeId, exception);
            crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.METRICS_GET_FAILED, "i18n.analytics.metrics.get.failed"));
            crmResponse.send();
        }

        return Json.object();
    }

    /**
     * <p>
     * Sets the measure field in the metrics response based on the provided field operations and modular fields
     * </p>
     *
     * @param measureToModularFieldMap which holds the {@link ModularField} that containing the modular field details
     * @param metricsResponse          the JSON object to which the measure field will be added
     * @param crmResponse              Represents the {@link CrmResponse}
     * @param i18nResolver             an instance of {@link I18nResolver} for resolving internationalization (i18n) details if needed
     */
    private void setMeasureField(final Map<Long, Collection<ReportModularField>> measureToModularFieldMap,
                                 final JsonObject metricsResponse,
                                 final CrmResponse crmResponse,
                                 final I18nResolver i18nResolver) {
        final JsonArray jsonArray = Json.array();

        measureToModularFieldMap.forEach((measureId, reportModularFields) ->
        {
            final JsonObject measureJson = Json.object();

            measureJson.put(CRMReportKeys.MEASURE_ID, measureId);
            measureJson.put(CRMViewPropertyKeys.MEASURE_NAME, i18nResolver.get(OperationType.getOperationType(Math.toIntExact((measureId)))
                    .getI18nKeyOfDisplayName()));
            setMeasureModularField(reportModularFields, measureJson, crmResponse);
            jsonArray.put(measureJson);
        });

        metricsResponse.put(CRMViewPropertyKeys.METRICS, jsonArray);
    }

    /**
     * <p>
     * Sets the modular fields for a measure in the data JSON object
     * </p>
     *
     * @param reportModularFields which holds the {@link ReportModularField} that containing the modular field details
     * @param metricsResponse     the JSON object to which the measure field will be added
     * @param crmResponse         Represents the {@link CrmResponse}
     */
    private void setMeasureModularField(final Collection<ReportModularField> reportModularFields,
                                        final JsonObject metricsResponse,
                                        final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final Map<Integer, Map<Long, ModularField>> moduleReportFieldMap = new HashMap<>();

        for (final ReportModularField reportField : reportModularFields) {
            final ModularField modularField = reportField.getModularField();

            if (Objects.nonNull(modularField)) {
                moduleReportFieldMap.computeIfAbsent(modularField.getModuleId(), k -> new HashMap<>())
                        .put(reportField.id(), modularField);
            }
        }

        moduleReportFieldMap.forEach((moduleId, modularFieldMap) ->
        {
            final JsonObject moduleJson = Json.object();
            final JsonArray fieldArray = Json.array();

            for (final Map.Entry<Long, ModularField> entry : modularFieldMap.entrySet()) {
                fieldArray.put(Json.object().put(CRMReportKeys.FIELD_ID, entry.getKey())
                        .put(CRMViewPropertyKeys.FIELD_NAME, crmResponse.resolveI18nPlaceholder(entry.getValue().getFieldName())));
            }

            moduleJson.put(CRMReportKeys.MODULE_ID, moduleId);
            moduleJson.put(CRMViewPropertyKeys.MODULE_NAME, crmResponse.resolveI18nPlaceholder(ModuleType.getModuleType(moduleId).getI18nKey()));
            moduleJson.put(CRMViewPropertyKeys.FIELDS, fieldArray);

            jsonArray.put(moduleJson);
        });

        metricsResponse.put(CRMViewPropertyKeys.MODULES, jsonArray);
    }
}
