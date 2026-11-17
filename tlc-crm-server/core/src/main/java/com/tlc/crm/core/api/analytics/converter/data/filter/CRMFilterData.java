package com.tlc.crm.core.api.analytics.converter.data.filter;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.service.table.TableDetailService;
import com.tlc.analytics.config.model.view.report.dynamic.filter.AnalyticsFilterData;
import com.tlc.analytics.core.api.FilterDataRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.path.PathMappingService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;
import com.tlc.crm.core.api.analytics.converter.general.fields.CRMViewFieldsConverter;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.i18n.I18nResolver;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class processes CRM filter data based on organization and user details, retrieves relevant fields, and then
 * converts them into a structured JSON response format.
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class CRMFilterData implements CRMReportsData {
    private final ColumnDetailsService columnDetailsService = ColumnDetailsService.getInstance();
    private final TableDetailService tableDetailService = TableDetailService.getInstance();
    private final PathMappingService pathMappingService = PathMappingService.getInstance();
    private final ReportModularFieldService reportModularFieldService = ReportModularFieldService.getInstance();

    private static class Instance {
        private static final CRMFilterData INSTANCE = new CRMFilterData();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link CRMFilterData} singleton
     */
    public static CRMFilterData INSTANCE() {
        return Instance.INSTANCE;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMFilterData.class);

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestData      Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     * @return The list of {@link GraphAxis}
     */
    @Override
    public List<GraphAxis<Object>> getGraphAxis(final @NonNull AnalyticsRequest analyticsRequest,
                                                final @NonNull ActionType actionType,
                                                final @NonNull JsonObject requestData,
                                                final @NonNull I18nResolver i18nResolver,
                                                final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {

            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                try {
                    getResponse(orgId, fetchFilterData(orgId, analyticsRequest.userId(), requestData, reportFieldMap), Collections.emptyMap(),
                            i18nResolver).forEach(crmResponse::put);
                    crmResponse.send();
                } catch (final Exception ex) {
                    LOGGER.error("CRM Analytics: Failed to fetch filter data for orgId: {}", orgId, ex);
                    crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.FILTER_DATA_FETCH_ACTION_FAILED,
                            "i18n.analytics.filter.data.fetch.action.failed"));
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics: Failed to fetch report field map for filter data, orgId: {}", orgId, cause);
                crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.FILTER_DATA_FETCH_ACTION_FAILED,
                        "i18n.analytics.filter.data.fetch.action.failed"));
                crmResponse.send();
            }
        });

        return Collections.emptyList();
    }

    /**
     * Fetches filter data using the pre-fetched report field map.
     *
     * @param reportFieldMap pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    private List<GraphAxis<Object>> fetchFilterData(final Long orgId, final Long userId, final JsonObject requestData,
                                                    final Map<Long, ReportModularField> reportFieldMap) throws Exception {
        final Collection<Long> involvedColumnIds = new LinkedHashSet<>();
        final CRMViewFieldsConverter converter = CRMViewFieldsConverter.INSTANCE();
        final ANAField anaField = converter.covert(orgId, System.currentTimeMillis(), requestData.getLong(CRMReportKeys.FIELD_ID), null,
                2L, false, Json.object(), involvedColumnIds, reportFieldMap);
        final AnalyticsFilterData analyticsFilter = getAnalyticsFilter(orgId, anaField, requestData);
        final Set<Long> columnIds = pathMappingService.getColumnIds(anaField.getReportFields(), anaField.getFilterFields());
        final Collection<ColumnDetail> columnDetails = columnDetailsService.getAll(columnIds);
        final Map<Long, ColumnDetail> columnDetailsMap = columnDetails.stream().collect(Collectors.toMap(ColumnDetail::id, Function.identity()));
        final Set<Long> involvedTables = columnDetails.stream().map(ColumnDetail::getTableId).collect(Collectors.toSet());
        final Collection<TableDetail> involvedTableDetails = tableDetailService.getAll(involvedTables);
        final Map<Long, TableDetail> tableDetailsMap = involvedTableDetails.stream().collect(Collectors.toMap(TableDetail::id, Function.identity()));
        final TableDetail rootTableDetail = pathMappingService.getRootTableDetails(anaField.getRootTableId(), involvedTableDetails);

        final FilterDataRequest filterDataRequest = FilterDataRequest.builder().view(analyticsFilter)
                .columnDetailsMap(columnDetailsMap).tableDetailMap(tableDetailsMap).rootTableDetail(rootTableDetail)
                .orgId(orgId).userId(userId).build();

        LOGGER.info("CRM Analytics: Successfully created a data filter request for orgId: {} with anaFieldIds: {}", orgId, anaField.getColumnIds());
        return CRMFilterDataConverter.INSTANCE().getGraphAxisResponse(filterDataRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organisation id
     * @param graphAxes  Contains the list of {@link GraphAxis}
     * @param crmUserMap The user id as a key and {@link CrmUser} as value
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject getResponse(final @NonNull Long orgId,
                                  final @NonNull List<GraphAxis<Object>> graphAxes,
                                  final @NonNull Map<Long, CrmUser> crmUserMap,
                                  final @NonNull I18nResolver i18nResolver) {
        final JsonObject dataJson = Json.object();
        final JsonArray rowArray = Json.array();

        for (final GraphAxis<Object> graphAxis : graphAxes) {
            rowArray.put(graphAxis.getColumnMap().isEmpty() ? graphAxis.getValue() :
                    crmUserMap.get(NumberUtils.parseNumber(graphAxis.getValue())
                                    .longValue())
                            .getFullName());
        }

        return dataJson.put(FilterFieldKeys.VALUES, rowArray);
    }

    /**
     * This method retrieves and structures analytics filter data
     *
     * @param orgId       Represents the organization ID
     * @param anaField    It holds the {@link ANAField} details
     * @param requestData the JSON object with request details
     * @return an {@link AnalyticsFilterData} object containing structured filter data
     * @throws Exception if an error occurs during filter data retrieval or processing
     */
    private AnalyticsFilterData getAnalyticsFilter(final @NonNull Long orgId,
                                                   final @NonNull ANAField anaField,
                                                   final @NonNull JsonObject requestData) throws Exception {

        return AnalyticsFilterData.builder()
                .filterFields(anaField.getFilterFields())
                .reportFields(anaField.getReportFields())
                .rootTableId(anaField.getRootTableId())
                .build();
    }
}
