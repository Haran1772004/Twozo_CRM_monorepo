package com.tlc.crm.core.api.analytics.converter.template.templatedetails;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.config.keys.view.report.ReportFieldKeys;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.listeners.Callback;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.template.CRMReportTemplate;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMTemplateManagerAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMTemplateDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateDuration;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateCompareTo;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateMetrics;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateViewBy;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplatePeriodicFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplatePeriodicFilterValues;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateFilters;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateFilterValues;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateViewByBucket;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEDETAILS;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.dml.Criteria;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages Template related response
 * </p>
 *
 * @author Ryan
 * @version 1.0
 * @see CRMTemplateAPI
 */

public class CRMTemplateAPI implements CRMReportTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMTemplateAPI.class);

    private final ReportModularFieldService reportModularFieldService;
    private final CRMTemplateManagerAPI crmTemplateManagerAPI;
    private final CRMViewConfigManager<CRMTemplateDetails> crmTemplateManager;
    private final CRMReportManagerAPI crmReportManagerAPI;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;
    private final IconService iconService;

    /**
     * Creates template API dependencies.
     */
    private CRMTemplateAPI() {
        this.crmTemplateManagerAPI = CRMTemplateManagerAPI.INSTANCE();
        this.crmTemplateManager = crmTemplateManagerAPI.getCRMTemplatedManager();
        this.crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        this.analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
        this.iconService = IconService.getInstance();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
    }

    private static class Instance {
        private static final CRMTemplateAPI INSTANCE = new CRMTemplateAPI();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link CRMTemplateAPI} singleton
     */
    public static CRMTemplateAPI INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Processes a request to retrieve a list of available report templates, grouped by module.
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param request          Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     */
    @Override
    public void processTemplateListRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                           final @NonNull JsonObject request,
                                           final @NonNull I18nResolver i18nResolver,
                                           final @NonNull ActionType actionType) {
        final Long orgId = analyticsRequest.orgId();
        final JsonObject response = Json.object();
        final JsonArray jsonArray = Json.array();
        final ModuleType[] values = ModuleType.values();

        try {
            for (final ModuleType moduleType : values) {

                if (moduleType.isTemplateEnabled()) {
                    final int moduleId = moduleType.getTypeCode();
                    final JsonObject moduleJson = Json.object();
                    final JsonArray templateArray = Json.array();
                    final Collection<CRMTemplateDetails> crmTemplateDetails = crmTemplateManager.getAll(orgId,
                            Criteria.eq(crmTemplateManager.getTable().getColumn(CRMTEMPLATEDETAILS.MODULE_ID), moduleId));

                    moduleJson.put(CRMReportKeys.MODULE_ID, moduleId);
                    moduleJson.put(CRMViewPropertyKeys.MODULE_NAME, analyticsRequest.crmResponse().resolveI18nPlaceholder(moduleType.getI18nKey()));
                    StoreType.getTypeBySource(moduleType.getName()).filter(StoreType.CONTACT::equals)
                            .ifPresent(type -> moduleJson.put(OptionKeys.IS_DEFAULT, Boolean.TRUE));
                    moduleJson.put(CRMViewPropertyKeys.IS_PREVIEW, isViewAccessible(analyticsRequest, moduleId));

                    for (final CRMTemplateDetails crmTemplate : crmTemplateDetails) {
                        final JsonObject templateJson = Json.object();
                        final Icon icon = iconService.fetchIconByTypeAndName(Icon.Type.CHART, CRMViewType.getCrmViewType(crmTemplate.getViewTypeId()).getAliasName());
                        final JsonObject iconObject = Json.object();

                        iconObject.put(CommonKeys.DISPLAY_NAME, i18nResolver.get(icon.getI18nName()));
                        iconObject.put(CommonKeys.NAME, icon.getName());
                        iconObject.put(CRMReportKeys.ID, icon.id());

                        templateJson.put(CRMReportKeys.ID, crmTemplate.id());
                        templateJson.put(CRMReportKeys.NAME, crmTemplate.getViewName());
                        templateJson.put(CommonKeys.ICON, iconObject);
                        templateArray.put(templateJson);
                    }

                    moduleJson.put(CRMReportKeys.TEMPLATES, templateArray);
                    jsonArray.put(moduleJson);
                }
            }
            response.put(FilterFieldKeys.VALUES, jsonArray);
        } catch (Exception error) {
            LOGGER.error("CRM Analytics: Template list get action failed for orgId: {}. Reason: {} ", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.TEMPLATE_GET_FAILED);
        }

        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        response.forEach(crmResponse::put);
        crmResponse.send();
    }

    /**
     * <p>
     * Processes a request to retrieve the detailed configuration data for a specific
     * report template.
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param request          Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     */
    @Override
    public void processTemplateDataRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                           final @NonNull JsonObject request,
                                           final @NonNull I18nResolver i18nResolver,
                                           final @NonNull ActionType actionType) {
        final Long orgId = analyticsRequest.orgId();
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Map<Long, ReportModularField> allReportFields) {
                try {
                    final Map<Long, Long> reportFieldMap = allReportFields.values().stream()
                            .filter(field -> !field.isCustom() && Objects.nonNull(field.getSystemFieldId()))
                            .collect(Collectors.toMap(ReportModularField::getSystemFieldId, ReportModularField::id));
                    final JsonObject templateResponse = buildTemplateResponse(request, reportFieldMap, analyticsRequest);

                    templateResponse.forEach(crmResponse::put);
                    crmResponse.send();
                } catch (final Exception exception) {
                    LOGGER.error("CRM Analytics : template data build failed, error: {}", exception.getMessage());
                    crmResponse.setErrorCode(ErrorCodeUtil.getOrDefaultErrorCode(exception));
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCodeUtil.getOrDefaultErrorCode(exception))));
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics : getByOrgId callback failed, error: {}", cause.getMessage());
                final ErrorCode errorCode = ErrorCode.get(AnalyticsErrorCodes.TEMPLATE_GET_FAILED);
                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
                crmResponse.send();
            }
        });
    }

    /**
     * <p>
     * Builds the template response JsonObject using the pre-fetched report field map.
     * </p>
     */
    private JsonObject buildTemplateResponse(final JsonObject request, final Map<Long, Long> reportFieldMap,
                                             final AnalyticsRequest analyticsRequest) {
        final JsonObject response = Json.object();
        final JsonObject viewProperties = Json.object();
        final JsonArray metricsArray = Json.array();
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        final CRMTemplateDetails crmTemplateDetails = crmTemplateManager.get(analyticsRequest.orgId(), request.getLong(CRMReportKeys.ID));

        response.put(CRMChildViewKeys.VIEW_NAME, crmTemplateDetails.getViewName());
        response.put(CRMReportKeys.VIEW_TYPE_ID, crmTemplateDetails.getViewTypeId());

        getMetricsJson(crmTemplateDetails.getTemplateMetrics(), reportFieldMap, metricsArray, crmResponse);

        if (CRMViewType.getCrmViewType(crmTemplateDetails.getViewTypeId()).equals(CRMViewType.KPI)) {
            final JsonObject durationJson = Json.object();
            final JsonObject compareToJson = Json.object();

            getCompareToJson(crmTemplateDetails.getTemplateCompareTo(), compareToJson);
            getDurationJson(crmTemplateDetails.getTemplateDuration(), reportFieldMap, durationJson, crmResponse);

            viewProperties.put(CRMKPIKeys.DURATION, durationJson);
            viewProperties.put(CRMKPIKeys.COMPARE_TO, compareToJson);
        } else {
            final JsonArray viewByArray = Json.array();

            getViewByArray(crmTemplateDetails.getTemplateViewBy(), reportFieldMap, viewByArray, crmResponse);

            viewProperties.put(CRMViewPropertyKeys.VIEW_BY, viewByArray);
        }

        viewProperties.put(CRMViewPropertyKeys.METRICS, metricsArray);
        response.put(CRMChildViewKeys.VIEW_PROPERTIES, viewProperties);
        return response;
    }

    /**
     * Checks whether a use has access to the module
     *
     * @param analyticsRequest the current analytics request containing organization and role identifiers
     * @param moduleId         the ID of the module (view) to check
     * @return {@code true}     if the user has access to the specified module, otherwise {@code false}
     * @throws ErrorCode if an error occurs while retrieving permissions
     */
    private boolean isViewAccessible(final AnalyticsRequest analyticsRequest, final Integer moduleId) {
        try {
            final Collection<Integer> accessModuleIds = analyticsPermissionHandler.getAuthorizedModuleIds(analyticsRequest.orgId(), analyticsRequest.roleId());

            if (accessModuleIds.contains(moduleId)) {
                return true;
            }
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics: Error in Setting isViewAccessible", exception);
            throw ErrorCode.get(AnalyticsErrorCodes.MODULE_ID_NOT_FOUND);
        }

        return false;
    }

    /**
     * Loads metricsArray with metric data
     *
     * @param crmTemplateMetrics Represents the {@link CRMTemplateMetrics} details
     * @param reportFieldMap     Represents the {@link Map} that contains reportModularFields
     * @param metricsArray       Represents the {@link JsonArray}
     */
    private void getMetricsJson(final Collection<CRMTemplateMetrics> crmTemplateMetrics, final Map<Long, Long> reportFieldMap,
                                final JsonArray metricsArray, final CrmResponse crmResponse) {
        for (final CRMTemplateMetrics crmTemplateMetric : crmTemplateMetrics) {
            final JsonObject metricJson = Json.object();

            metricJson.put(CRMReportKeys.MEASURE_ID, crmTemplateMetric.getMeasureId());
            metricJson.put(CRMReportKeys.MODULE_ID, crmTemplateMetric.getModuleId());
            metricJson.put(CRMReportKeys.FIELD_ID, reportFieldMap.get(crmTemplateMetric.getFieldId()));
            metricJson.put(ReportFieldKeys.LABEL, crmResponse.resolveI18nPlaceholder(crmTemplateMetric.getLabel()));
            metricJson.put(CRMViewPropertyKeys.ORDER, crmTemplateMetric.getOrder());

            final JsonArray filterArray = Json.array();
            final JsonObject periodicFilter = Json.object();

            getPeriodicFilterJson(crmTemplateMetric.getTemplatePeriodicFilter(), reportFieldMap, periodicFilter);
            getFiltersArray(crmTemplateMetric.getTemplateFilters(), reportFieldMap, filterArray);

            metricJson.put(CRMReportKeys.PERIODIC_FILTERS, periodicFilter);
            metricJson.put(FilterFieldKeys.FILTERS, filterArray);
            metricsArray.put(metricJson);
        }
    }

    /**
     * Loads periodic filter JsonObject with data
     *
     * @param crmTemplatePeriodicFilter Represents the {@link CRMTemplatePeriodicFilter} details
     * @param reportFieldMap            Represents the {@link Map} that contains reportModularFields
     * @param periodicFilter            Represents the {@link JsonObject}
     */
    private void getPeriodicFilterJson(final CRMTemplatePeriodicFilter crmTemplatePeriodicFilter, final Map<Long, Long> reportFieldMap,
                                       final JsonObject periodicFilter) {
        if (Objects.nonNull(crmTemplatePeriodicFilter)) {
            periodicFilter.put(CRMReportKeys.FIELD_ID, reportFieldMap.get(crmTemplatePeriodicFilter.getFieldId()));
            periodicFilter.put(CRMReportKeys.COMPARATOR_ID, crmTemplatePeriodicFilter.getComparatorId());
            periodicFilter.put(CRMReportKeys.FUNCTION_ID, crmTemplatePeriodicFilter.getFunctionId());

            final CRMTemplatePeriodicFilterValues crmTemplatePeriodicFilterValues = crmTemplatePeriodicFilter.getTemplatePeriodicFilterValues();
            final JsonArray values = Json.array();

            if (Objects.nonNull(crmTemplatePeriodicFilterValues)) {
                final JsonObject periodicFilterValues = Json.object();
                final Long periodicFrom = crmTemplatePeriodicFilterValues.getFrom();
                final Long periodicTo = crmTemplatePeriodicFilterValues.getTo();
                final Long periodicValue = crmTemplatePeriodicFilterValues.getTimeUnitValue();

                if (Objects.nonNull(periodicFrom) && Objects.nonNull(periodicTo)) {
                    periodicFilterValues.put(CommonKeys.FROM, periodicFrom);
                    periodicFilterValues.put(CommonKeys.TO, periodicTo);
                }

                if (Objects.nonNull(periodicValue)) {
                    periodicFilterValues.put(CRMReportKeys.VALUE, periodicValue);
                }

                values.put(periodicFilterValues);
            }

            periodicFilter.put(CRMReportKeys.VALUES, values);
        }
    }

    /**
     * Loads filterArray with filter data
     *
     * @param crmTemplateFilters Represents the {@link CRMTemplateFilters} details
     * @param reportFieldMap     Represents the {@link Map} that contains reportModularFields
     * @param filterArray        Represents the {@link JsonArray}
     */
    private void getFiltersArray(final Collection<CRMTemplateFilters> crmTemplateFilters, final Map<Long, Long> reportFieldMap,
                                 final JsonArray filterArray) {
        if (!crmTemplateFilters.isEmpty()) {

            for (final CRMTemplateFilters filter : crmTemplateFilters) {
                final JsonObject filterJson = Json.object();
                final int filterOrder = filter.getOrder();

                filterJson.put(CRMReportKeys.FIELD_ID, reportFieldMap.get(filter.getFieldId()));
                filterJson.put(CRMReportKeys.COMPARATOR_ID, filter.getComparatorId());
                filterJson.put(CRMReportKeys.FUNCTION_ID, filter.getFunctionId());
                filterJson.put(CRMReportKeys.ORDER, filterOrder);

                final JsonArray filterValuesArray = Json.array();
                final CRMTemplateFilterValues crmTemplateFilterValues = filter.getTemplateFilterValues().stream()
                                                                                                        .skip(filterOrder - 1L)
                                                                                                        .findFirst()
                                                                                                        .orElse(null);

                if (Objects.nonNull(crmTemplateFilterValues)) {
                    final JsonObject filterValues = Json.object();
                    final Long filterFrom = crmTemplateFilterValues.getFrom();
                    final Long filterTo = crmTemplateFilterValues.getTo();
                    final Long filterValue = crmTemplateFilterValues.getTimeUnitValue();

                    if (Objects.nonNull(filterFrom) && Objects.nonNull(filterTo)) {
                        filterValues.put(CommonKeys.FROM, filterFrom);
                        filterValues.put(CommonKeys.TO, filterTo);
                    }

                    if (Objects.nonNull(filterValue)) {
                        filterValues.put(CRMReportKeys.VALUE, filterValue);
                    }
                    filterValuesArray.put(filterValues);
                }

                filterJson.put(CRMReportKeys.VALUES, filterValuesArray);
                filterArray.put(filterJson);
            }
        }
    }

    /**
     * Loads compare to JsonObject with data
     *
     * @param crmTemplateCompareTo Represents the {@link CRMTemplateCompareTo} details
     * @param compareToJson        Represents the {@link JsonObject}
     */
    private void getCompareToJson(final CRMTemplateCompareTo crmTemplateCompareTo, final JsonObject compareToJson) {
        if (Objects.nonNull(crmTemplateCompareTo)) {
            final Long compareToFromValue = crmTemplateCompareTo.getFrom();
            final Long compareToToValue = crmTemplateCompareTo.getTo();

            compareToJson.put(CRMReportKeys.COMPARATOR_ID, crmTemplateCompareTo.getComparatorId());

            if (Objects.nonNull(compareToFromValue)) {
                compareToJson.put(CommonKeys.FROM, compareToFromValue);
            }

            if (Objects.nonNull(compareToToValue)) {
                compareToJson.put(CommonKeys.TO, compareToToValue);
            }
        }
    }

    /**
     * Loads duration JsonObject with data
     *
     * @param crmTemplateDuration Represents the {@link CRMTemplateDuration} details
     * @param reportFieldMap      Represents the {@link Map} that contains reportModularFields
     * @param durationJson        Represents the {@link JsonObject}
     * @param crmResponse         Represents the {@link CrmResponse}
     */
    private void getDurationJson(final CRMTemplateDuration crmTemplateDuration, final Map<Long, Long> reportFieldMap,
                                 final JsonObject durationJson, final CrmResponse crmResponse) {
        if (Objects.nonNull(crmTemplateDuration)) {
            final Long durationFrom = crmTemplateDuration.getFrom();
            final Long durationTo = crmTemplateDuration.getTo();

            durationJson.put(CRMReportKeys.FIELD_ID, reportFieldMap.get(crmTemplateDuration.getFieldId()));
            durationJson.put(CRMKPIKeys.TIME_UNIT_ID, crmTemplateDuration.getTimeUnitId());
            durationJson.put(CRMViewPropertyKeys.LABEL, crmResponse.resolveI18nPlaceholder(crmTemplateDuration.getLabel()));

            if (Objects.nonNull(durationFrom)) {
                durationJson.put(CommonKeys.FROM, durationFrom);
            }

            if (Objects.nonNull(durationTo)) {
                durationJson.put(CommonKeys.TO, durationTo);
            }
        }
    }

    /**
     * Loads viewBy array with viewBy data
     *
     * @param crmTemplateViewBy Represents the {@link CRMTemplateViewBy} details
     * @param reportFieldMap    Represents the {@link Map} that contains reportModularFields
     * @param viewByArray       Represents the {@link JsonArray}
     * @param crmResponse       Represents the {@link CrmResponse}
     */
    private void getViewByArray(final Collection<CRMTemplateViewBy> crmTemplateViewBy, final Map<Long, Long> reportFieldMap,
                                final JsonArray viewByArray, final CrmResponse crmResponse) {
        if (!crmTemplateViewBy.isEmpty()) {

            for (final CRMTemplateViewBy viewBy : crmTemplateViewBy) {
                final JsonObject viewByJson = Json.object();
                final Long viewByFieldId = viewBy.getFieldId();
                final Integer viewByModuleId = viewBy.getModuleId();
                final Integer viewBySubModuleId = viewBy.getSubModuleId();
                final Long viewByTimePeriodFieldId = viewBy.getTimePeriodFieldId();

                viewByJson.put(ReportFieldKeys.LABEL, crmResponse.resolveI18nPlaceholder(viewBy.getLabel()));
                viewByJson.put(CRMReportKeys.ORDER, viewBy.getOrder());

                if (Objects.nonNull(viewByFieldId)) {
                    viewByJson.put(CRMReportKeys.FIELD_ID, reportFieldMap.get(viewByFieldId));
                }

                if (Objects.nonNull(viewByModuleId)) {
                    viewByJson.put(CRMReportKeys.MODULE_ID, viewByModuleId);
                }

                if (Objects.nonNull(viewBySubModuleId)) {
                    viewByJson.put(CRMReportKeys.SUB_MODULE_ID, viewBySubModuleId);
                }

                if (Objects.nonNull(viewByTimePeriodFieldId)) {
                    viewByJson.put(CRMReportKeys.TIME_PERIOD_FIELD_ID, viewByTimePeriodFieldId);
                }

                final CRMTemplateViewByBucket crmTemplateViewByBucket = viewBy.getTemplateViewByBucket();
                final JsonObject bucket = Json.object();
                final Long bucketFrom = crmTemplateViewByBucket.getFrom();
                final Long bucketTo = crmTemplateViewByBucket.getTo();
                final Long bucketUnitId = crmTemplateViewByBucket.getUnitId();

                if ((Objects.nonNull(bucketFrom) && Objects.nonNull(bucketTo)) || Objects.nonNull(bucketUnitId)) {

                    if (Objects.nonNull(bucketFrom)) {
                        bucket.put(CommonKeys.FROM, bucketFrom);
                    }

                    if (Objects.nonNull(bucketTo)) {
                        bucket.put(CommonKeys.TO, bucketTo);
                    }

                    if (Objects.nonNull(bucketUnitId)) {
                        bucket.put(CRMReportKeys.UNIT_ID, bucketUnitId);
                    }

                    viewByJson.put(CRMReportKeys.BUCKET, bucket);
                }
                viewByArray.put(viewByJson);
            }
        }
    }
}
