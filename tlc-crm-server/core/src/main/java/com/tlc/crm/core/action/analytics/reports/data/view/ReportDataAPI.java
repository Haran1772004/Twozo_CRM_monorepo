package com.tlc.crm.core.action.analytics.reports.data.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.action.analytics.reports.data.ReportEntityResolver;
import com.tlc.crm.core.action.analytics.reports.data.UserIdFetchHandler;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.integ.general.analytics.CRMChartViewTypeService;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.Validator;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;

import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Manages report data related web actions
 * </p>
 *
 * @author jamesh
 * @version 1.0
 * @see ReportsAction
 */
@AnalyticsAuthorizer
@WebAction(path = "/crm/analytics/view-data")
public class ReportDataAPI extends ReportsAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportDataAPI.class);

    private final UserIdFetchHandler userIdFetchHandler;
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final Validator validator;
    private final CRMReportsAPI crmReportsAPI;
    private final CRMChartViewTypeService crmChartViewTypeService;
    private final IconService iconService;
    private final ReportEntityResolver reportEntityResolver;
    private final CRMViewConfigManager<CRMChildViewDetails> crmChildViewDetailsManager;
    private final ReportModularFieldService reportModularFieldService;

    /**
     * Creates report-data action dependencies.
     */
    public ReportDataAPI() {
        userIdFetchHandler = UserIdFetchHandler.getINSTANCE();
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        validator = Validator.INSTANCE();
        crmReportsAPI = CRMReportsAPI.getInstance();
        crmChartViewTypeService = CRMChartViewTypeService.INSTANCE();
        iconService = IconService.getInstance();
        reportEntityResolver = ReportEntityResolver.getInstance();
        crmChildViewDetailsManager = crmViewManagerAPI.getCRMChildViewManager();
        reportModularFieldService = ReportModularFieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Synchronous validation only performs non-async checks.
     * View validation requires async report field fetching, so the actual view validation
     * is handled in {@link #validateRequest(Long, Long, JsonObject, ActionType, Consumer)}.
     * </p>
     *
     * @param orgId      It represents the organization ID
     * @param userId     It represents the user ID
     * @param data       The JsonObject containing the request data, must not be null
     * @param actionType The type of action being performed, must not be null
     * @throws Exception if the validation fails or any error occurs during validation
     */
    @Override
    protected void validateRequest(final @NonNull Long orgId,
                                   final @NonNull Long userId,
                                   final @NonNull JsonObject data,
                                   final @NonNull ActionType actionType) throws Exception {
        if (ActionType.APPLY == actionType) {
            validator.viewApplyValidator()
                    .validateRequest(orgId, userId, data);
        } else {
            validator.viewGetValidator()
                    .validateRequest(orgId, userId, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Async version that delegates to the validator's async method, enabling non-blocking
     * validation when report modular fields need to be fetched from the cache.
     * </p>
     *
     * @param orgId             It represents the organization ID
     * @param userId            It represents the user ID
     * @param data              The JsonObject containing the request data, must not be null
     * @param actionType        The type of action being performed, must not be null
     * @param completionHandler Called with {@code null} on success, or with the exception on failure
     */
    @Override
    protected void validateRequest(final @NonNull Long orgId,
                                   final @NonNull Long userId,
                                   final @NonNull JsonObject data,
                                   final @NonNull ActionType actionType,
                                   final @NonNull Consumer<Exception> completionHandler) {
        if (ActionType.APPLY == actionType) {
            validator.viewApplyValidator()
                    .validateRequest(orgId, userId, data, completionHandler);
        } else {
            validator.viewGetValidator()
                    .validateRequest(orgId, userId, data, completionHandler);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fetches the report modular fields asynchronously and, on success, resolves the graph
     * axis data and dispatches the response. On failure, sends an error response immediately.</p>
     *
     * @param analyticsRequest the analytics request context, including org, user, and response handle
     * @param actionType       the type of action being performed, must not be null
     * @param requestData      the JSON payload containing request-specific data, must not be null
     * @param i18nResolver     an instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    protected final void processRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                        final @NonNull ActionType actionType,
                                        final @NonNull JsonObject requestData,
                                        final @NonNull I18nResolver i18nResolver) {
        final long orgId = analyticsRequest.orgId();
        final CrmResponse crmResponse = analyticsRequest.crmResponse();
        final CRMViewType viewType = getViewType(orgId, actionType, requestData);

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                try {
                    processGraphAxisAndResponse(viewType, analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap, crmResponse);
                } catch (final Exception exception) {
                    LOGGER.error("CRM Analytics: Failed to fetch report modular fields for view-data, orgId: {}", orgId, exception);
                    crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.VIEW_APPLY_ACTION_FAILED, "i18n.analytics.view.apply.action.failed"));
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("getByOrgId callback failed for metrics, orgId: {} and viewTypeId: {}", orgId, viewType.getTypeCode(), cause);
                crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.VIEW_APPLY_ACTION_FAILED, "i18n.analytics.view.apply.action.failed", cause));
                crmResponse.send();
            }
        });
    }

    /**
     * <p>
     * Processes the graph axis data and sends the response
     * </p>
     *
     * @param viewType           Represents the {@link CRMViewType}
     * @param analyticsRequest   Represents the {@link AnalyticsRequest} details
     * @param actionType         Represents the {@link ActionType}
     * @param requestData        Represents the {@link JsonObject} details
     * @param i18nResolver       Represents the {@link I18nResolver}
     * @param reportFieldMap     The fetched ReportModularField map, may be null
     * @param crmResponse        Represents the {@link CrmResponse}
     */
    private void processGraphAxisAndResponse(final CRMViewType viewType,
                                            final AnalyticsRequest analyticsRequest,
                                            final ActionType actionType,
                                            final JsonObject requestData,
                                            final I18nResolver i18nResolver,
                                            final Map<Long, ReportModularField> reportFieldMap,
                                            final CrmResponse crmResponse) {
        final long orgId = analyticsRequest.orgId();
        final List<GraphAxis<Object>> graphAxisList = getGraphAxis(viewType, analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);

        reportEntityResolver.replaceEntityIdsWithDisplayNames(orgId, graphAxisList, reportFieldMap);

        final Collection<Long> userIds = userIdFetchHandler.getUserIds(orgId, graphAxisList, reportFieldMap);
        final boolean isKpi = viewType == CRMViewType.KPI;

        if (userIds.isEmpty()) {
            final JsonObject response = getJsonResponse(orgId, Collections.emptyMap(), graphAxisList, viewType, i18nResolver);

            if (!graphAxisList.isEmpty() && !isKpi) {
                response.put(CRMViewPropertyKeys.CONVERTIBLE_VIEW_TYPE, getConvertibleViewTypes(orgId, actionType, graphAxisList, requestData, i18nResolver, reportFieldMap));
            }

            response.forEach(crmResponse::put);
            crmResponse.send();
            return;
        }

        fetchAndDispatchUser(orgId, userIds, graphAxisList, crmResponse, viewType, i18nResolver,
                isKpi ? Json.array() : getConvertibleViewTypes(orgId, actionType, graphAxisList, requestData, i18nResolver, reportFieldMap));
    }

    /**
     * {@inheritDoc }
     *
     * @param orgId      Represents an organisation id
     * @param crmUserMap Contains the collection of crm users
     * @param data       Represents the data
     * @return The {@link JsonObject} details
     */
    @NonNull
    @Override
    protected JsonObject getJsonResponse(final @NonNull Long orgId,
                                         final @NonNull Map<Long, CrmUser> crmUserMap,
                                         final @NonNull Object data,
                                         final CRMViewType viewType,
                                         final @NonNull I18nResolver i18nResolver) {
        return getViewSpecificAPI(viewType).getResponse(orgId, (List<GraphAxis<Object>>) data, crmUserMap, i18nResolver);
    }

    /**
     * <p>
     * Gets the list of {@link GraphAxis} details
     * </p>
     *
     * @param viewType         Represents the {@link CRMViewType}
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestData      Represents the {@link JsonObject} details
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @return The list of {@link GraphAxis}
     */
    private List<GraphAxis<Object>> getGraphAxis(final CRMViewType viewType,
                                                 final AnalyticsRequest analyticsRequest,
                                                 final ActionType actionType,
                                                 final JsonObject requestData,
                                                 final I18nResolver i18nResolver,
                                                 final Map<Long, ReportModularField> reportFieldMap) {
        return getViewSpecificAPI(viewType).getGraphAxis(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
    }

    /**
     * <p>
     * Resolves the view-specific report data API based on the given {@link CRMViewType}.
     * Falls back to the default chart data API for unrecognized or null view types.
     * </p>
     *
     * @param viewType Represents the {@link CRMViewType}
     * @return The view-specific {@link CRMReportsData} report data API
     */
    private CRMReportsData getViewSpecificAPI(final CRMViewType viewType) {
        if (viewType != null) {
            switch (viewType) {
                case KPI: return crmReportsAPI.getKPIDataAPI();
                case BUBBLE: return crmReportsAPI.getBubbleChartDataAPI();
                case HEAT_MAP: return crmReportsAPI.getHeapMapAPI();
                default: break;
            }
        }

        return crmReportsAPI.getChartDataAPI();
    }

    /**
     * <p>
     * Gets the {@link CRMViewType} based on the action type
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param actionType  Represents the {@link ActionType}
     * @param requestData Hods the request json details
     * @return The {@link CRMViewType} details
     */
    private CRMViewType getViewType(final Long orgId, final ActionType actionType, final JsonObject requestData) {
        return actionType.equals(ActionType.APPLY) ? CRMViewType.getCrmViewType(requestData.getInt(CRMReportKeys.VIEW_TYPE_ID)) :
                crmChildViewDetailsManager.get(orgId, requestData.getLong(CRMReportKeys.VIEW_ID)).getViewType();
    }

    /**
     * <p>
     * Resolves the convertible view types based on action type (GET or APPLY).
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param actionType    Represents the {@link ActionType}
     * @param graphAxisList The list of graph axes
     * @param requestData   The request data
     * @param i18nResolver  Represents the {@link I18nResolver}
     * @return The {@link JsonArray} containing convertible types
     */
    private JsonArray getConvertibleViewTypes(final long orgId, final ActionType actionType,
                                              final List<GraphAxis<Object>> graphAxisList,
                                              final JsonObject requestData,
                                              final I18nResolver i18nResolver,
                                              final Map<Long, ReportModularField> reportFieldMap) {
        return actionType == ActionType.GET ? getConvertibleViewTypesForGet(orgId, graphAxisList, i18nResolver, reportFieldMap)
                : getConvertibleTypesForApply(orgId, requestData, i18nResolver, reportFieldMap);
    }

    /**
     * <p>
     * Creates a jsonObject containing chart types to which the current chart type can be converted
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param crmViewType  Represents the {@link CRMViewType}
     * @param i18nResolver Represents the {@link I18nResolver}
     * @return The {@link JsonObject} containing convertible types
     */
    private JsonObject getConvertibleType(final Long orgId, final CRMViewType crmViewType, final I18nResolver i18nResolver) {
        final JsonObject convertibleViewTypeJson = Json.object();
        final String aliasName = crmViewType.getAliasName();

        convertibleViewTypeJson.put(CRMReportKeys.VIEW_TYPE_ID, CRMViewType.getTypeId(crmViewType, orgId));
        convertibleViewTypeJson.put(CRMReportKeys.NAME, aliasName);
        convertibleViewTypeJson.put(CommonKeys.DISPLAY_NAME, i18nResolver.get(iconService.fetchIconByTypeAndName(Icon.Type.CHART, aliasName).getI18nName()));

        return convertibleViewTypeJson;
    }

    /**
     * <p>
     * Creates a jsonArray containing chart types to which the current chart type can be converted
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param requestData  A {@link JsonObject} containing the request data needed to generate the response
     * @param i18nResolver Represents the {@link I18nResolver}
     * @return The {@link JsonArray} containing convertible types
     */
    private JsonArray getConvertibleTypesForApply(final long orgId, final @NonNull JsonObject requestData,
                                                  final @NonNull I18nResolver i18nResolver, final Map<Long, ReportModularField> reportFieldMap) {
        final JsonObject viewProperties = requestData.getJsonObject(CRMChildViewKeys.VIEW_PROPERTIES);
        final JsonArray metrics = viewProperties.getJsonArray(CRMChildViewKeys.METRICS);
        final int metricSize = metrics.size();
        final JsonArray viewByJsonArray = viewProperties.getJsonArray(CRMViewPropertyKeys.VIEW_BY);
        final int viewBySize = viewByJsonArray.size();
        final Set<Long> measureIds = new HashSet<>(metricSize);
        final List<Long> fieldIds = new ArrayList<>(viewBySize);
        boolean isSameMeasure = true;

        for (int index = NumericalConstant.ZERO; index < metricSize; index++) {
            measureIds.add(metrics.getJsonObject(index).getLong(CRMReportKeys.MEASURE_ID));

            if (measureIds.size() > NumericalConstant.ONE) {
                isSameMeasure = false;
                break;
            }
        }

        for (int index = NumericalConstant.ZERO; index < viewBySize; index++) {
            final JsonObject viewBy = viewByJsonArray.getJsonObject(index);

            fieldIds.add(viewBy.containsKey(CRMReportKeys.FIELD_ID) ? viewBy.getLong(CRMReportKeys.FIELD_ID) : null);
        }

        return buildConvertibleViewTypesArray(orgId, isSameMeasure, metricSize, fieldIds, viewBySize, i18nResolver, reportFieldMap);
    }

    /**
     * <p>
     * Creates a jsonArray containing chart types to which the current chart type can be converted
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param graphAxisList The list of graph axes to be included in the response.
     * @param i18nResolver  Represents the {@link I18nResolver}
     * @return The {@link JsonArray} containing convertible types
     */
    private JsonArray getConvertibleViewTypesForGet(final long orgId, final List<GraphAxis<Object>> graphAxisList,
                                                    final @NonNull I18nResolver i18nResolver, final Map<Long, ReportModularField> reportFieldMap) {
        final List<Long> fieldIds = new ArrayList<>();
        long firstOperationId = NumericalConstant.NEGATIVE_ONE;
        boolean isSameMeasure = true;
        int metricSize = NumericalConstant.ZERO;

        for (final GraphAxis<Object> graphAxis : graphAxisList) {
            final ReportField reportField = graphAxis.getReportField();
            final int axisType = reportField.getAxisType();

            if (axisType == NumericalConstant.ONE) {
                fieldIds.add(reportField.getFieldId());
            } else if (axisType == NumericalConstant.TWO) {
                final long operationId = reportField.getOperationId();

                if (metricSize == NumericalConstant.ZERO) {
                    firstOperationId = operationId;
                } else if (isSameMeasure && firstOperationId != operationId) {
                    isSameMeasure = false;
                }

                metricSize++;
            }
        }

        return buildConvertibleViewTypesArray(orgId, isSameMeasure, metricSize, fieldIds, fieldIds.size(), i18nResolver, reportFieldMap);
    }

    /**
     * <p>
     * Builds a {@link JsonArray} of convertible chart view types based on the given metric
     * and field configuration.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param isSameMeasure Whether all metrics use the same measure
     * @param metricSize    The number of metrics
     * @param fieldIds      The list of field IDs used in view-by
     * @param viewBySize    The number of view-by fields
     * @param i18nResolver  Represents the {@link I18nResolver}
     * @return The {@link JsonArray} containing convertible chart types
     */
    private JsonArray buildConvertibleViewTypesArray(final long orgId, final boolean isSameMeasure,
                                                     final int metricSize, final List<Long> fieldIds,
                                                     final int viewBySize,
                                                     final @NonNull I18nResolver i18nResolver,
                                                     final Map<Long, ReportModularField> reportFieldMap) {
        final EnumSet<CRMViewType> supportViewTypes = crmChartViewTypeService.getSupportedViewTypes(
                isSameMeasure, metricSize, orgId, fieldIds, viewBySize, reportFieldMap);
        final JsonArray jsonArray = Json.array();

        if (Objects.nonNull(supportViewTypes) && !supportViewTypes.isEmpty()) {

            for (final CRMViewType crmViewType : supportViewTypes) {

                if (CRMViewType.isChartType(crmViewType)) {
                    jsonArray.put(getConvertibleType(orgId, crmViewType, i18nResolver));
                }
            }
        }

        return jsonArray;
    }
}