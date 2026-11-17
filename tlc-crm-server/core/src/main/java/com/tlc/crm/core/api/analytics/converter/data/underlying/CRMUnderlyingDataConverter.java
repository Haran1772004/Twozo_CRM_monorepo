package com.tlc.crm.core.api.analytics.converter.data.underlying;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.keys.data.UnderlyingDataKeys;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.FieldCategory;
import com.tlc.analytics.config.model.view.report.AnalyticsCustomFieldConfig;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.service.table.TableDetailService;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.Filter;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsSort;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsConfiguredField;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsLimit;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.UnderlyingDataRequest;
import com.tlc.analytics.core.api.path.AnalyticsPathFinderService;
import com.tlc.analytics.core.api.path.PathMappingService;
import com.tlc.analytics.sql.resource.ANAREPORT;

import com.tlc.analytics.sql.resource.ANATABLEDETAILS;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.TypeChecker;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;
import com.tlc.crm.core.api.analytics.converter.data.ColumnDetailsPatternResolver;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.lookup.CRMLookUp;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldColumnMappingService;
import com.tlc.crm.core.api.analytics.service.module.CRMCustomModularFieldColumnMappingService;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldService;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMDefaultFieldConfig;
import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMFieldConfig;
import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMUnderlyingData;
import com.tlc.crm.core.api.analytics.entity.data.underlying.MapperTableDetails;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.CRMReportsBuilder;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMMODULARFIELD;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELDCOLUMNMAPPING;
import com.tlc.crm.core.sql.resource.CRMREPORTDEFAULTFIELDCONFIG;
import com.tlc.crm.core.sql.resource.MCANAMAPPERTABLES;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.dml.Criteria;

import com.tlc.sql.common.api.dml.Table;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides an abstract way to manage the underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMReportsData
 */
