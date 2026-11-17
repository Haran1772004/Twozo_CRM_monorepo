package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.cache.AnalyticsConfigCache;
import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.service.table.TableDetailService;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.core.api.AnalyticsResponseProvider;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.KPIRequest;
import com.tlc.analytics.core.api.path.PathMappingService;
import com.tlc.analytics.sql.resource.ANACONFIGURABLEFUNCTIONDETAILS;
import com.tlc.analytics.sql.resource.ANAPARENTCONFIGURABLEFUNCTIONDETAILS;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.converter.data.ColumnDetailsPatternResolver;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * Handles the processing of KPI data requests within the CRM reporting system.
 * Responsible for building KPI requests, fetching graph axis data from the analytics
 * response provider, and converting the results into JSON responses.
 *
 * @author jamesh
 * @version 1.0
 * @see CRMReportsData
 */
class CRMKPIData extends CRMKPIDataConverter implements CRMReportsData {
    private static final int DEFAULT_INDEX = 0;
    private static final int Y_AXIS_OFFSET = 3;
    private static final String COLUMN_NAME_KEY_FORMAT = "%s_%s_%s_%s_%s";
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMKPIData.class);

    private final AnalyticsResponseProvider analyticsResponseProvider;
    private final AnalyticsConfigCache analyticsConfigCache;
    private final ColumnDetailsService columnDetailsService;
    private final TableDetailService tableDetailService;
    private final PathMappingService chPathMappingService;
    private final CRMReportsAPI crmReportsAPI;
    private final AnalyticsConfigManagerImpl configManager;
    private final ColumnDetailsPatternResolver columnDetailsPatternResolver;
    private final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager;

    private CRMKPIData() {
        this.analyticsResponseProvider = AnalyticsResponseProvider.INSTANCE();
        this.analyticsConfigCache = AnalyticsConfigCache.INSTANCE();
        this.columnDetailsService = ColumnDetailsService.getInstance();
        this.tableDetailService = TableDetailService.getInstance();
        this.chPathMappingService = PathMappingService.getInstance();
        this.crmReportsAPI = CRMReportsAPI.getInstance();
        this.configManager = AnalyticsConfigManagerImpl.INSTANCE();
        this.columnDetailsPatternResolver = ColumnDetailsPatternResolver.INSTANCE();
        final CRMViewManagerAPI crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        this.crmChildViewManager = crmViewManagerAPI.getCRMChildViewManager();
    }

    private static class Instance {
        private static final CRMKPIData INSTANCE = new CRMKPIData();
    }

    /**
     * Returns the singleton instance of {@link CRMKPIData}.
     *
     * @return the singleton instance
     */
    public static CRMKPIData INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Fetches graph axis data for a KPI analytics request by creating a KPI request,
     * iterating over its comparators, and collecting the axis responses.
     * Returns a default axis with value 0 for any comparator that yields no results.
     *
     * @param analyticsRequest the analytics request details (must not be null)
     * @param actionType       the action type (must not be null)
     * @param requestData      the JSON request data (must not be null)
     * @param i18nResolver     the internationalization resolver (must not be null)
     * @return the list of {@link GraphAxis} details
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
            final KPIRequest kpiRequest = createKPIRequest(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
            final Collection<KPIComparator> kpiComparators = kpiRequest.getView().getComparators();
            final List<GraphAxis<Object>> graphAxis = new ArrayList<>();
            final Long viewId = kpiRequest.getViewId();

            LOGGER.info("CRM Analytics: Successfully created KPI request for orgId: {} and viewId: {}", orgId, viewId);

            for (final KPIComparator kpiComparator : kpiComparators) {
                final KPIComparatorSide kpiComparatorSide = kpiComparator.getComparatorSide();

                kpiRequest.setKpiComparator(kpiComparator);

                LOGGER.info("CRM Analytics: Going to fetch KPI graph axis for orgId: {} and viewId: {}", orgId, viewId);

                final List<GraphAxis<Object>> graphAxes = analyticsResponseProvider.getKPIResponseProvider()
                        .getGraphAxisResponse(kpiRequest);

                if (graphAxes.isEmpty()) {
                    final GraphAxis<Object> emptyAxis = new GraphAxis<>(null, null);

                    emptyAxis.setComparatorSide(kpiComparatorSide);
                    emptyAxis.getAxisValues().add(AxisValue.<Object>create(DEFAULT_INDEX));
                    emptyAxis.setOperationType(kpiRequest.getOperationType());
                    graphAxis.add(emptyAxis);
                } else {
                    graphAxes.get(DEFAULT_INDEX).setComparatorSide(kpiComparatorSide);
                    graphAxes.get(DEFAULT_INDEX).setComparatorType(Objects.equals(KPIComparatorSide.RIGHT, kpiComparatorSide)
                            ? kpiComparator.getComparatorType() : null);

                    graphAxis.addAll(graphAxes);
                }
            }

            LOGGER.info("CRM Analytics: Successfully fetched KPI graph axis for orgId: {} and viewId: {}", orgId, viewId);

            return graphAxis;
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to get KPI graph axis for orgId: {}. Reason: {}", orgId, error.getMessage(), error);
            throw ErrorCode.get(AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.chart.data.fetch.action.failed");
        }
    }

    /**
     * Converts the fetched graph axis data into a JSON response by delegating
     * to the parent converter's {@code getData} method.
     *
     * @param orgId        the organization ID (must not be null)
     * @param graphAxes    the list of {@link GraphAxis} (must not be null)
     * @param crmUserMap   the map of user IDs to {@link CrmUser} (must not be null)
     * @param i18nResolver the internationalization resolver (must not be null)
     * @return the {@link JsonObject} response
     */
    @Override
    public JsonObject getResponse(final @NonNull Long orgId,
                                  final @NonNull List<GraphAxis<Object>> graphAxes,
                                  final @NonNull Map<Long, CrmUser> crmUserMap,
                                  final @NonNull I18nResolver i18nResolver) {
        try {
            return getData(orgId, graphAxes, crmUserMap, i18nResolver);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch KPI data for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.KPI_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.kpi.data.fetch.action.failed");
        }
    }

    /**
     * Creates a {@link KPIRequest} by loading the KPI view, resolving column/table details,
     * building configurable function mappings, and assembling the request builder.
     *
     * @param analyticsRequest the analytics request details
     * @param actionType       the action type
     * @param requestData      the JSON request data
     * @param i18nResolver     the internationalization resolver
     * @return the fully-built {@link KPIRequest}
     */
    private KPIRequest createKPIRequest(final AnalyticsRequest analyticsRequest,
                                        final ActionType actionType,
                                        final JsonObject requestData,
                                        final I18nResolver i18nResolver,
                                        final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();
        final KPIView kpiView = getKPIView(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
        final Collection<ReportField> reportFields = kpiView.getReportFields();
        final Collection<FilterField> filterFields = kpiView.getFilterFields();
        final Set<Long> columnIds = chPathMappingService.getColumnIds(reportFields, filterFields);

        chPathMappingService.loadColumnRelationDetails(orgId, columnIds, kpiView.getReportPathMapping());

        final Collection<ColumnDetail> columnDetails = columnDetailsService.getAll(columnIds);
        final Map<Long, ColumnDetail> columnDetailsMap = buildColumnDetailsMap(columnDetails);
        final Set<Long> involvedTableIds = extractTableIds(columnDetails);
        final Collection<TableDetail> involvedTableDetails = tableDetailService.getAll(involvedTableIds);
        final Map<Long, TableDetail> tableDetailsMap = buildTableDetailsMap(involvedTableDetails);
        final Collection<RelationsInPath> relationsInPaths = kpiView.getReportPathMapping().getPathInfo().getRelationsInPaths();
        final Map<Long, TableRelation> tableRelationMap = (relationsInPaths != null && !relationsInPaths.isEmpty())
                ? chPathMappingService.getTableRelations(orgId, relationsInPaths) : new HashMap<>();
        final TableDetail rootTableDetail = chPathMappingService.getRootTableDetails(kpiView.getRootTableId(), involvedTableDetails);
        final Map<Long, ConfigurableFunctionDetail> configurableFunctionDetailsMap = new HashMap<>();
        final Map<String, Long> columnNameDetails = new HashMap<>();

        buildFunctionColumn(orgId, reportFields, configurableFunctionDetailsMap);
        final OperationType operationType = getOperationType(kpiView, configurableFunctionDetailsMap);

        if (ActionType.APPLY == actionType) {
            buildColumnNameDetails(orgId, reportFields, columnDetailsMap, columnNameDetails);
        }

        return KPIRequest.builder()
                .orgId(orgId)
                .userId(analyticsRequest.userId())
                .view(kpiView)
                .rootTableDetail(rootTableDetail)
                .tableDetailMap(tableDetailsMap)
                .tableRelationMap(tableRelationMap)
                .operationType(operationType)
                .configurableFunctionDetailsMap(configurableFunctionDetailsMap)
                .columnDetailsMap(columnDetailsMap)
                .columnNameDetails(columnNameDetails)
                .isApplyConfiguration(ActionType.APPLY == actionType)
                .viewId(kpiView.getViewId())
                .timezoneInfo(analyticsRequest.timezoneInfo())
                .columnDetailsPatternMap(columnDetailsPatternResolver.getColumnPatternMap(reportFields, filterFields))
                .build();
    }

    /**
     * Builds a map of column ID → {@link ColumnDetail} from the given collection.
     *
     * @param columnDetails the collection of column details
     * @return a pre-sized map keyed by column ID
     */
    private Map<Long, ColumnDetail> buildColumnDetailsMap(final Collection<ColumnDetail> columnDetails) {
        final Map<Long, ColumnDetail> map = new HashMap<>(columnDetails.size());

        for (final ColumnDetail cd : columnDetails) {
            map.put(cd.id(), cd);
        }

        return map;
    }

    /**
     * Extracts the unique table IDs from the given column details collection.
     *
     * @param columnDetails the collection of column details
     * @return a set of unique table IDs
     */
    private Set<Long> extractTableIds(final Collection<ColumnDetail> columnDetails) {
        final Set<Long> tableIds = new HashSet<>(columnDetails.size());

        for (final ColumnDetail cd : columnDetails) {
            tableIds.add(cd.getTableId());
        }

        return tableIds;
    }

    /**
     * Builds a map of table ID → {@link TableDetail} from the given collection.
     *
     * @param tableDetails the collection of table details
     * @return a pre-sized map keyed by table ID
     */
    private Map<Long, TableDetail> buildTableDetailsMap(final Collection<TableDetail> tableDetails) {
        final Map<Long, TableDetail> map = new HashMap<>(tableDetails.size());

        for (final TableDetail td : tableDetails) {
            map.put(td.id(), td);
        }

        return map;
    }

    /**
     * Builds column name details for APPLY action by mapping each report field to its
     * composite column name key and computed axis offset value.
     *
     * @param orgId             the organization ID
     * @param reportFields      the report fields to process
     * @param columnDetailsMap  the column details map for name resolution
     * @param columnNameDetails the map to populate with column name → axis offset
     */
    private void buildColumnNameDetails(final Long orgId,
                                        final Collection<ReportField> reportFields,
                                        final Map<Long, ColumnDetail> columnDetailsMap,
                                        final Map<String, Long> columnNameDetails) {
        try {
            CodeBlockExecutor.forEachSafely(reportFields,
                    reportField ->
                    {
                        final Long columnId = reportField.getColumnId();
                        final Integer axisType = reportField.getAxisType();
                        final ColumnDetail columnInfo = columnDetailsMap.get(columnId);
                        final String key = String.format(COLUMN_NAME_KEY_FORMAT, columnInfo.getColumnName(),
                                reportField.getOperationType().getTypeCode(), reportField.getFieldId(), null, null);
                        final long value = reportField.getAxis() == Axis.X_AXIS ? axisType + reportField.getOrder()
                                : axisType + Y_AXIS_OFFSET;

                        columnNameDetails.put(key, value);
                    });
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics: Failed to apply configuration for orgId: {}. Reason: {}", orgId, exception.getMessage(), exception);
            throw ErrorCode.get(AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.chart.data.fetch.action.failed");
        }
    }

    /**
     * Builds the configurable function details by querying the database for function
     * configurations matching the report field column IDs.
     *
     * @param orgId                          the organization ID
     * @param reportFields                   the report fields containing column IDs
     * @param configurableFunctionDetailsMap the map to populate with column ID → function details
     */
    private void buildFunctionColumn(final Long orgId,
                                     final Collection<ReportField> reportFields,
                                     final Map<Long, ConfigurableFunctionDetail> configurableFunctionDetailsMap) {
        final Set<Long> columnIds = new HashSet<>(reportFields.size());

        for (final ReportField reportField : reportFields) {
            columnIds.add(reportField.getColumnId());
        }

        if (columnIds.isEmpty()) {
            return;
        }

        final Table configurableFunctionTable = configManager.getConfigurableManager().getTable();
        final Table parentFunctionTable = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(configurableFunctionTable);

        selectQuery.addSelectClause(configurableFunctionTable, parentFunctionTable);
        selectQuery.addJoinClause(new JoinClause(configurableFunctionTable.getPKColumn(),
                parentFunctionTable.getColumn(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(
                configurableFunctionTable.getColumn(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID), columnIds)));

        final DataContainer dataContainer = DataStoreProvider.getSecureOrgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(configurableFunctionTable).forEach(row ->
                configurableFunctionDetailsMap.put(
                        row.get(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID),
                        buildConfigurationFunctionDetails(dataContainer, row, parentFunctionTable)));
    }

    /**
     * Builds a {@link ConfigurableFunctionDetail} from the given data row and its
     * parent function configuration row.
     *
     * @param dataContainer       the data container holding function-related details
     * @param row                 the configuration function details row
     * @param parentFunctionTable the parent function table reference (reused, not re-created)
     * @return the built {@link ConfigurableFunctionDetail}
     */
    private ConfigurableFunctionDetail buildConfigurationFunctionDetails(final DataContainer dataContainer,
                                                                         final Row row,
                                                                         final Table parentFunctionTable) {
        final Row parentConfigurationRow = dataContainer.getRow(parentFunctionTable, new WhereClause(Criteria.eq(
                        parentFunctionTable.getColumn(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID),
                        row.getPKValue())));

        final ParentConfigurableFunctionDetail parentDetails = ParentConfigurableFunctionDetail.builder()
                .columnFunctionId(parentConfigurationRow.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID))
                .parentColumnId(parentConfigurationRow.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.PARENT_COLUMN_ID))
                .build();

        return ConfigurableFunctionDetail.builder()
                .finalFunction(row.get(ANACONFIGURABLEFUNCTIONDETAILS.FINAL_FUNCTION))
                .columnId(row.get(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID))
                .functionTypeId(row.get(ANACONFIGURABLEFUNCTIONDETAILS.FUNCTION_TYPE_ID))
                .parentColumnFunctionDetails(List.of(parentDetails))
                .build();
    }

    /**
     * Determines the {@link OperationType} for the KPI view by checking if any report
     * field's column ID has a configurable function. Falls back to {@link OperationType#ACTUAL}.
     *
     * @param kpiView                        the KPI view
     * @param configurableFunctionDetailsMap the map of column ID → function details
     * @return the resolved operation type
     */
    private OperationType getOperationType(final KPIView kpiView,
                                           final Map<Long, ConfigurableFunctionDetail> configurableFunctionDetailsMap) {
        if (!configurableFunctionDetailsMap.isEmpty()) {
            final Collection<ReportField> reportFields = kpiView.getReportFields();

            for (final ReportField reportField : reportFields) {
                if (configurableFunctionDetailsMap.containsKey(reportField.getColumnId())) {
                    return reportField.getOperationType();
                }
            }
        }

        return OperationType.ACTUAL;
    }

    /**
     * Retrieves the {@link KPIView} entity either from the APPLY action's model config
     * or from the persisted child view configuration.
     *
     * @param analyticsRequest the analytics request details
     * @param actionType       the action type
     * @param requestData      the JSON request data
     * @param i18nResolver     the internationalization resolver
     * @return the {@link KPIView} details
     */
    private KPIView getKPIView(final AnalyticsRequest analyticsRequest,
                               final ActionType actionType,
                               final JsonObject requestData,
                               final I18nResolver i18nResolver,
                               final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();

        if (ActionType.APPLY == actionType) {
            final Collection<KPIView> kpiViews = crmReportsAPI.getKPIConfigAPI()
                    .getModelConfig(analyticsRequest, requestData, i18nResolver, reportFieldMap);

            return kpiViews.iterator().next();
        }
        final CRMChildViewDetails crmChildViewDetails = crmChildViewManager.get(orgId, requestData.getLong(CRMReportKeys.VIEW_ID));
        final Collection<CRMANAViewMapper> crmViewMappers = crmChildViewDetails.getCrmViewMapper();
        final Set<Long> anaViewIds = new HashSet<>(crmViewMappers.size());

        for (final CRMANAViewMapper mapper : crmViewMappers) {
            anaViewIds.add(mapper.getAnaViewId());
        }

        return analyticsConfigCache.kpiViewManager()
                .getAll(orgId, anaViewIds)
                .iterator()
                .next();
    }
}
