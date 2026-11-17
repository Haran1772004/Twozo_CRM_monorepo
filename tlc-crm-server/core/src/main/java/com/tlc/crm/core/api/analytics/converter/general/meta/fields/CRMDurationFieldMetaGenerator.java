package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorType;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates duration-related field metadata for KPI widgets.
 *
 * <p>Fetches report modular fields via async callback and builds a response
 * containing duration-allowed fields, time units, and comparator types.</p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
class CRMDurationFieldMetaGenerator implements ReportsMetaResponse<JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMDurationFieldMetaGenerator.class);

    private final ReportModularFieldService reportModularFieldService;

    /**
     * <p>
     * Private constructor that initializes all required service instances
     * </p>
     */
    private CRMDurationFieldMetaGenerator() {
        reportModularFieldService = ReportModularFieldService.getInstance();
    }

    private static class Instance {
        private static final CRMDurationFieldMetaGenerator INSTANCE = new CRMDurationFieldMetaGenerator();
    }

    /**
     * Returns the singleton instance.
     *
     * @return {@link CRMDurationFieldMetaGenerator} singleton
     */
    public static CRMDurationFieldMetaGenerator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Fetches report modular fields via callback, filters duration-allowed fields,
     * builds the response with fields, time units, and comparator types,
     * and sends it via {@code crmResponse}.
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
        final int moduleId = jsonObject.getInt(CRMReportKeys.MODULE_ID);
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {

            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                try {
                    final Collection<ReportModularField> activeFields = reportFieldMap.values()
                            .stream()
                            .filter(ReportModularField::getIsActive)
                            .collect(Collectors.toList());

                    final List<ReportModularField> moduleFields = ModularFieldUtil.filterByModule(moduleId, activeFields);

                    if (moduleFields.isEmpty()) {
                        throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                                "i18n.analytics.config.invalid.module");
                    }

                    final JsonObject response = buildDurationResponse(moduleFields, crmResponse, i18nResolver);

                    response.forEach(crmResponse::put);
                    crmResponse.send();
                } catch (final Exception exception) {
                    LOGGER.error("Failed to process duration fields for orgId: {}, moduleId: {}", orgId, moduleId, exception);
                    crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.DURATION_GET_FAILED, "i18n.analytics.duration.get.failed"));
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("Failed to fetch report fields for duration meta, orgId: {}", orgId, cause);
                crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.DURATION_GET_FAILED, "i18n.analytics.duration.get.failed"));
                crmResponse.send();
            }
        });

        return Json.object();
    }

    /**
     * Builds the duration response containing fields, time units, and comparator types.
     *
     * @param reportModularFields the filtered report modular fields for the module
     * @param crmResponse         the CRM response (used for i18n placeholder resolution)
     * @param i18nResolver        resolver for internationalized display names
     * @return the built {@link JsonObject} with duration metadata
     */
    private JsonObject buildDurationResponse(final Collection<ReportModularField> reportModularFields,
                                             final CrmResponse crmResponse,
                                             final I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonArray fieldsArray = Json.array();
        final JsonArray timeUnitArray = Json.array();
        final JsonArray compareArray = Json.array();

        // Duration-allowed fields
        for (final ReportModularField reportModularField : reportModularFields) {
            final ModularField modularField = reportModularField.getModularField();

            if (modularField != null && modularField.getModularFieldConfig().isDurationAllowed()) {
                fieldsArray.put(Json.object()
                        .put(CRMReportKeys.FIELD_ID, reportModularField.id())
                        .put(CRMViewPropertyKeys.FIELD_NAME, crmResponse.resolveI18nPlaceholder(modularField.getFieldName())));
            }
        }
        final Collection<OperationType> kpiTimeUnits = OperationType.getKPITimeUnits();

        // KPI time units
        for (final OperationType kpiTimeUnit : kpiTimeUnits) {
            final JsonObject timeUnit = Json.object()
                    .put(CRMKPIKeys.TIME_UNIT_ID, kpiTimeUnit.getTypeCode())
                    .put(CRMKPIKeys.TIME_UNIT_NAME, i18nResolver.get(kpiTimeUnit.getI18nKeyOfDisplayName()));

            if (Objects.equals(CRMKPIKeys.CUSTOM, kpiTimeUnit.name().toLowerCase())) {
                timeUnit.put(CommonKeys.IS_CUSTOM, true);
            }

            timeUnitArray.put(timeUnit);
        }
        final KPIComparatorType[] values = KPIComparatorType.values();

        // Comparator types
        for (final KPIComparatorType comparatorType : values) {
            final JsonObject compare = Json.object()
                    .put(CRMKPIKeys.COMPARE_UNIT_ID, comparatorType.getTypeCode())
                    .put(CRMKPIKeys.COMPARE_UNIT_NAME, i18nResolver.get(comparatorType.getI18key()));

            if (Objects.equals(CRMKPIKeys.CUSTOM, comparatorType.name().toLowerCase())) {
                compare.put(CommonKeys.IS_CUSTOM, true);
            }

            compareArray.put(compare);
        }

        response.put(CRMViewPropertyKeys.FIELDS, fieldsArray);
        response.put(CRMViewPropertyKeys.TIME_UNITS, timeUnitArray);
        response.put(CRMKPIKeys.COMPARE_TO, compareArray);

        return response;
    }
}