class CRMUnderlyingDataConverter implements CRMReportsData {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMUnderlyingDataConverter.class);
    private static final Integer DEFAULT_START = 1;
    private static final Integer DEFAULT_LIMIT = 10;
    private static final Set<String> USER_COLUMNS = Set.of(CRMReportKeys.SALES_OWNER, CRMReportKeys.CREATED_BY,
            CRMReportKeys.UPDATED_BY, CRMReportKeys.ASSIGN_TO_USER,
            CRMReportKeys.CREATOR, CRMReportKeys.COLLABORATORS);
    private UnderlyingDataRequest underlyingDataRequest;
    private final ColumnDetailsService columnDetailsService;
    private final TableDetailService tableDetailService;
    private final CRMReportManagerAPI crmManager;
    private final CRMLookUp crmLookUp;
    private final PathMappingService pathMappingService;
    private final CRMReportsBuilder crmReportsBuilder;
    private final CRMUnderlyingDataResponseConverter crmUnderlyingDataResponseConverter;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final DisplayValueResolver displayValueResolver;
    private final ColumnDetailsPatternResolver columnDetailsPatternResolver;
    private final PathMappingService chPathMappingService;
    private final AnalyticsConfigManager<ChartView> chartLayout;
    private final AnalyticsConfigManager<KPIView> kpiLayout;
    private final CRMSystemModularFieldColumnMappingService systemColumnMappingService;
    private final CRMCustomModularFieldColumnMappingService customColumnMappingService;
    private final CRMSystemModularFieldService systemModularFieldService;
    private final AnalyticsFieldService analyticsFieldService;
    private final AnalyticsConfigManagerImpl analyticsConfigManager;

    private CRMUnderlyingDataConverter() {
        this.columnDetailsService = ColumnDetailsService.getInstance();
        this.tableDetailService = TableDetailService.getInstance();
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.crmLookUp = CRMLookUp.INSTANCE();
        this.pathMappingService = PathMappingService.getInstance();
        this.crmReportsBuilder = CRMReportsBuilder.INSTANCE();
        this.crmUnderlyingDataResponseConverter = CRMUnderlyingDataResponseConverter.INSTANCE();
        final AnalyticsServiceAPI analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.displayValueResolver = DisplayValueResolver.INSTANCE();
        this.columnDetailsPatternResolver = ColumnDetailsPatternResolver.INSTANCE();
        this.chPathMappingService = PathMappingService.getInstance();
        this.chartLayout = analyticsServiceAPI.getChartLayout();
        this.kpiLayout = analyticsServiceAPI.getKPILayout();
        this.systemColumnMappingService = CRMSystemModularFieldColumnMappingService.getInstance();
        this.customColumnMappingService = CRMCustomModularFieldColumnMappingService.getInstance();
        this.systemModularFieldService = CRMSystemModularFieldService.getInstance();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
        this.analyticsConfigManager = AnalyticsConfigManagerImpl.INSTANCE();
    }

    private static class Instance {
        private static final CRMUnderlyingDataConverter INSTANCE = new CRMUnderlyingDataConverter();
    }

    public static CRMUnderlyingDataConverter INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestJson      Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @return The list of {@link GraphAxis}
     * @deprecated Use {@link #getGraphAxis(AnalyticsRequest, ActionType, JsonObject, I18nResolver, Map)} instead
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @Override
    public @NonNull List<GraphAxis<Object>> getGraphAxis(final @NonNull AnalyticsRequest analyticsRequest,
                                                         final @NonNull ActionType actionType,
                                                         final @NonNull JsonObject requestJson,
                                                         final @NonNull I18nResolver i18nResolver,
                                                         final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();

        try {
            final CRMViewType reportType = CRMViewType.getCrmViewType(requestJson.getInt(CRMReportKeys.VIEW_TYPE_ID));
            final Long viewId = requestJson.getLong(CRMReportKeys.VIEW_ID);
            final CRMUnderlyingData crmUnderlyingData = constructCRMUnderlyingData(orgId, requestJson, analyticsRequest.crmResponse(), i18nResolver, null, null, reportFieldMap);

            LOGGER.info("CRM Analytics: Successfully created the CRM underlying request for orgId: {} and viewId: {}", orgId, viewId);
            this.underlyingDataRequest = createUnderlyingRequest(analyticsRequest, reportType, crmUnderlyingData, actionType, reportFieldMap);

            LOGGER.info("CRM Analytics: Successfully created the Analytics underlying request for orgId: {} and viewId: {}", orgId, viewId);

            return crmUnderlyingDataResponseConverter.getGraphAxisResponse(underlyingDataRequest);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch underlying data for orgId: {}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.UNDERLYING_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.underlying.data.fetch.action.failed");
        }
    }

    /**
     * Retrieves underlying graph axis data using explicit pagination values.
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestJson      Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param start            1-based start row index for retrieval
     * @param count            Number of rows to fetch
     * @return The list of {@link GraphAxis}
     * @deprecated Use {@link #getGraphAxis(AnalyticsRequest, ActionType, JsonObject, I18nResolver, int, int, Map)} instead
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @Override
    public @NonNull List<GraphAxis<Object>> getGraphAxis(final @NonNull AnalyticsRequest analyticsRequest, final @NonNull ActionType actionType,
                                                         final @NonNull JsonObject requestJson, final @NonNull I18nResolver i18nResolver,
                                                         final int start, final int count,
                                                         final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();

        try {
            final CRMViewType reportType = CRMViewType.getCrmViewType(requestJson.getInt(CRMReportKeys.VIEW_TYPE_ID));
            final Long viewId = requestJson.getLong(CRMReportKeys.VIEW_ID);
            final CRMUnderlyingData crmUnderlyingData = constructCRMUnderlyingData(orgId, requestJson,
                    analyticsRequest.crmResponse(), i18nResolver, start, count, reportFieldMap);
            this.underlyingDataRequest = createUnderlyingRequest(analyticsRequest, reportType, crmUnderlyingData, actionType, reportFieldMap);

            LOGGER.info("CRM Analytics: Successfully created the Analytics underlying request for orgId: {} and viewId: {}", orgId, viewId);

            return crmUnderlyingDataResponseConverter.getGraphAxisResponse(underlyingDataRequest);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch underlying data for orgId:{}. Reason:{}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.UNDERLYING_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.underlying.data.fetch.action.failed");
        }
    }

    /**
     * Builds the {@link UnderlyingDataRequest} once from the given request context, without applying any
     * pagination limit. This pre-built request can then be reused across multiple paginated calls
     * (see {@link #getGraphAxis(UnderlyingDataRequest, int, int)}) so that the expensive request-construction
     * work (field mappings, path resolution, etc.) is only performed once per export.
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestJson      Holds the {@link JsonObject} data
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @return A fully-constructed {@link UnderlyingDataRequest} with no pagination limit set
     * @deprecated Use {@link #buildUnderlyingRequest(AnalyticsRequest, ActionType, JsonObject, I18nResolver, Map)} instead
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @Override
    public @NonNull UnderlyingDataRequest buildUnderlyingRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                                                 final @NonNull ActionType actionType,
                                                                 final @NonNull JsonObject requestJson,
                                                                 final @NonNull I18nResolver i18nResolver,
                                                                 final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();

        try {
            final CRMViewType reportType = CRMViewType.getCrmViewType(requestJson.getInt(CRMReportKeys.VIEW_TYPE_ID));
            final Long viewId = requestJson.getLong(CRMReportKeys.VIEW_ID);
            final CRMUnderlyingData crmUnderlyingData = constructCRMUnderlyingData(orgId, requestJson, analyticsRequest.crmResponse(),
                    i18nResolver, null, null, reportFieldMap);

            LOGGER.info("CRM Analytics: Successfully created the CRM underlying request (export pre-build) for orgId: {} and viewId: {}", orgId, viewId);
            final UnderlyingDataRequest builtRequest = createUnderlyingRequest(analyticsRequest, reportType, crmUnderlyingData, actionType, reportFieldMap);
            this.underlyingDataRequest = builtRequest;

            LOGGER.info("CRM Analytics: Successfully created the Analytics underlying request (export pre-build) for orgId: {} and viewId: {}", orgId, viewId);

            return builtRequest;
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to pre-build underlying data request for orgId:{}. Reason:{}", orgId, error);
            throw ErrorCode.get(AnalyticsErrorCodes.UNDERLYING_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.underlying.data.fetch.action.failed");
        }
    }

    /**
     * Retrieves graph axis data using an already-built {@link UnderlyingDataRequest}, applying only the
     * pagination window ({@code start}/{@code count}) for this chunk. The request is mutated in-place
     * (only its {@code start}/{@code limit} fields change) so that no expensive reconstruction is needed
     * between export chunks.
     *
     * @param builtRequest The pre-built underlying data request (obtained from {@link #buildUnderlyingRequest})
     * @param start        1-based start row index for this chunk
     * @param count        Number of rows to fetch for this chunk
     * @return A list of {@link GraphAxis} objects for the requested chunk
     */
    @Override
    public @NonNull List<GraphAxis<Object>> getGraphAxis(final @NonNull UnderlyingDataRequest builtRequest,
                                                         final int start,
                                                         final int count) {
        try {
            builtRequest.setStart(start <= NumericalConstant.ZERO ? DEFAULT_START : start);
            builtRequest.setLimit(count);
            this.underlyingDataRequest = builtRequest;

            return crmUnderlyingDataResponseConverter.getGraphAxisResponse(builtRequest);
        } catch (final Exception error) {
            LOGGER.error("CRM Analytics: Failed to fetch underlying data chunk (start={}, count={}) for orgId:{}. Reason:{}",
                    start, count, builtRequest.getOrgId(), error);
            throw ErrorCode.get(AnalyticsErrorCodes.UNDERLYING_DATA_FETCH_ACTION_FAILED,
                    "i18n.analytics.underlying.data.fetch.action.failed");
        }
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

        LOGGER.info("CRM Analytics: [UNDERLYING-DEBUG] getResponse called - orgId: {}, graphAxes size: {}, crmUserMap size: {}", orgId, graphAxes.size(), crmUserMap.size());

        try {
            final Set<String> columnHeaders = new LinkedHashSet<>();
            final List<Object> values = new ArrayList<>();

            CodeBlockExecutor.forEachSafely(graphAxes,
                    graphAxis ->
                    {
                        final String columnName = graphAxis.getColumnName()
                                .toString();
                        final Long columnId = graphAxis.getColumnNameMap()
                                .get(columnName);
                        final Object columnValue = graphAxis.getValue();
                        final boolean isUserColumn = USER_COLUMNS.stream()
                                .anyMatch(col -> col.equalsIgnoreCase(columnName));

                        if (Objects.nonNull(columnValue) && !crmUserMap.isEmpty()
                                && isUserColumn) {
                            values.add(resolveUserColumnValue(columnValue, crmUserMap));
                        } else {
                            if (Objects.nonNull(columnValue)) {
                                values.add(displayValueResolver.resolveDisplayValue(orgId, columnValue, columnId, i18nResolver));
                            } else {
                                values.add(null);
                            }
                        }

                        columnHeaders.add(columnName);
                    });
            final List<String> headers = new ArrayList<>(columnHeaders);
            final int headerCount = headers.size();
            final JsonArray rowValues = Json.array();
            final int totalCount = values.size();
            final Collection<Object> pkValues = new HashSet<>();
            final Collection<AnalyticsConfiguredField> configuredFieldDetails = underlyingDataRequest.getView()
                    .getConfiguredFieldDetails();

            for (int index = 0; index < totalCount; index += headerCount) {
                final JsonObject valueJson = Json.object();
                boolean isDuplicate = false;

                for (int headerIndex = 0; headerIndex < headerCount && (index + headerIndex) < totalCount; headerIndex++) {
                    final String fieldName = headers.get(headerIndex);
                    final Object fieldValue = values.get(index + headerIndex);

                    if (CRMReportKeys.ID.equalsIgnoreCase(fieldName)) {

                        if (!pkValues.add(fieldValue)) {
                            isDuplicate = true;
                            break;
                        }
                    } else {
                        valueJson.put(getFieldId(fieldName, configuredFieldDetails).toString(), fieldValue);
                    }
                }

                if (!isDuplicate) {
                    rowValues.put(valueJson);
                }
            }

            if (graphAxes.isEmpty()) {
                dataJson.put(UnderlyingDataKeys.TOTAL_RECORDS, NumericalConstant.ZERO);
                dataJson.put(UnderlyingDataKeys.HAS_MORE, false);
            } else {
                final GraphAxis<Object> graphAxisDetails = graphAxes.get(NumericalConstant.ZERO);
                final Long totalRecords = graphAxisDetails.getTotalRecords();
                final AnalyticsLimit analyticsLimit = graphAxisDetails.getAnalyticsLimit();

                dataJson.put(UnderlyingDataKeys.TOTAL_RECORDS, totalRecords);
                dataJson.put(UnderlyingDataKeys.HAS_MORE, ((analyticsLimit.start() + analyticsLimit.limit()) - NumericalConstant.ONE) < totalRecords);
            }

            dataJson.put(UnderlyingDataKeys.ROWS, rowValues);
            dataJson.put(UnderlyingDataKeys.STRUCTURE, fetchStructure(configuredFieldDetails));

            LOGGER.info("CRM Analytics: [UNDERLYING-DEBUG] response built - orgId: {}, columnHeaders: {}, totalValues: {}, rowCount: {}, totalRecords: {}",
                    orgId, columnHeaders, values.size(), rowValues.size(), dataJson.opt("total_records"));
        } catch (final Exception exception) {
            throw ErrorCode.get(ConfigErrorCodes.UNDERLYING_DATA_RESPONSE_FAILED,
                    "i18n.analytics.underlying.data.response.failed");
        }

        return dataJson;
    }

    /**
     * Resolves a user column value to the user's full name.
     *
     * <p>Handles both single user ID values ({@link Long}) and array values
     * (e.g., Collaborators field which stores multiple user IDs as an array).
     * For arrays, each user ID is resolved and the names are joined with commas.</p>
     *
     * @param columnValue the raw column value (could be Long, array, collection, or String like "[1,2]")
     * @param crmUserMap  map of user ID to {@link CrmUser}
     * @return the resolved display value (user name or comma-separated names),
     * or the original value as string if resolution fails
     */
    private String resolveUserColumnValue(final Object columnValue,
                                          final Map<Long, CrmUser> crmUserMap) {
        if (columnValue instanceof final Number number) {
            final Long id = number.longValue();
            final CrmUser crmUser = crmUserMap.get(id);

            return Objects.isNull(crmUser) ? columnValue.toString() : crmUser.getFullName();
        }

        final List<Long> userIds = extractUserIds(columnValue);

        if (userIds.isEmpty()) {
            return null;
        }

        final StringBuilder names = new StringBuilder();

        for (int i = 0; i < userIds.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }

            final Long userId = userIds.get(i);
            final CrmUser crmUser = crmUserMap.get(userId);

            names.append(Objects.isNull(crmUser) ? String.valueOf(userId) : crmUser.getFullName());
        }

        return names.toString();
    }

    /**
     * Extracts user IDs from a value that may be a Java array, collection,
     * or string representation of an array (e.g. {@code "[123, 456]"}).
     *
     * @param value the value to extract IDs from
     * @return a list of parsed user IDs, empty if the value is empty or unparseable
     */
    private List<Long> extractUserIds(final Object value) {
        final List<Long> ids = new ArrayList<>();

        if (value instanceof Collection<?> collection) {
            for (final Object element : collection) {
                parseLongSafely(element, ids);
            }
        } else if (value.getClass().isArray()) {
            final int length = java.lang.reflect.Array.getLength(value);

            for (int i = 0; i < length; i++) {
                parseLongSafely(java.lang.reflect.Array.get(value, i), ids);
            }
        } else if (value instanceof String str) {
            final String trimmed = str.trim();

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                final String inner = trimmed.substring(1, trimmed.length() - 1).trim();

                if (!inner.isEmpty()) {
                    for (final String element : inner.split(",")) {
                        parseLongSafely(element.trim(), ids);
                    }
                }
            } else {
                parseLongSafely(str, ids);
            }
        }

        return ids;
    }

    /**
     * Safely parses a value to Long and adds it to the list.
     *
     * @param value the value to parse
     * @param ids   the list to add the parsed Long to
     */
    private void parseLongSafely(final Object value, final List<Long> ids) {
        if (Objects.isNull(value)) {
            return;
        }
        final Number number = NumberUtils.parseNumber(value);

        if (Objects.nonNull(number)) {
            ids.add(number.longValue());
        }
    }

    /**
     * Retrieves the ID of the field that matches the given field name.
     *
     * @param fieldName                 The name of the field to search for
     * @param analyticsConfiguredFields It holds the collection of {@link AnalyticsConfiguredField} details
     * @return the ID of the matching field, or null if no match is found
     */
    private Long getFieldId(final String fieldName, final Collection<AnalyticsConfiguredField> analyticsConfiguredFields) {
        return analyticsConfiguredFields.stream()
                .filter(field -> fieldName.equals(field.fieldName()))
                .map(AnalyticsConfiguredField::id)
                .findFirst()
                .orElse(null);
    }

    /**
     * <p>
     * Fetches the structure based on the configured fields
     * </p>
     *
     * @param analyticsConfiguredFields It holds the collection of {@link AnalyticsConfiguredField} details
     * @return {@link JsonObject} of structure
     */
    private JsonObject fetchStructure(final Collection<AnalyticsConfiguredField> analyticsConfiguredFields) {
        final JsonObject structureJson = Json.object();
        final JsonArray fieldsArray = Json.array();

        try {
            CodeBlockExecutor.forEachSafely(analyticsConfiguredFields,
                    analyticsConfiguredField -> fieldsArray.put(Json.object().put(UnderlyingDataKeys.ID, analyticsConfiguredField.id())
                            .put(UnderlyingDataKeys.DISPLAY_NAME, analyticsConfiguredField.fieldName())
                            .put(UnderlyingDataKeys.HIDDEN, analyticsConfiguredField.hidden())
                            .put(UnderlyingDataKeys.IS_SORTABLE, analyticsConfiguredField.isSortable())
                            .put(UnderlyingDataKeys.FIELD_TYPE, analyticsConfiguredField.storeType()
                                    .getName())
                            .put(UnderlyingDataKeys.NAME, analyticsConfiguredField.id()
                                    .toString())
                            .put(UnderlyingDataKeys.IS_MANDATORY_FIELD, analyticsConfiguredField.isMandatory())));

            structureJson.put(UnderlyingDataKeys.COLUMNS, fieldsArray);

            return structureJson;
        } catch (final Exception exception) {
            throw ErrorCode.get(ConfigErrorCodes.UNDERLYING_DATA_RESPONSE_FAILED,
                    "i18n.analytics.underlying.data.response.failed");
        }
    }

    /**
     * <p>
     * Creates an underlying data request
     * </p>
     *
     * @param analyticsRequest  Represents the {@link AnalyticsRequest} details
     * @param crmUnderlyingData Represents the {@link CRMUnderlyingData } which holds the underlying data details
     * @return UnderlyingDataRequest  The constructed request containing necessary details
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @NonNull
    private UnderlyingDataRequest createUnderlyingRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                                           final @NonNull CRMViewType reportType,
                                                           final @NonNull CRMUnderlyingData crmUnderlyingData,
                                                           final @NonNull ActionType actionType,
                                                            final @NonNull Map<Long, ReportModularField> reportFieldMap) throws Exception {
        final long orgId = analyticsRequest.orgId();
        final Long viewId = crmUnderlyingData.getViewId();
        final String rootTableName = crmLookUp.getRootTable(crmUnderlyingData.getModuleId().intValue()).toLowerCase();
        final Long rootTableId = AnalyticsConfigManagerImpl.INSTANCE()
                .getTableManager()
                .getPKValue(orgId,
                        Criteria.eq(AnalyticsConfigManagerImpl.INSTANCE()
                                        .getTableManager()
                                        .getTable()
                                        .getColumn(ANATABLEDETAILS.TABLE_NAME),
                                rootTableName));
        final Set<Long> configuredFieldIds = new HashSet<>(crmUnderlyingData.getConfiguredFields());
        final Collection<AnalyticsConfiguredField> analyticsConfiguredFields = crmUnderlyingData.getConfiguredFieldDetails();
        final Collection<ReportModularField> reportModularFields = reportFieldMap.values().stream()
                .filter(field -> configuredFieldIds.contains(field.id())).toList();

        final Set<Long> systemModularFieldIds = new HashSet<>();
        for (final ReportModularField field : reportModularFields) {
            if (!field.isCustom()) {
                systemModularFieldIds.add(field.getSystemFieldId());
            }
        }
        final Map<Long, CRMSystemModularField> systemModularFieldMap = systemModularFieldService.getAll(systemModularFieldIds);
        final Set<Long> allCrmFieldIds = new HashSet<>();

        for (final ReportModularField field : reportModularFields) {
            if (field.isCustom()) {
                final CRMCustomModularField customField = field.getCustomModularField();
                if (Objects.nonNull(customField) && Objects.nonNull(customField.getCrmFieldId())) {
                    allCrmFieldIds.add(customField.getCrmFieldId());
                }
            }
        }

        for (final CRMSystemModularField systemField : systemModularFieldMap.values()) {

            if (Objects.nonNull(systemField) && Objects.nonNull(systemField.getCrmFieldId())) {
                allCrmFieldIds.add(systemField.getCrmFieldId());
            }
        }
        final Map<Long, FieldInfo> fieldInfoMap = allCrmFieldIds.isEmpty() ? Map.of() : fieldService.getFieldsById(orgId, allCrmFieldIds);
        final Map<String, Pair<String, Long>> configuredColumnDetails = new HashMap<>();
        final boolean isExport = actionType.equals(ActionType.INIT_EXPORT);
        final Collection<ReportField> reportFields = new ArrayList<>();
        final Collection<FilterField> filterFields = new ArrayList<>();
        final Set<Long> columnIds = new HashSet<>();
        final KpiDurationContext kpiDurationContext = loadCriteriaFields(orgId, crmUnderlyingData.getChildViewId(), reportType, columnIds, filterFields, reportFieldMap);
        Long actualColumnId;
        Long order = 0L;

        for (ReportModularField reportModularField : reportModularFields) {
            if (reportModularField.isCustom()) {
                final CRMCustomModularField customModularField = reportModularField.getCustomModularField();
                final FieldInfo fieldInfo = fieldInfoMap.get(customModularField.getCrmFieldId());
                final StoreType storeType = fieldInfo.storeType();
                final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(storeType);
                final Long fieldTypeId = fieldType.id();
                final Collection<CRMCustomModularFieldColumnMapping> customModularFieldColumnMappings = customColumnMappingService.getCustomColumnMappings(customModularField.getModuleId(), fieldTypeId);
                final String customFieldConfig = new AnalyticsCustomFieldConfig(fieldInfo.columnIndex()).toJsonAsString();

                for (final CRMCustomModularFieldColumnMapping customModularFieldColumnMapping : customModularFieldColumnMappings) {
                    columnIds.add(customModularFieldColumnMapping.getColumnId());

                    final ColumnDetail columnInfo = columnDetailsService.get(customModularFieldColumnMapping.getColumnId());

                    assert columnInfo != null;
                    actualColumnId = customModularFieldColumnMapping.getColumnId();

                    final ReportField reportField = crmReportsBuilder.getReportField(orgId, actualColumnId, System.currentTimeMillis(), System.currentTimeMillis(), order,
                            OperationType.ACTUAL.getTypeCode(), Axis.X_AXIS.getType(), FieldCategory.CATEGORICAL.getTypeCode(),
                            "", fieldInfo.storeType().getType(), null, null, null, reportModularField.id(),
                            null, null, null, null, null,
                            fieldInfo.aliasName(), customFieldConfig, false);

                    if (configuredFieldIds.contains(reportModularField.id())) {
                        for (final AnalyticsConfiguredField analyticsConfiguredField : analyticsConfiguredFields) {
                            if (Objects.equals(reportModularField.id(), analyticsConfiguredField.id())) {
                                final String selectColumnName = String.format("%s%d", columnInfo.getColumnName(), reportModularField.id());

                                configuredColumnDetails.put(selectColumnName, Pair.of(analyticsConfiguredField.fieldName(), columnInfo.id()));
                            }
                        }

                        configuredColumnDetails.put(CRMMODULARFIELD.ID, Pair.of(CRMReportKeys.ID, null));
                    }

                    reportFields.add(reportField);
                }
            } else {
                final Collection<CRMSystemModularFieldColumnMapping> systemColumnMappings = systemColumnMappingService
                        .getSystemColumnMappings(reportModularField.getSystemFieldId());

                for (final CRMSystemModularFieldColumnMapping fieldColumnMapping : systemColumnMappings) {
                    final Long currentTime = System.currentTimeMillis();
                    order = order + 1;

                    Stream.of(fieldColumnMapping.getFilterFieldId(),
                                    fieldColumnMapping.getJoinFieldId(),
                                    fieldColumnMapping.getGroupByFieldId(),
                                    fieldColumnMapping.getSelectorFieldId())
                            .filter(Objects::nonNull)
                            .forEach(columnIds::add);

                    switch (fieldColumnMapping.getFieldCategory()) {
                        case MAPPER_FIELD, PRIMARY_FIELD, FILTER_FIELD -> {
                            if (fieldColumnMapping.getFilterFieldId() != null) {
                                filterFields.add(buildFilterFields(orgId, currentTime, order, fieldColumnMapping, reportModularField, systemModularFieldMap, fieldInfoMap));
                            }
                        }
                        case ACTUAL_FIELD -> {
                            if (fieldColumnMapping.getFilterFieldId() != null) {
                                filterFields.add(buildFilterFields(orgId, currentTime, order, fieldColumnMapping, reportModularField, systemModularFieldMap, fieldInfoMap));
                            }

                            if (fieldColumnMapping.getSelectorFieldId() != null) {
                                final Long modularFieldId = fieldColumnMapping.getModularFieldId();
                                final CRMSystemModularField modularField = systemModularFieldMap.get(modularFieldId);
                                final Long crmFieldId = modularField.getCrmFieldId();
                                final boolean isAnalyticsField = Objects.isNull(crmFieldId);
                                final AnalyticsField analyticsField = isAnalyticsField ? analyticsFieldService.getAnalyticsFieldById(modularField.getAnaFieldId()) : null;
                                final FieldInfo crmFieldInfo = isAnalyticsField ? null : fieldInfoMap.get(crmFieldId);
                                final ColumnDetail columnInfo = columnDetailsService.get(fieldColumnMapping.getSelectorFieldId());
                                assert columnInfo != null;
                                actualColumnId = fieldColumnMapping.getSelectorFieldId();

                                final ReportField reportField = crmReportsBuilder.getReportField(orgId, actualColumnId, currentTime, currentTime, order,
                                        Objects.isNull(fieldColumnMapping.getOperationTypeId()) ? OperationType.ACTUAL.getTypeCode()
                                                : fieldColumnMapping.getOperationTypeId(), Axis.X_AXIS.getType(), FieldCategory.CATEGORICAL.getTypeCode(),
                                        "", isAnalyticsField ? fieldTypeService.get(analyticsField.getFieldTypeId()).storeType().getType()
                                                : crmFieldInfo.storeType().getType(),
                                        null, null, fieldColumnMapping.getGroupByFieldId(), reportModularField.id(),
                                        null, null, null, null, fieldColumnMapping.getCaseWhenConfig(),
                                        isAnalyticsField ? null : crmFieldInfo.aliasName(), null, false);

                                if (configuredFieldIds.contains(reportModularField.id())) {
                                    for (final AnalyticsConfiguredField analyticsConfiguredField : analyticsConfiguredFields) {
                                        if (Objects.equals(reportModularField.id(), analyticsConfiguredField.id())) {
                                            final String selectColumnName = String.format("%s%d", columnInfo.getColumnName(), reportModularField.id());

                                            configuredColumnDetails.put(selectColumnName, Pair.of(analyticsConfiguredField.fieldName(), columnInfo.id()));
                                        }
                                    }

                                    configuredColumnDetails.put(CRMMODULARFIELD.ID, Pair.of(CRMReportKeys.ID, null));
                                }

                                reportFields.add(reportField);
                            }
                        }
                    }
                }
            }
        }

        final ReportPathMapping reportPathMapping = crmLookUp.getReportPathMapping(orgId, rootTableId, columnIds);
        final Function<Set<Long>, Collection<TableDetail>> tableDetailsRetriever = (ids) -> tableDetailService.getAll(ids);
        final Collection<ColumnDetail> columnDetails = new ArrayList<>();
        final Map<Long, ColumnDetail> columnDetailsMap = new HashMap<>();
        final Set<Long> involvedTables = new HashSet<>();
        final List<TableDetail> involvedTableDetails = new ArrayList<>();

        final SafeConsumer<Collection<Long>> columnDetailsHandler = (columnId) ->
        {
            columnDetails.addAll(columnDetailsService.getAll(columnIds));
            columnDetailsMap.putAll(columnDetails.stream().collect(Collectors.toMap(ColumnDetail::id, Function.identity(),
                    (existingColumnDetails, currentColumnDetails) -> currentColumnDetails)));
        };

        final SafeConsumer<Collection<ColumnDetail>> tableDetailsHandler = (columnInfos) ->
        {
            involvedTables.addAll(columnInfos.stream().map(columnDetail -> columnDetail.getTableDetail().id())
                    .collect(Collectors.toSet()));

            involvedTableDetails.addAll(new ArrayList<>(tableDetailService.getAll(involvedTables)));
        };
        try {
            columnDetailsHandler.accept(columnIds);
            tableDetailsHandler.accept(columnDetails);
            final Map<String, Long> columnNameDetails = columnDetails.stream().collect(Collectors.toMap(ColumnDetail::getColumnName,
                    ColumnDetail::id, (existingId, currentId) -> currentId));
            final TableDetail rootTableDetail = tableDetailService.get(rootTableId);
            final List<RelationsInPath> relationsInPaths = AnalyticsPathFinderService.getInstance().findAndGetPath(rootTableDetail,
                    tableDetailsRetriever, involvedTableDetails);
            LOGGER.info("CRM Analytics: Successfully retrieved the shortest path for CRM underlying request orgId: {}, viewId: {}", orgId, viewId);

            pathMappingService.loadColumnRelationDetails(orgId, columnIds, reportPathMapping);
            columnDetailsHandler.accept(columnIds);
            tableDetailsHandler.accept(columnDetails);
            final Map<Long, TableDetail> tableDetailsMap = involvedTableDetails.stream().collect(Collectors.toMap(TableDetail::id,
                    Function.identity(), (existingTableDetails, currentTableDetails) -> currentTableDetails));
            final Map<Long, TableRelation> tableRelationMap = (relationsInPaths != null) && !relationsInPaths.isEmpty()
                    ? chPathMappingService.getTableRelations(orgId, relationsInPaths) : new HashMap<>();
            final AnalyticsLimit analyticsLimit = crmUnderlyingData.getLimit();
            final AnalyticsSort analyticsSort = crmUnderlyingData.getAnalyticsSort();
            Integer start = null;
            Integer limit = null;
            ReportField reportField = null;

            if (Objects.nonNull(analyticsLimit)) {
                start = analyticsLimit.start();
                limit = analyticsLimit.limit();
            }

            if (Objects.nonNull(analyticsSort)) {
                try {
                    final Long fieldId = analyticsSort.fieldId();
                    final ReportModularField reportModularField = reportFieldMap.get(fieldId);

                    if (reportModularField.isCustom()) {
                        final CRMCustomModularField customModularField = reportModularField.getCustomModularField();
                        final FieldInfo fieldInfo = fieldInfoMap.getOrDefault(customModularField.getCrmFieldId(),
                                fieldService.getField(orgId, customModularField.getCrmFieldId()));
                        if (!fieldInfoMap.containsKey(customModularField.getCrmFieldId())) {
                            LOGGER.warn("CRM Analytics: Cache miss for fieldInfoMap - orgId: {}, crmFieldId: {}", orgId, customModularField.getCrmFieldId());
                        }
                        final StoreType storeType = fieldInfo.storeType();
                        final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(storeType);
                        final Long fieldTypeId = fieldType.id();
                        final Collection<CRMCustomModularFieldColumnMapping> customModularFieldColumnMappings = customColumnMappingService.getCustomColumnMappings(customModularField.getModuleId(), fieldTypeId);
                        final String customFieldConfig = new AnalyticsCustomFieldConfig(fieldInfo.columnIndex()).toJsonAsString();

                        for (CRMCustomModularFieldColumnMapping customModularFieldColumnMapping : customModularFieldColumnMappings) {
                            final ColumnDetail columnInfo = columnDetailsService.get(customModularFieldColumnMapping.getColumnId());
                            assert columnInfo != null;
                            reportField = crmReportsBuilder.getReportField(orgId, customModularFieldColumnMapping.getColumnId(), System.currentTimeMillis(), System.currentTimeMillis(), order,
                                    OperationType.ACTUAL.getTypeCode(), Axis.X_AXIS.getType(), FieldCategory.CATEGORICAL.getTypeCode(),
                                    "", fieldInfo.storeType().getType(), null, null, null, reportModularField.id(),
                                    null, null, null, null, null,
                                    fieldInfo.aliasName(), customFieldConfig, false);
                        }
                    } else {
                        final CRMSystemModularFieldColumnMapping fieldColumnMapping = crmManager.getSystemFieldColumnMapping().getAll(orgId,
                                        Criteria.eq(crmManager.getSystemFieldColumnMapping().getTable().getColumn(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.MODULAR_FIELD_ID),
                                                reportModularField.getModularField().id())).stream()
                                .filter(mapping -> com.tlc.crm.core.api.analytics.entity.integ.reports.FieldCategory.ACTUAL_FIELD == mapping.getFieldCategory() && Objects.nonNull(mapping.getSelectorFieldId())).findFirst()
                                .orElseThrow(() -> ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD,
                                        "i18n.analytics.config.invalid.field"));
                        final Long columnId = fieldColumnMapping.getSelectorFieldId();
                        final CRMSystemModularField crmSystemModularField = systemModularFieldService.get(fieldColumnMapping.getModularFieldId());
                        final Long crmFieldId = crmSystemModularField.getCrmFieldId();
                        final boolean isAnalyticsField = Objects.isNull(crmFieldId);
                        final AnalyticsField analyticsField = isAnalyticsField ? analyticsFieldService.getAnalyticsFieldById(crmSystemModularField.getAnaFieldId()) : null;
                        final FieldInfo crmFieldInfo = isAnalyticsField ? null : fieldInfoMap.getOrDefault(crmFieldId,
                                fieldService.getField(orgId, crmFieldId));
                        if (!isAnalyticsField && !fieldInfoMap.containsKey(crmFieldId)) {
                            LOGGER.warn("CRM Analytics: Cache miss for fieldInfoMap - orgId: {}, crmFieldId: {}", orgId, crmFieldId);
                        }
                        final ColumnDetail sortColumnDetail = columnDetailsService.get(columnId);

                        if (Objects.nonNull(sortColumnDetail)) {
                            columnDetailsMap.put(sortColumnDetail.id(), sortColumnDetail);

                        }
                        final Long currentTime = Instant.now().toEpochMilli();

                        reportField = crmReportsBuilder.getReportField(orgId, columnId, currentTime, currentTime, order,
                                OperationType.ACTUAL.getTypeCode(), Axis.X_AXIS.getType(), FieldCategory.CATEGORICAL.getTypeCode(),
                                "", isAnalyticsField ? fieldTypeService.get(analyticsField.id()).storeType().getType()
                                        : crmFieldInfo.storeType().getType(), null, null, fieldColumnMapping.getGroupByFieldId(),
                                reportModularField.id(), null, null, null, null,
                                fieldColumnMapping.getConfigAsString(), Objects.nonNull(crmFieldId) ? crmFieldInfo.aliasName() : null, null, false);
                    }
                } catch (Exception exception) {
                    LOGGER.error("CRM Analytics: Failed to process sort field for orgId: {}, viewId: {}", orgId, viewId, exception);
                    throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                            "i18n.analytics.config.invalid.module");
                }

            }

            final CRMUnderlyingData underlyingData = CRMUnderlyingData.builder()
                    .orgId(orgId)
                    .reportPathMapping(reportPathMapping)
                    .filterFields(filterFields)
                    .reportFields(reportFields)
                    .limit(crmUnderlyingData.getLimit())
                    .configuredFieldDetails(crmUnderlyingData.getConfiguredFieldDetails())
                    .configuredFields(crmUnderlyingData.getConfiguredFields())
                    .build();

            return UnderlyingDataRequest.builder()
                    .orgId(orgId)
                    .userId(analyticsRequest.userId())
                    .view(underlyingData)
                    .moduleId(crmUnderlyingData.getModuleId())
                    .rootTableDetail(rootTableDetail)
                    .tableDetailMap(tableDetailsMap)
                    .tableRelationMap(tableRelationMap)
                    .columnDetailsMap(columnDetailsMap)
                    .moduleId(crmUnderlyingData.getModuleId())
                    .columnNameDetails(columnNameDetails)
                    .configuredColumnDetails(configuredColumnDetails)
                    .isExport(isExport)
                    .timezoneInfo(analyticsRequest.timezoneInfo())
                    .start(start)
                    .limit(limit)
                    .reportFieldSort(Pair.of(reportField, analyticsSort))
                    .isUnderlyingRequest(true)
                    .columnDetailsPatternMap(columnDetailsPatternResolver.getColumnPatternMap(reportFields, filterFields))
                    .operationType(kpiDurationContext.operationType())
                    .durationColumnId(kpiDurationContext.durationColumnId())
                    .durationFrom(kpiDurationContext.from())
                    .durationTo(kpiDurationContext.to())
                    .build();
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics: Failed to create CRM underlying request for orgId: {}, viewId: {}", orgId, viewId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_VIEW_ID,
                    "i18n.analytics.config.view.invalid.id");
        }
    }

    /**
     * Holds the KPI duration context extracted from the parent KPI view.
     * Used to apply the KPI's time period filter on underlying data queries.
     *
     * @param operationType    the KPI's duration operation type (TODAY, THIS_WEEK, etc.), or {@code null} if not a KPI
     * @param durationColumnId the column ID of the KPI's X_AXIS (duration) field, or {@code null} if not a KPI
     * @param from             the custom duration start time (epoch millis), only for {@code OperationType.CUSTOM}
     * @param to               the custom duration end time (epoch millis), only for {@code OperationType.CUSTOM}
     */
    private record KpiDurationContext(OperationType operationType, Long durationColumnId, Long from, Long to) {
        static final KpiDurationContext EMPTY = new KpiDurationContext(null, null, null, null);
    }

    /**
     * Loads criteria fields and column IDs based on the given {@link CRMViewType}.
     * Additionally, for KPI views, extracts the duration context (operationType, duration column ID,
     * and from/to for CUSTOM duration) so that underlying data can be filtered by the KPI's time period.
     *
     * @param orgId        Represents the organization ID
     * @param childViewId  Represents the child view ID
     * @param reportType   It holds the {@link CRMViewType} details
     * @param columnIds    The collection to store extracted column IDs
     * @param filterFields The collection to store extracted filter fields.
     * @return the KPI duration context if the parent view is a KPI, otherwise {@link KpiDurationContext#EMPTY}
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    private KpiDurationContext loadCriteriaFields(final Long orgId, final Long childViewId, final CRMViewType reportType,
                                                  final Collection<Long> columnIds, final Collection<FilterField> filterFields, final Map<Long, ReportModularField> reportFieldMap) {
        final AnalyticsReport analyticsReport;
        KpiDurationContext kpiDurationContext = KpiDurationContext.EMPTY;

        if (CRMViewType.KPI == reportType) {
            final KPIView kpiView = kpiLayout.getAll(orgId, Criteria.eq(kpiLayout.getTable().getColumn(ANAREPORT.VIEW_ID), childViewId))
                    .stream().findFirst().orElseThrow(() -> ErrorCode.get(AnalyticsErrorCodes.ANALYTICS_REPORT_NOT_FOUND));
            analyticsReport = kpiView;

            kpiDurationContext = extractKpiDurationContext(kpiView);

            if (kpiDurationContext.durationColumnId() != null) {
                columnIds.add(kpiDurationContext.durationColumnId());
            }
        } else {
            analyticsReport = chartLayout.getAll(orgId, Criteria.eq(chartLayout.getTable().getColumn(ANAREPORT.VIEW_ID), childViewId))
                    .stream().findFirst().orElseThrow(() -> ErrorCode.get(AnalyticsErrorCodes.ANALYTICS_REPORT_NOT_FOUND));
        }

        final Collection<FilterField> anaFilterFields = analyticsReport.getFilterFields();

        if (Objects.nonNull(anaFilterFields)) {
            final Collection<FilterField> criteriaFilterFields = anaFilterFields.stream().filter(FilterField::isCustomFilter)
                    .collect(Collectors.toUnmodifiableSet());

            filterFields.addAll(criteriaFilterFields);

            if (!criteriaFilterFields.isEmpty()) {
                loadColumnIds(orgId, columnIds, criteriaFilterFields, reportFieldMap);
            }
        }

        return kpiDurationContext;
    }

    /**
     * Extracts the KPI duration context from a KPI view.
     * <p>
     * The duration is determined by:
     * <ul>
     *   <li>The {@code operationType} from the KPI's comparators (LEFT/compare-to side)</li>
     *   <li>The duration column from the X_AXIS report field's column ID</li>
     *   <li>For CUSTOM duration, the from/to values from the comparator</li>
     * </ul>
     *
     * @param kpiView the KPI view to extract duration context from
     * @return the extracted duration context, or {@link KpiDurationContext#EMPTY} if no duration info is found
     */
    private KpiDurationContext extractKpiDurationContext(final KPIView kpiView) {
        Long durationColumnId = null;

        if (kpiView.getReportFields() != null) {
            for (final ReportField reportField : kpiView.getReportFields()) {
                if (Axis.X_AXIS == reportField.getAxis()) {
                    durationColumnId = reportField.getColumnId();
                    break;
                }
            }
        }

        if (durationColumnId == null) {
            return KpiDurationContext.EMPTY;
        }

        // Find the LEFT (compare-to) comparator to get the operationType and from/to
        if (kpiView.getComparators() != null) {
            for (final KPIComparator comparator : kpiView.getComparators()) {
                if (KPIComparatorSide.LEFT == comparator.getComparatorSide()) {
                    final OperationType operationType = comparator.getOperationType();

                    if (OperationType.getKPITimeUnits().contains(operationType)) {
                        return new KpiDurationContext(operationType, durationColumnId,
                                comparator.getFrom(), comparator.getTo());
                    }
                }
            }
        }

        return KpiDurationContext.EMPTY;
    }

    /**
     * <p>
     * Loads the collection of column IDs with the actual column IDs extracted from the provided {@link FilterField}
     * </p>
     *
     * @param columnIds            Represents the collection of column IDs
     * @param criteriaFilterFields Represents the collection of {@link FilterField} details
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    private void loadColumnIds(final Long orgId, final Collection<Long> columnIds, final Collection<FilterField> criteriaFilterFields, final Map<Long, ReportModularField> reportFieldMap) {
        final Collection<Long> fieldIds = criteriaFilterFields.stream()
                .map(BaseField::getFieldId)
                .collect(Collectors.toUnmodifiableSet());

        try {
            final Collection<ReportModularField> reportModularFields = reportFieldMap.values().stream()
                    .filter(field -> fieldIds.contains(field.id())).toList();

            // Batch pre-fetch system modular fields only
            final Set<Long> systemModularFieldIds = new HashSet<>();
            for (final ReportModularField field : reportModularFields) {
                if (!field.isCustom()) {
                    systemModularFieldIds.add(field.getSystemFieldId());
                }
            }
            final Map<Long, CRMSystemModularField> systemModularFieldMap = systemModularFieldService.getAll(systemModularFieldIds);
            final Set<Long> allCrmFieldIds = new HashSet<>();

            for (final ReportModularField field : reportModularFields) {
                if (field.isCustom()) {
                    final CRMCustomModularField customField = field.getCustomModularField();
                    if (Objects.nonNull(customField) && Objects.nonNull(customField.getCrmFieldId())) {
                        allCrmFieldIds.add(customField.getCrmFieldId());
                    }
                }
            }

            final Map<Long, FieldInfo> fieldInfoMap = allCrmFieldIds.isEmpty() ? Map.of() : fieldService.getFieldsById(orgId, allCrmFieldIds);

            final Collection<ModularField> systemModularFields = new ArrayList<>();

            for (ReportModularField reportModularField : reportModularFields) {
                if (reportModularField.isCustom()) {
                    final CRMCustomModularField customModularField = reportModularField.getCustomModularField();
                    final FieldInfo fieldInfo = fieldInfoMap.get(customModularField.getCrmFieldId());
                    final StoreType storeType = fieldInfo.storeType();
                    final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(storeType);
                    final Long fieldTypeId = fieldType.id();
                    final Collection<CRMCustomModularFieldColumnMapping> customModularFieldColumnMappings = customColumnMappingService.getCustomColumnMappings(customModularField.getModuleId(), fieldTypeId);

                    for (final CRMCustomModularFieldColumnMapping customModularFieldColumnMapping : customModularFieldColumnMappings) {
                        columnIds.add(customModularFieldColumnMapping.getColumnId());
                    }
                } else {
                    systemModularFields.add(systemModularFieldMap.get(reportModularField.getSystemFieldId()));
                }
            }

            for (final ModularField systemModularField : systemModularFields) {
                final Collection<CRMSystemModularFieldColumnMapping> systemColumnMappings = systemColumnMappingService
                        .getSystemColumnMappings(systemModularField.id());

                for (final CRMSystemModularFieldColumnMapping fieldColumnMapping : systemColumnMappings) {
                    addIfNonNull(columnIds, fieldColumnMapping.getFilterFieldId());
                    addIfNonNull(columnIds, fieldColumnMapping.getJoinFieldId());
                    addIfNonNull(columnIds, fieldColumnMapping.getGroupByFieldId());
                    addIfNonNull(columnIds, fieldColumnMapping.getSelectorFieldId());
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(AnalyticsErrorCodes.FIELD_COLUMN_MAPPING_FETCH_FAILED,
                    "i18n.analytics.config.field.column.mapping.fetch.failed");
        }
    }

    /**
     * Adds the given value to the target collection if the value is not {@code null}.
     *
     * @param columnIds the collection to which the value should be added
     * @param columnId  the value to be added if it is not {@code null}
     */
    private void addIfNonNull(final Collection<Long> columnIds, final Long columnId) {
        if (Objects.nonNull(columnId)) {
            columnIds.add(columnId);
        }
    }

    /**
     * <p>
     * Constructs a CRMUnderlyingData object using default pagination from the request.
     * </p>
     *
     * @param orgId        The unique identifier for the organization
     * @param jsonObject   A JsonObject containing the request data with keys: VIEW_ID, MODULE_ID, CHILD_VIEW_ID,
     *                     SELECT (configured field IDs), SORT, and LIMIT
     * @param crmResponse  Represents the {@link CrmResponse} for i18n resolution; if null, uses I18nResolutionAdapter
     * @param i18nResolver An instance of I18nResolver used for internationalization of field names
     * @return A CRMUnderlyingData object containing view configuration, configured fields, sort/limit settings
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @NonNull
    private CRMUnderlyingData constructCRMUnderlyingData(final @NonNull Long orgId,
                                                         final @NonNull JsonObject jsonObject,
                                                         final CrmResponse crmResponse,
                                                         final @NonNull I18nResolver i18nResolver,
                                                         final @NonNull Map<Long, ReportModularField> reportFieldMap) {
        return constructCRMUnderlyingData(orgId, jsonObject, crmResponse, i18nResolver, null, null, reportFieldMap);
    }

    /**
     * <p>
     * Constructs a CRMUnderlyingData object with optional pagination override values.
     * </p>
     *
     * @param orgId        The unique identifier for the organization
     * @param jsonObject   A JsonObject containing request data with keys: VIEW_ID, MODULE_ID, CHILD_VIEW_ID,
     *                     SELECT (configured field IDs), SORT (field ID and sort type), and LIMIT (start/count)
     * @param crmResponse  Represents the {@link CrmResponse} for i18n resolution; if null, uses I18nResolutionAdapter
     * @param i18nResolver An instance of I18nResolver used for internationalization of field names
     * @param start        Optional 1-based start row index; when null, uses request JSON LIMIT or default start (1)
     * @param count        Optional row count limit; when null, uses request JSON LIMIT or default count (10)
     * @return A CRMUnderlyingData object containing view configuration, configured fields, sort/limit settings,
     * and field details with display names and metadata
     * @throws ErrorCode with AnalyticsErrorCodes if module is invalid, field is not sortable, or mapper tables cannot be fetched
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @NonNull
    private CRMUnderlyingData constructCRMUnderlyingData(final @NonNull Long orgId,
                                                         final @NonNull JsonObject jsonObject,
                                                         final CrmResponse crmResponse,
                                                         final @NonNull I18nResolver i18nResolver,
                                                         final Integer start,
                                                         final Integer count,
                                                         final @NonNull Map<Long, ReportModularField> reportFieldMap) {
        final Long viewId = jsonObject.getLong(CRMReportKeys.VIEW_ID);
        final Integer moduleId = jsonObject.getInt(CRMReportKeys.MODULE_ID);
        final Long childViewId = jsonObject.getLong(CRMReportKeys.CHILD_VIEW_ID);
        final Collection<Long> configuredFieldIds = new HashSet<>();
        final Collection<Long> selectFieldIds = new HashSet<>();
        final Collection<AnalyticsConfiguredField> configuredFieldDetails = new ArrayList<>();
        AnalyticsLimit analyticsLimit = null;
        AnalyticsSort analyticsSort = null;
        final JsonArray selectJsonArray = jsonObject.optJsonArray(CRMViewPropertyKeys.SELECT);
        final int totalFields = Objects.isNull(selectJsonArray) ? NumericalConstant.ZERO : selectJsonArray.size();
        final Collection<ChartView> chartViews = analyticsConfigManager.getChartViewManager()
                .getAll(orgId, Criteria.eq(Table.get(ANAREPORT.TABLE).getColumn(ANAREPORT.VIEW_ID), childViewId));


        if (NumericalConstant.ZERO < totalFields) {
            for (int index = 0; index < totalFields; index++) {
                final Long fieldId = selectJsonArray.getLong(index);
                configuredFieldIds.add(fieldId);
                selectFieldIds.add(fieldId);
            }
        } else {
            final CRMViewConfigManager<CRMDefaultFieldConfig> defaultFieldConfigManager = crmManager.getDefaultFieldConfigManager();
            final Criteria criteria = Criteria.eq(defaultFieldConfigManager.getTable()
                            .getColumn(CRMREPORTDEFAULTFIELDCONFIG.MODULE_ID),
                    moduleId);

            try {
                final Collection<CRMDefaultFieldConfig> crmDefaultFieldConfigs = defaultFieldConfigManager.getAll(orgId, criteria);
                final Set<Long> systemModularFieldIds = crmDefaultFieldConfigs.stream().map(CRMDefaultFieldConfig::getFieldId).collect(Collectors.toSet());

                reportFieldMap.values().stream()
                        .filter(field -> !field.isCustom() && systemModularFieldIds.contains(field.getSystemFieldId()))
                        .forEach(reportModularField -> configuredFieldIds.add(reportModularField.id()));
            } catch (final Exception exception) {
                LOGGER.error("CRM Analytics: Failed to create CRM underlying configured fields for orgId: {}, viewId: {}, moduleId: {}", orgId, viewId, moduleId, exception);
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                        "i18n.analytics.config.invalid.module");
            }
        }

        chartViews.stream().flatMap(chartView -> chartView.getReportFields().stream()).map(ReportField::getFieldId)
                .forEach(configuredFieldIds::add);
        configuredFieldDetails.addAll(getConfiguredFields(orgId, moduleId, configuredFieldIds, selectFieldIds, crmResponse, i18nResolver, reportFieldMap));
        createConfiguredFields(orgId, moduleId, configuredFieldIds);

        if (jsonObject.containsKey(CRMViewPropertyKeys.SORT)) {
            final JsonObject sortJson;
            final JsonArray sortArray = jsonObject.optJsonArray(CRMViewPropertyKeys.SORT);

            if (Objects.nonNull(sortArray) && sortArray.size() > 0) {
                sortJson = sortArray.getJsonObject(0);
            } else {
                sortJson = jsonObject.optJsonObject(CRMViewPropertyKeys.SORT);
            }

            if (Objects.nonNull(sortJson)) {
                final Long fieldId = sortJson.getLong(CRMViewPropertyKeys.FIELD);
                final ReportModularField reportModularField = reportFieldMap.get(fieldId);
                final StoreType storeType = fetchStoreTye(orgId, reportModularField);

                if (!StoreType.isSortable(storeType)) {
                    throw ErrorCode.get(AnalyticsErrorCodes.INVALID_SORT_FIELD,
                            "i18n.analytics.config.field.sort.not.applied");
                }
                analyticsSort = new AnalyticsSort(fieldId, SortType.getSortType(sortJson.getString(CRMViewPropertyKeys.TYPE)));
            }
        }

        if (Objects.nonNull(start) && Objects.nonNull(count)) {
            analyticsLimit = new AnalyticsLimit(start <= NumericalConstant.ZERO ? DEFAULT_START : start, count);
        } else if (jsonObject.containsKey(CommonKeys.LIMIT)) {
            final JsonObject limitJson = jsonObject.getJsonObject(CommonKeys.LIMIT);
            final int requestStart = limitJson.optInt(CommonKeys.START, DEFAULT_START);
            final int limit = limitJson.optInt(CommonKeys.COUNT, DEFAULT_LIMIT);

            analyticsLimit = new AnalyticsLimit(requestStart <= NumericalConstant.ZERO ? DEFAULT_START : requestStart,
                    limit);
        }

        try {
            final CRMViewConfigManager<MapperTableDetails> mapperTableManager = crmManager.getFieldMapperTableDetails();
            final Criteria criteria = Criteria.eq(mapperTableManager.getTable()
                            .getColumn(MCANAMAPPERTABLES.MODULE_ID),
                    moduleId);
            mapperTableManager.getAll(orgId, criteria);
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics: Failed to fetch CRM underlying mapper table details for orgId: {}, viewId: {}, moduleId: {}", orgId, viewId, moduleId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.MAPPER_TABLE_FETCH_ACTION_FAILED,
                    "i18n.analytics.config.invalid.mapper.tables");
        }

        return crmReportsBuilder.getCRMUnderlyingDataBuilder(viewId,
                moduleId,
                childViewId,
                configuredFieldIds,
                configuredFieldDetails,
                analyticsLimit,
                analyticsSort);
    }

    /**
     * Fetches the {@link StoreType} for the given {@link ReportModularField} by resolving
     * its associated custom or system modular field and looking up the CRM field info.
     *
     * @param orgId               the organization ID
     * @param reportModularField  the report modular field to resolve
     * @return the resolved {@link StoreType}
     */
    private StoreType fetchStoreTye(@NonNull Long orgId, ReportModularField reportModularField) {
        if (reportModularField.isCustom()) {
            final CRMCustomModularField customModularField = reportModularField.getCustomModularField();
            return fieldService.getField(orgId, customModularField.getCrmFieldId()).storeType();
        } else {
            final CRMSystemModularField systemModularField = reportModularField.getSystemModularField();

            if (Objects.isNull(systemModularField.getCrmFieldId())) {
                final Long anaFieldId = systemModularField.getAnaFieldId();
                final AnalyticsField analyticsField = analyticsFieldService.getAnalyticsFieldById(anaFieldId);
                return fieldTypeService.get(analyticsField.getFieldTypeId()).storeType();
            } else {
                return fieldService.getField(orgId, systemModularField.getCrmFieldId()).storeType();
            }
        }
    }

    /**
     * Builds and returns a {@link FilterField} based on provided parameters and field mappings.
     *
     * @param orgId              Organization id to identify the context for filtering.
     * @param currentTime        Current timestamp, used for time-related filters.
     * @param order              Ordering value to set priority or sequence in filtering.
     * @param fieldColumnMapping A FieldColumnMapping object containing the CRM field and filter field information.
     * @param systemModularFieldMap pre-fetched map of system modular field ID to CRMSystemModularField
     * @param fieldInfoMap          pre-fetched map of CRM field ID to FieldInfo
     * @return A FilterField instance, created using the provided field mapping and filter criteria.
     */
    private FilterField buildFilterFields(final Long orgId,
                                          final Long currentTime,
                                          final Long order,
                                          final CRMSystemModularFieldColumnMapping fieldColumnMapping,
                                          final ReportModularField reportModularField,
                                          final Map<Long, CRMSystemModularField> systemModularFieldMap,
                                          final Map<Long, FieldInfo> fieldInfoMap) {
        final Long filterColumnId = fieldColumnMapping.getFilterFieldId();
        final Long crmFieldValue = fieldColumnMapping.getCrmFieldValue();
        final Object value = Objects.isNull(crmFieldValue) ? fieldColumnMapping.getFilterValue() : crmFieldValue;

        final CRMSystemModularField modularField = systemModularFieldMap.get(fieldColumnMapping.getModularFieldId());
        final Long crmFieldId = modularField.getCrmFieldId();
        final boolean isAnalyticsField = Objects.isNull(crmFieldId);
        final AnalyticsField analyticsField = isAnalyticsField ? analyticsFieldService.getAnalyticsFieldById(modularField.getAnaFieldId()) : null;
        final FieldInfo crmFieldInfo = isAnalyticsField ? null : fieldInfoMap.getOrDefault(crmFieldId,
                fieldService.getField(orgId, crmFieldId));
        if (!isAnalyticsField && !fieldInfoMap.containsKey(crmFieldId)) {
            LOGGER.warn("CRM Analytics: Cache miss for fieldInfoMap in createSystemFilterField - orgId: {}, crmFieldId: {}", orgId, crmFieldId);
        }

        return crmReportsBuilder.getFilterField(orgId, filterColumnId, filterColumnId, currentTime, currentTime, order,
                OperationType.ACTUAL.getTypeCode(),
                TypeChecker.isNumber(value) ? Filter.NUMERIC.getTypeCode() : Filter.TEXT.getTypeCode(),
                OperatorType.IS.getTypeCode(),
                isAnalyticsField ? fieldTypeService.get(analyticsField.id()).storeType().getType() : crmFieldInfo.storeType().getType(),
                value, null, null, null, false,
                reportModularField.id(), null, fieldColumnMapping.getConfigAsString(),
                Objects.nonNull(crmFieldId) ? crmFieldInfo.aliasName() : null, null);
    }

    /**
     * <p>
     * Retrieves configured fields for a specified organization and module based on a collection of configured field ids
     * </p>
     *
     * @param orgId              The unique identifier of the organization from which the fields are being retrieved
     * @param moduleId           The unique identifier of the module for which the fields are being retrieved
     * @param configuredFieldIds A collection of field IDs that are configured and need to be checked against the retrieved fields
     * @param selectFieldIds     A collection of field IDs from the select array that should be marked as mandatory
     * @param crmResponse        Represents the {@link CrmResponse}
     * @param i18nResolver       The resolver used to fetch the internationalized field names
     * @return A collection of {@link AnalyticsConfiguredField }representing the configured fields
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    private Collection<AnalyticsConfiguredField> getConfiguredFields(final Long orgId,
                                                                     final Integer moduleId,
                                                                     final Collection<Long> configuredFieldIds,
                                                                     final Collection<Long> selectFieldIds,
                                                                     final CrmResponse crmResponse,
                                                                     final I18nResolver i18nResolver,
                                                                     final Map<Long, ReportModularField> reportFieldMap) {
        final Collection<AnalyticsConfiguredField> configuredFieldDetails = new ArrayList<>();
        final Collection<Long> crmFieldIds = new HashSet<>();
        final Collection<Long> anaFieldIds = new HashSet<>();
        final Map<Long, StoreType> fieldTypeMap = new HashMap<>();
        final boolean crmResponseIsNull = Objects.isNull(crmResponse);
        final I18nResolutionAdapter i18nResolutionAdapter = crmResponseIsNull ? new I18NCRMResolutionAdapter(i18nResolver, orgId) : null;

        try {
            final Collection<ReportModularField> reportModularFields = reportFieldMap.values().stream()
                    .filter(ReportModularField::getIsActive).toList();
            final Set<Long> systemModularFieldIds = new HashSet<>();
            for (final ReportModularField field : reportModularFields) {
                if (!field.isCustom()) {
                    systemModularFieldIds.add(field.getSystemFieldId());
                }
            }
            final Map<Long, CRMSystemModularField> systemModularFieldMap = systemModularFieldService.getAll(systemModularFieldIds);

            final Collection<ReportModularField> underlyingReportModularFields = reportModularFields.stream()
                    .filter(reportModularField -> {
                        final ModularField modularField = reportModularField.isCustom()
                                ? reportModularField.getCustomModularField()
                                : systemModularFieldMap.get(reportModularField.getSystemFieldId());
                        if (Objects.isNull(modularField)) {
                            return false;
                        }
                        final ModularFieldConfig modularFieldConfig = modularField.getModularFieldConfig();

                        return Objects.equals(modularField.getModuleId(), moduleId) &&
                                modularFieldConfig.isUnderlyingField();
                    }).toList();

            if (underlyingReportModularFields.isEmpty()) {
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                        "i18n.analytics.config.invalid.module");
            }

            CodeBlockExecutor.forEachSafely(underlyingReportModularFields, reportModularField -> {
                final ModularField modularField = reportModularField.isCustom()
                        ? reportModularField.getCustomModularField()
                        : systemModularFieldMap.get(reportModularField.getSystemFieldId());

                if (Objects.nonNull(modularField)) {

                    if (modularField instanceof CRMSystemModularField crmSystemModularField) {
                        Optional.ofNullable(crmSystemModularField.getAnaFieldId()).ifPresent(anaFieldIds::add);
                    }

                    Optional.ofNullable(modularField.getCrmFieldId()).ifPresent(crmFieldIds::add);
                }
            });

            if (!anaFieldIds.isEmpty()) {
                final Collection<AnalyticsField> analyticsFields = analyticsFieldService.getAnalyticsFieldsByIds(anaFieldIds).values();
                final Map<Long, Collection<Long>> anaFieldTypeMap = new HashMap<>();

                CodeBlockExecutor.forEachSafely(analyticsFields, analyticsField ->
                        anaFieldTypeMap.computeIfAbsent(analyticsField.getFieldTypeId(), value -> new HashSet<>())
                                .add(analyticsField.id()));

                if (!anaFieldTypeMap.isEmpty()) {
                    final Collection<Long> fieldTypeIds = anaFieldTypeMap.keySet();
                    final Collection<FieldType> fieldTypes = fieldTypeService.getFieldTypesByIds(fieldTypeIds);

                    CodeBlockExecutor.forEachSafely(fieldTypes, fieldType ->
                    {
                        final StoreType storeType = fieldType.storeType();

                        if (Objects.nonNull(storeType)) {
                            final Collection<Long> fieldIds = anaFieldTypeMap.get(fieldType.id());
                            final StoreType fieldStoreType = resolveUnderlyingStoreType(storeType);

                            fieldIds.forEach(fieldId -> fieldTypeMap.put(fieldId, fieldStoreType));
                        }
                    });
                }
            }
            final Map<Long, StoreType> rawStoreTypeMap = new HashMap<>();

            if (!crmFieldIds.isEmpty()) {
                final Map<Long, FieldInfo> fields = fieldService.getFieldsById(orgId, crmFieldIds);

                for (final FieldInfo fieldInfo : fields.values()) {
                    final FieldStore fieldStore = fieldInfo.fieldStore();

                    if (Objects.nonNull(fieldStore)) {
                        fieldTypeMap.put(fieldInfo.id(), resolveUnderlyingStoreType(fieldStore.storeType()));
                        rawStoreTypeMap.put(fieldInfo.id(), fieldStore.storeType());
                    }
                }
            }

            CodeBlockExecutor.forEachSafely(underlyingReportModularFields, reportModularField -> {
                final Long systemFieldId = reportModularField.getSystemFieldId();
                final ModularField modularField = reportModularField.isCustom()
                        ? reportModularField.getCustomModularField() : systemModularFieldMap.get(systemFieldId);
                final Long crmFieldId = modularField.getCrmFieldId();
                final Long analyticsFieldId = reportModularField.isCustom() ? null : systemModularFieldMap.get(systemFieldId).getAnaFieldId();

                if (!(Objects.isNull(crmFieldId) && Objects.isNull(analyticsFieldId))) {
                    final long fieldId = reportModularField.id();
                    final boolean isHidden = !configuredFieldIds.contains(fieldId);
                    final boolean isMandatory = selectFieldIds.contains(fieldId)  || modularField.getModularFieldConfig().isMandatory();
                    final boolean isAnalyticsField = Objects.isNull(crmFieldId);
                    final String fieldName = crmResponseIsNull ? i18nResolutionAdapter.get(modularField.getFieldName())
                            : crmResponse.resolveI18nPlaceholder(modularField.getFieldName());
                    final StoreType rawStoreType = isAnalyticsField
                            ? fieldTypeService.get(analyticsFieldService.getAnalyticsFieldById(analyticsFieldId).getFieldTypeId()).storeType()
                            : rawStoreTypeMap.get(crmFieldId);

                    configuredFieldDetails.add(new AnalyticsConfiguredField(fieldId, fieldName, isHidden,
                            StoreType.isSortable(rawStoreType), isMandatory,
                            isAnalyticsField ? fieldTypeMap.get(analyticsFieldId) : fieldTypeMap.get(crmFieldId)));
                }
            });
        } catch (final Exception exception) {
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                    "i18n.analytics.config.invalid.module");
        }

        return configuredFieldDetails;
    }

    /**
     * <p>
     * Creates configured fields for a specified organization and module using a collection of configured field ids
     * </p>
     *
     * @param orgId              The unique identifier of the organization for which the fields are being configured
     * @param moduleId           The unique identifier of the module under which the fields are being configured
     * @param configuredFieldIds A collection of field IDs that need to be configured for the specified organization and module
     */
    private void createConfiguredFields(final Long orgId,
                                        final Integer moduleId,
                                        final Collection<Long> configuredFieldIds) {
        try {
            final Collection<CRMFieldConfig> crmFieldConfigs = new ArrayList<>();

            CodeBlockExecutor.forEachSafely(configuredFieldIds,
                    configuredFieldId -> crmFieldConfigs.add(CRMFieldConfig.builder()
                            .orgId(orgId)
                            .moduleId(moduleId)
                            .fieldId(configuredFieldId)
                            .build()));

            crmManager.getFieldConfigManager()
                    .create(orgId, crmFieldConfigs);
            LOGGER.info("CRM Analytics: Successfully created CRM underlying configured fields for orgId: {} with ids: {}", orgId, configuredFieldIds);
        } catch (final Exception exception) {
            LOGGER.error("CRM Analytics: Failed to create CRM underlying configured fields for orgId: {}, moduleId: {}", orgId, moduleId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_CONFIGURED_FIELDS,
                    "i18n.analytics.config.invalid.configured.fields");
        }
    }

    /**
     * <p>
     * Resolves the appropriate store type based on the provided store type
     * </p>
     *
     * @param storeType Represents the {@link StoreType} to be checked
     * @return the resolved underlying data store type
     */
    private StoreType resolveUnderlyingStoreType(final StoreType storeType) {
        return switch (storeType) {
            case DATE, DATE_TIME, DATE_RANGE, DATE_TIME_RANGE -> storeType;
            default -> StoreType.SINGLE_STRING;
        };
    }


}
