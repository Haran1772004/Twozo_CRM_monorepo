package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.cache.AnalyticsConfigCache;
import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.service.table.TableDetailService;
import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.core.api.AnalyticsResponseProvider;
import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.path.PathMappingService;
import com.tlc.analytics.core.api.response.ResponseProvider;

import com.tlc.analytics.sql.resource.ANACONFIGURABLEFUNCTIONDETAILS;
import com.tlc.analytics.sql.resource.ANAPARENTCONFIGURABLEFUNCTIONDETAILS;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;
import com.tlc.crm.core.api.analytics.converter.data.ColumnDetailsPatternResolver;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Handling and processing {@link GraphAxis} data used in chart rendering.
 *
 * @author karthick V
 * @version 1.0
 * @see CRMReportsData
 */
public abstract class AbstractViewData implements CRMReportsData {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractViewData.class);
    private static final String COLUMN_NAME_KEY_FORMAT = "%s_%s_%s_null_%s";

    private final AnalyticsResponseProvider analyticsResponseProvider;
    private final AnalyticsConfigCache analyticsConfigCache;
    private final ColumnDetailsService columnDetailsService;
    private final TableDetailService tableDetailService;
    private final PathMappingService chPathMappingService;
    private final CRMReportsAPI crmReportsAPI;
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMChartDataConverter crmChartDataConverter;
    private final AnalyticsConfigManagerImpl analyticsConfigManager;
    private final ColumnDetailsPatternResolver columnDetailsPatternResolver;

    /**
     * Creates chart-view data dependencies.
     */
    public AbstractViewData() {
        analyticsResponseProvider = AnalyticsResponseProvider.INSTANCE();
        analyticsConfigCache = AnalyticsConfigCache.INSTANCE();
        columnDetailsService = ColumnDetailsService.getInstance();
        tableDetailService = TableDetailService.getInstance();
        chPathMappingService = PathMappingService.getInstance();
        crmReportsAPI = CRMReportsAPI.getInstance();
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        crmChartDataConverter = CRMChartDataConverter.INSTANCE();
        analyticsConfigManager = AnalyticsConfigManagerImpl.INSTANCE();
        columnDetailsPatternResolver = ColumnDetailsPatternResolver.INSTANCE();
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestData      Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @return The list of {@link GraphAxis}
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @Override
    public List<GraphAxis<Object>> getGraphAxis(final @NonNull AnalyticsRequest analyticsRequest,
                                                final @NonNull ActionType actionType,
                                                final @NonNull JsonObject requestData,
                                                final @NonNull I18nResolver i18nResolver,
                                                final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();

        try {
            final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap = new LinkedHashMap<>();
            final Collection<ChartRequest> chartRequests = createChartRequest(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
            final ResponseProvider<ChartView, ChartRequest> chartResponseProvider = analyticsResponseProvider.getChartResponseProvider();

            for (final ChartRequest chartRequest : chartRequests) {
                final List<GraphAxis<Object>> graphAxes = chartResponseProvider.getGraphAxisResponse(chartRequest);

                graphAxisMap.put(chartRequest, graphAxes);
            }

            return shouldFlattenGraphAxes(graphAxisMap) ? flattenGraphAxes(graphAxisMap) : mergeGraphAxis(graphAxisMap);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch chart response for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.chart.data.fetch.action.failed");
        }
    }

    /**
     * <p>
     * Determines whether the graph axes should be flattened rather than merged, based on whether
     * the last chart request's response has exactly X-axis entries.
     * </p>
     *
     * @param graphAxisMap the map of chart requests to their graph axis lists
     * @return {@code true} if the axes should be flattened, {@code false} otherwise
     */
    private boolean shouldFlattenGraphAxes(final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap) {
        if (graphAxisMap.isEmpty()) {
            return false;
        }

        List<GraphAxis<Object>> lastEntry = null;
        final Collection<List<GraphAxis<Object>>> values = graphAxisMap.values();

        for (final List<GraphAxis<Object>> value : values) {
            lastEntry = value;
        }

        if (lastEntry == null) {
            return false;
        }

        int xAxisCount = 0;

        for (final GraphAxis<Object> axis : lastEntry) {
            if (Axis.X_AXIS == axis.getReportField().getAxis()) {
                xAxisCount++;
            }
        }

        return xAxisCount == NumericalConstant.TWO;
    }

    /**
     * <p>
     * Flattens all graph axis lists from the map values into a single list.
     * </p>
     *
     * @param graphAxisMap the map of chart requests to their graph axis lists
     * @return a combined flat list of all graph axes
     */
    private List<GraphAxis<Object>> flattenGraphAxes(final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap) {
        return graphAxisMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organisation id
     * @param graphAxes  Contains the list of {@link GraphAxis}
     * @param crmUserMap The map key represents the user id and value is {@link CrmUser} details
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject getResponse(final @NonNull Long orgId,
                                  final @NonNull List<GraphAxis<Object>> graphAxes,
                                  final @NonNull Map<Long, CrmUser> crmUserMap,
                                  final @NonNull I18nResolver i18nResolver) {
        try {
            return crmChartDataConverter.getData(orgId, graphAxes, crmUserMap, i18nResolver);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch chart response for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.chart.data.fetch.action.failed");
        }
    }

    /**
     * <p>
     * Creates a collection of {@link ChartRequest} objects based on the provided parameters.
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       The type of action being performed, must not be null
     * @param requestData      The JsonObject containing the request data, must not be null
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return the collection of {@link ChartRequest} details
     */
    private Collection<ChartRequest> createChartRequest(final AnalyticsRequest analyticsRequest,
                                                        final ActionType actionType,
                                                        final JsonObject requestData,
                                                        final I18nResolver i18nResolver,
                                                        final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();
        final Collection<ChartView> chartViews = getChartView(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
        final boolean isApplyConfiguration = ActionType.APPLY == actionType;
        final Collection<ChartRequest> chartRequests = new ArrayList<>(chartViews.size());

        for (final ChartView chartView : chartViews) {
            chartRequests.add(buildChartRequest(orgId, chartView, isApplyConfiguration, analyticsRequest));
        }

        return chartRequests;
    }

    /**
     * <p>
     * Builds a single {@link ChartRequest} from a {@link ChartView} and its associated configurations.
     * </p>
     *
     * @param orgId                the organisation id
     * @param chartView            the chart view configuration
     * @param isApplyConfiguration whether this is an apply action
     * @param analyticsRequest     the original analytics request
     * @return a fully populated {@link ChartRequest}
     */
    private ChartRequest buildChartRequest(final Long orgId,
                                           final ChartView chartView,
                                           final boolean isApplyConfiguration,
                                           final AnalyticsRequest analyticsRequest) {
        final Collection<ReportField> reportFields = chartView.getReportFields();
        final Collection<FilterField> filterFields = chartView.getFilterFields();
        final Set<Long> columnIds = chPathMappingService.getColumnIds(reportFields, filterFields);

        chPathMappingService.loadColumnRelationDetails(orgId, columnIds, chartView.getReportPathMapping());

        final Collection<ColumnDetail> chColumnDetails = columnDetailsService.getAll(columnIds);
        final Set<Long> involvedTableIds = chColumnDetails.stream().map(ColumnDetail::getTableId).collect(Collectors.toSet());
        final Map<Long, TableDetail> tableDetailsMap = tableDetailService.getAll(involvedTableIds).stream()
                .collect(Collectors.toMap(TableDetail::id, table -> table));
        final Map<Long, ColumnDetail> columnDetailsMap = chColumnDetails.stream()
                .collect(Collectors.toMap(ColumnDetail::id, column -> column));
        final Collection<RelationsInPath> relationsInPaths = chartView.getReportPathMapping().getPathInfo().getRelationsInPaths();
        final Map<Long, TableRelation> tableRelationMap = (relationsInPaths != null) && !relationsInPaths.isEmpty()
                ? chPathMappingService.getTableRelations(orgId, relationsInPaths) : new HashMap<>();
        final Map<Long, ConfigurableFunctionDetail> configurableFunctionDetailsMap = new HashMap<>();

        buildFunctionColumn(orgId, chartView, configurableFunctionDetailsMap);

        final Map<String, Long> columnNameDetails = isApplyConfiguration ? buildColumnNameDetails(orgId, chartView, columnDetailsMap)
                : new HashMap<>();

        return ChartRequest.builder()
                .orgId(orgId)
                .userId(analyticsRequest.userId())
                .view(chartView)
                .rootTableDetail(chPathMappingService.getRootTableDetails(chartView.getRootTableId(), tableDetailsMap.values()))
                .tableDetailMap(tableDetailsMap)
                .tableRelationMap(tableRelationMap)
                .configurableFunctionDetailsMap(configurableFunctionDetailsMap)
                .columnDetailsMap(columnDetailsMap)
                .columnNameDetails(columnNameDetails)
                .isApplyConfiguration(isApplyConfiguration)
                .timezoneInfo(analyticsRequest.timezoneInfo())
                .columnDetailsPatternMap(columnDetailsPatternResolver.getColumnPatternMap(reportFields, filterFields))
                .chartUniqueId(chartView.getChartUniqueId())
                .build();
    }

    /**
     * <p>
     * Builds column name details mapping for the apply configuration action. Maps a composite
     * column key (column name, operation type, field id, chart unique id) to its computed axis order.
     * </p>
     *
     * @param orgId            the organisation id
     * @param chartView        the chart view containing report fields
     * @param columnDetailsMap the map of column id to {@link ColumnDetail}
     * @return a map of composite column keys to their axis order values
     */
    private Map<String, Long> buildColumnNameDetails(final Long orgId,
                                                     final ChartView chartView,
                                                     final Map<Long, ColumnDetail> columnDetailsMap) {
        final Map<String, Long> columnNameDetails = new HashMap<>();
        final Long chartUniqueId = chartView.getChartUniqueId();

        try {
            CodeBlockExecutor.forEachSafely(chartView.getReportFields(), reportField ->
            {
                final ColumnDetail columnInfo = columnDetailsMap.get(reportField.getColumnId());

                if (columnInfo == null) {
                    return;
                }

                final int axisType = reportField.getAxisType();
                final String key = String.format(COLUMN_NAME_KEY_FORMAT, columnInfo.getColumnName(), reportField.getOperationType().getTypeCode(),
                        reportField.getFieldId(), chartUniqueId);
                final long order = Axis.X_AXIS == reportField.getAxis() ? axisType + reportField.getOrder() : axisType + NumericalConstant.THREE;

                columnNameDetails.put(key, order);
            });
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics: Failed to apply configuration for orgId: {}", orgId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.chart.data.fetch.action.failed");
        }

        return columnNameDetails;
    }

    /**
     * <p>
     * Builds the configuration function details column.
     * </p>
     *
     * @param orgId                          Represents an organisation id
     * @param chartView                      Represents the {@link ChartView} details
     * @param configurableFunctionDetailsMap The map contains the column id as the key and the value as {@link ConfigurableFunctionDetail}
     */
    private void buildFunctionColumn(final Long orgId,
                                     final ChartView chartView,
                                     final Map<Long, ConfigurableFunctionDetail> configurableFunctionDetailsMap) {
        final AnalyticsConfigManager<ColumnDetail> configurableFunctionManager = analyticsConfigManager.getColumnManager();
        final Collection<Long> columnIds = chartView.getReportFields().stream().map(BaseField::getColumnId).toList();
        final Set<Long> functionColumnIds = configurableFunctionManager.get(orgId, columnIds).stream().map(ColumnDetail::id)
                .collect(Collectors.toSet());

        if (functionColumnIds.isEmpty()) {
            return;
        }

        final Table configurableFunctionTable = analyticsConfigManager.getConfigurableManager().getTable();
        final Table parentFunctionTable = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(configurableFunctionTable);

        selectQuery.addSelectClause(configurableFunctionTable, parentFunctionTable);
        selectQuery.addJoinClause(new JoinClause(configurableFunctionTable.getPKColumn(),
                parentFunctionTable.getColumn(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(configurableFunctionTable.getColumn(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID),
                functionColumnIds)));

        final DataContainer dataContainer = DataStoreProvider.getSecureOrgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(configurableFunctionTable).forEach(row -> configurableFunctionDetailsMap.put(
                row.get(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID), buildConfigurationFunctionDetails(dataContainer, row)));
    }

    /**
     * <p>
     * Builds the configuration details based on the given {@link Row}.
     * </p>
     *
     * @param dataContainer Holds the function related details
     * @param row           Represents the configuration function details row
     * @return The {@link ConfigurableFunctionDetail}
     */
    private ConfigurableFunctionDetail buildConfigurationFunctionDetails(final DataContainer dataContainer,
                                                                         final Row row) {
        final Table parentFunctionTable = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final Row parentConfigurationRow = dataContainer.getRow(parentFunctionTable,
                new WhereClause(Criteria.eq(parentFunctionTable.getColumn(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID),
                        row.getPKValue())));

        final ParentConfigurableFunctionDetail parentConfigurableFunctionDetail = ParentConfigurableFunctionDetail.builder()
                .columnFunctionId(parentConfigurationRow.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID))
                .parentColumnId(parentConfigurationRow.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.PARENT_COLUMN_ID))
                .build();

        return ConfigurableFunctionDetail.builder()
                .finalFunction(row.get(ANACONFIGURABLEFUNCTIONDETAILS.FINAL_FUNCTION))
                .columnId(row.get(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID))
                .functionTypeId(row.get(ANACONFIGURABLEFUNCTIONDETAILS.FUNCTION_TYPE_ID))
                .parentColumnFunctionDetails(List.of(parentConfigurableFunctionDetail))
                .build();
    }

    /**
     * <p>
     * Retrieves the {@link ChartView} entity.
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       The type of action being performed, must not be null
     * @param requestData      The JsonObject containing the request data, must not be null
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return the {@link ChartView} details
     */
    private Collection<ChartView> getChartView(final AnalyticsRequest analyticsRequest,
                                               final ActionType actionType,
                                               final JsonObject requestData,
                                               final I18nResolver i18nResolver,
                                               final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();

        if (ActionType.APPLY == actionType) {
            return crmReportsAPI.getChartConfigAPI().getModelConfig(analyticsRequest, requestData, i18nResolver, reportFieldMap);
        }

        final Set<Long> anaViewIds = crmViewManagerAPI.getCRMChildViewManager()
                .get(orgId, requestData.getLong(CRMReportKeys.VIEW_ID)).getCrmViewMapper()
                .stream()
                .map(CRMANAViewMapper::getAnaViewId)
                .collect(Collectors.toSet());

        return analyticsConfigCache.chartViewManager().getAll(orgId, anaViewIds);
    }

    /**
     * <p>
     * Merges all {@link GraphAxis} lists from the given map into a single list.
     * </p>
     *
     * @param graphAxisMap a map where each key is a {@link ChartRequest} and each value
     *                     is a list of {@link GraphAxis} objects
     * @return a combined list containing all {@link GraphAxis} instances from the map values
     */
    protected abstract List<GraphAxis<Object>> mergeGraphAxis(final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap);
}
