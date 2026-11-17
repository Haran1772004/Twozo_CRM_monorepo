package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.ReportFieldCustomRange;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.general.utils.ValueNormalizer;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;

import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.CRMViewConfigAPI;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.BucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.fields.filter.Comparator;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutConfig;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRViewFieldMapper;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMFIELDBUCKETUNIT;
import com.tlc.crm.core.sql.resource.CRMREPORTVIEWFIELDMAPPER;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;
import java.util.List;
import java.util.Collections;

/***
 * Responsible for managing the view response in CRM reports.
 * Author: jamesh
 * Version: 1.0
 * ***/

abstract class CRMViewResponseConverter<T extends AnalyticsReport> implements CRMViewConfigAPI<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMViewResponseConverter.class);
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMReportManagerAPI crmReportManagerAPI;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;
    private final FieldService fieldService;
    private final OptionService optionService;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final ActivityTypeManager activityTypeManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final TagManager tagManager;
    private final DealManager dealManager;
    private final TerritoryManager territoryManager;

    protected CRMViewResponseConverter() {
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
        this.fieldService = FieldService.getInstance();
        this.optionService = OptionService.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.tagManager = TagManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
    }

    /**
     * Processes the childView data by fetching view details from the request and generates
     * a corresponding JSONObject response.
     *
     * @param fetchProperties  if {@code true}, includes field and view-specific properties in the response
     * @param userIds          the collection of user IDs relevant to the configuration
     * @param fieldIds         the collection used to collect all field IDs involved in this configuration
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param childViewRequest The type of action being performed, must not be null
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return the response as a {@link JsonObject}
     */
    protected JsonObject getViewConfigResponse(final boolean fetchProperties,
                                               final @NonNull Collection<Long> userIds,
                                               final @NonNull Collection<Long> fieldIds,
                                               final @NonNull AnalyticsRequest analyticsRequest,
                                               final @NonNull JsonObject childViewRequest,
                                               final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        try {
            final Long orgId = analyticsRequest.orgId();
            final Long viewId = childViewRequest.getLong(CRMReportKeys.VIEW_ID);

            final CRMChildViewDetails crmChildViewDetails = crmViewManagerAPI.getCRMChildViewManager()
                    .get(orgId, viewId);

            final Collection<CRMANAViewMapper> crmANAViewMappers = crmChildViewDetails.getCrmViewMapper();

            final Collection<Long> anaViewIds = crmANAViewMappers.stream()
                    .map(CRMANAViewMapper::getAnaViewId).toList();

            final Collection<T> models = getModels(orgId, anaViewIds);

            if (Objects.nonNull(crmChildViewDetails.getDashboardLayoutPosition())) {
                final String layoutString = crmChildViewDetails.getDashboardLayoutPosition().getLayoutAsString();
                final DashboardLayoutConfig dashboardLayoutConfig = new DashboardLayoutConfig(layoutString);
                response.put(CRMReportKeys.LAYOUT_ID, dashboardLayoutConfig.getId());
            }

            buildViewConfigResponse(fetchProperties, userIds, fieldIds, models,
                    crmChildViewDetails, analyticsRequest, i18nResolver, response);
        } catch (Exception error) {
            LOGGER.error("Error in Child Config Response Generator", error);
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_CONFIGURATION,
                    "i18n.analytics.invalid.field.configuration");
        }
        return response;
    }

    /**
     * Processes the childView data using pre-fetched models and child view details,
     * and generates a corresponding JSONObject response.
     *
     * @param fetchProperties     if {@code true}, includes field and view-specific properties in the response
     * @param userIds             A collection of userIds
     * @param fieldIds            the collection used to collect all field IDs involved in this configuration
     * @param models              the collection of models representing the analytics view mappings
     * @param crmChildViewDetails Details of the CRM child view configuration
     * @param analyticsRequest    Represents the {@link AnalyticsRequest} details
     * @param i18nResolver        An instance of {@link I18nResolver} for handling internationalization
     * @return the response as a {@link JsonObject}
     */
    protected JsonObject getViewConfigResponse(final boolean fetchProperties,
                                               final @NonNull Collection<Long> userIds,
                                               final @NonNull Collection<Long> fieldIds,
                                               final @NonNull Collection<T> models,
                                               final @NonNull CRMChildViewDetails crmChildViewDetails,
                                               final @NonNull AnalyticsRequest analyticsRequest,
                                               final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        try {
            buildViewConfigResponse(fetchProperties, userIds, fieldIds, models,
                    crmChildViewDetails, analyticsRequest, i18nResolver, response);
        } catch (Exception error) {
            LOGGER.error("Error in Child Config Response Generator", error);
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_CONFIGURATION,
                    "i18n.analytics.invalid.field.configuration");
        }
        return response;
    }

    /**
     * Common logic shared by both {@code getViewConfigResponse} overloads. Populates the response
     * with view metadata, report/filter fields, permission flags, and optional view properties.
     *
     * @param fetchProperties     if {@code true}, includes field and view-specific properties in the response
     * @param userIds             the collection of user IDs relevant to the configuration
     * @param fieldIds            the collection used to collect all field IDs involved in this configuration
     * @param models              the collection of analytics report models
     * @param crmChildViewDetails details of the CRM child view configuration
     * @param analyticsRequest    the analytics request context
     * @param i18nResolver        resolver for internationalization
     * @param response            the JSON response object to populate
     */
    private void buildViewConfigResponse(final boolean fetchProperties,
                                         final Collection<Long> userIds,
                                         final Collection<Long> fieldIds,
                                         final Collection<T> models,
                                         final CRMChildViewDetails crmChildViewDetails,
                                         final AnalyticsRequest analyticsRequest,
                                         final I18nResolver i18nResolver,
                                         final JsonObject response) {
        response.put(CRMReportKeys.VIEW_ID, crmChildViewDetails.id());
        response.put(CRMChildViewKeys.VIEW_NAME, crmChildViewDetails.getName());
        response.put(CRMReportKeys.VIEW_TYPE_ID, crmChildViewDetails.getViewTypeId());
        response.put(CRMChildViewKeys.VIEW_POSITION, crmChildViewDetails.getCrmViewPosition());

        final Collection<ReportField> reportFields = new ArrayList<>();
        final Collection<FilterField> filterFields = new ArrayList<>();

        for (final T model : models) {
            if (model.getReportFields() != null) {
                reportFields.addAll(model.getReportFields());
            }

            if (model.getFilterFields() != null) {
                filterFields.addAll(model.getFilterFields());
            }
        }

        final Collection<Long> reportFieldIds = reportFields.stream().map(ReportField::getFieldId).toList();

        fieldIds.addAll(reportFieldIds);

        response.put(CRMViewPropertyKeys.IS_PREVIEW, analyticsPermissionHandler.isViewAccessible(reportFieldIds, analyticsRequest));
        response.put(CRMViewPropertyKeys.CAN_EXPORT, analyticsPermissionHandler.isExportable(reportFieldIds, analyticsRequest));
        response.put(CRMViewPropertyKeys.IS_EDITABLE, analyticsPermissionHandler.isEditable(reportFieldIds, analyticsRequest));

        if (fetchProperties) {
            final boolean isKPIView = CRMViewType.getCrmViewType(crmChildViewDetails.getViewTypeId()) == CRMViewType.KPI;
            setFields(analyticsRequest, crmChildViewDetails.id(), reportFields, filterFields, isKPIView, response, i18nResolver, userIds);
            setViewSpecificProperties(models.stream().findFirst(), response);
        }
    }

    /**
     * Builds and sets the field-level response (metrics, view-by, filters, underlying config)
     * into the given response JSON object.
     *
     * @param analyticsRequest the analytics request context
     * @param viewId           the CRM child view ID
     * @param reportFields     the collection of report fields from the analytics model
     * @param filterFields     the collection of filter fields from the analytics model
     * @param isKPI            whether the view is a KPI type
     * @param response         the response JSON object to populate
     * @param i18nResolver     resolver for internationalization
     * @param userIds          the collection of user IDs to populate for owner-type filters
     */
    private void setFields(final @NonNull AnalyticsRequest analyticsRequest,
                           final @NonNull Long viewId,
                           final @NonNull Collection<ReportField> reportFields,
                           final Collection<FilterField> filterFields,
                           final boolean isKPI,
                           final @NonNull JsonObject response,
                           final @NonNull I18nResolver i18nResolver,
                           final @NonNull Collection<Long> userIds) {
        final Long orgId = analyticsRequest.orgId();
        final JsonObject chartProp = Json.object();

        final Table table = crmReportManagerAPI.getViewFieldMapper()
                .getTable();

        final Column column = table.getColumn(CRMREPORTVIEWFIELDMAPPER.CRM_VIEW_ID);

        try {
            final Collection<CRMRViewFieldMapper> fieldMappers = crmReportManagerAPI.getViewFieldMapper()
                    .getAll(orgId, Criteria.eq(column, viewId)).stream()
                    .sorted(java.util.Comparator.comparing(CRMRViewFieldMapper::getOrder)).toList();

            final CRMViewConfigManager<ReportModularField> reportModularFieldManager = crmReportManagerAPI.getReportFieldManager();
            final CRMViewConfigManager<FieldBucketUnit> fieldBucketUnitManager = crmReportManagerAPI.getFieldBucketUnitManager();

            // --- Batch pre-fetch: collect all modular field IDs and fetch in one call ---
            final Set<Long> allModularFieldIds = new HashSet<>();

            for (final CRMRViewFieldMapper fieldMapper : fieldMappers) {
                allModularFieldIds.add(fieldMapper.getModularFieldId());
            }

            final Map<Long, ReportModularField> reportModularFieldMap = new HashMap<>();

            if (!allModularFieldIds.isEmpty()) {
                final Collection<ReportModularField> reportModularFields = reportModularFieldManager.get(orgId, allModularFieldIds);

                for (final ReportModularField reportModularField : reportModularFields) {
                    reportModularFieldMap.put(reportModularField.id(), reportModularField);
                }
            }

            // --- Batch pre-fetch: all CRM field infos ---
            final Set<Long> allCrmFieldIds = new HashSet<>();

            for (final ReportModularField rmf : reportModularFieldMap.values()) {
                final ModularField mf = rmf.getModularField();

                if (Objects.nonNull(mf) && Objects.nonNull(mf.getCrmFieldId())) {
                    allCrmFieldIds.add(mf.getCrmFieldId());
                }
            }

            final Map<Long, FieldInfo> fieldInfoMap = allCrmFieldIds.isEmpty()
                    ? Map.of() : fieldService.getFieldsById(orgId, allCrmFieldIds);

            // --- Batch pre-fetch: all bucket units for bucket-applied field mappers ---
            final Set<Long> bucketFieldMapperIds = new HashSet<>();

            for (final CRMRViewFieldMapper fieldMapper : fieldMappers) {
                if (fieldMapper.isBuckedApplied() && fieldMapper.isUnitApplied()) {
                    bucketFieldMapperIds.add(fieldMapper.id());
                }
            }

            final Map<Long, FieldBucketUnit> bucketUnitMap = new HashMap<>();

            if (!bucketFieldMapperIds.isEmpty()) {
                final Table fieldBucketUnitTable = fieldBucketUnitManager.getTable();

                fieldBucketUnitManager.getAll(orgId, Criteria.in(fieldBucketUnitTable.getColumn(CRMFIELDBUCKETUNIT.CRM_VIEW_FIELD_ID),
                        bucketFieldMapperIds)).forEach(unit -> bucketUnitMap.put(unit.getViewFieldId(), unit));
            }

            final Map<Long, Map<Long, Collection<JsonObject>>> filterYMapping = new HashMap<>();
            final JsonArray xArray = Json.array();
            final JsonArray yAxis = Json.array();
            final JsonArray underlyingConfigArray = Json.array();
            final Set<Long> orderList = new HashSet<>();
            final Set<Long> filterFieldIds = new HashSet<>();
            final Set<Long> xFields = new HashSet<>();
            final CrmResponse crmResponse = analyticsRequest.crmResponse();

            // Pre-build lookup maps for O(1) access instead of O(n) stream searches per iteration
            final Map<FilterFieldKey, FilterField> filterFieldMap = buildFilterFieldMap(filterFields);
            final Map<ReportFieldKey, ReportField> reportFieldMap = buildReportFieldMap(reportFields);

            for (final CRMRViewFieldMapper fieldMapper : fieldMappers) {
                final Long modularFieldId = fieldMapper.getModularFieldId();

                if (fieldMapper.isFilterColumn()) {
                    processFilterColumn(orgId, fieldMapper, filterFieldMap, filterFields,
                            reportModularFieldMap, fieldInfoMap,
                            filterYMapping, orderList, filterFieldIds,
                            i18nResolver, userIds);
                } else {
                    processReportColumn(orgId, fieldMapper, modularFieldId, reportFieldMap, reportFields,
                            reportModularFieldMap, bucketUnitMap, isKPI,
                            xArray, yAxis, underlyingConfigArray, xFields, chartProp,
                            crmResponse, i18nResolver);
                }
            }

            attachFiltersToYAxis(yAxis, filterYMapping);

            chartProp.put(CRMViewPropertyKeys.METRICS, yAxis);

            if (!isKPI) {
                chartProp.put(CRMViewPropertyKeys.VIEW_BY, xArray);
            }

            chartProp.put(CRMViewPropertyKeys.UNDERLYING_CONFIG, underlyingConfigArray);
            response.put(CRMChildViewKeys.VIEW_PROPERTIES, chartProp);
        } catch (final Exception error) {
            LOGGER.error("Error in setting Fields", error);
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_CONFIGURATION,
                    "i18n.analytics.invalid.field.configuration");
        }
    }

    /**
     * Processes a filter column field mapper — matches with its filter field, builds the filter value JSON,
     * and populates the filter-to-Y-axis mapping.
     *
     * @param orgId                 the organization ID
     * @param fieldMapper           the view field mapper being processed
     * @param filterFieldMap        pre-built lookup map for filter fields
     * @param filterFields          the mutable collection of filter fields (for removal after processing)
     * @param reportModularFieldMap pre-fetched map of modularFieldId to ReportModularField
     * @param fieldInfoMap          pre-fetched map of crmFieldId to FieldInfo
     * @param filterYMapping        mapping of filters to Y-axis columns
     * @param orderList             tracks processed filter orders
     * @param filterFieldIds        tracks processed filter field IDs
     * @param i18nResolver          resolver for internationalization
     * @param userIds               the collection of user IDs to populate for owner-type filters
     */
    private void processFilterColumn(final Long orgId,
                                     final CRMRViewFieldMapper fieldMapper,
                                     final Map<FilterFieldKey, FilterField> filterFieldMap,
                                     final Collection<FilterField> filterFields,
                                     final Map<Long, ReportModularField> reportModularFieldMap,
                                     final Map<Long, FieldInfo> fieldInfoMap,
                                     final Map<Long, Map<Long, Collection<JsonObject>>> filterYMapping,
                                     final Set<Long> orderList,
                                     final Set<Long> filterFieldIds,
                                     final I18nResolver i18nResolver,
                                     final Collection<Long> userIds) {
        final FilterFieldKey key = new FilterFieldKey(fieldMapper.getAnaActualColumnId(), fieldMapper.getFilterOrder());
        final FilterField filterField = filterFieldMap.get(key);

        if (Objects.isNull(filterField) || !filterField.isCustomFilter()) {
            return;
        }

        final JsonObject filterJson = Json.object();
        final JsonArray filterJsonArray = Json.array();
        final Object filterValue = filterField.getValue();
        final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();
        final JsonObject jsonObject = Json.object();
        final boolean isPeriodicFilter = fieldMapper.isPeriodFilterApplied();
        final ReportModularField reportModularField = reportModularFieldMap.get(fieldMapper.getModularFieldId());
        final JsonObject filterValueJson = Json.object();
        final Long filterOrder = filterField.getOrder();
        final boolean isExistingOrder = orderList.contains(filterOrder) && !filterFieldIds.add(filterField.id());

        buildFilterValueJson(orgId, filterField, filterFunctionMapper, filterValue, isPeriodicFilter,
                reportModularField, filterJson, filterValueJson, fieldInfoMap, i18nResolver, userIds);

        if (filterValueJson.size() > 0) {
            boolean isValueSet = false;
            final Map<Long, Collection<JsonObject>> filterJsonMap = filterYMapping.get(filterField.getFieldId());
            final Collection<JsonObject> filterFieldJson = Objects.isNull(filterJsonMap) ? List.of() : filterJsonMap.get((long) fieldMapper.getOrder());

            if (!filterFieldJson.isEmpty()) {
                for (final JsonObject filterJsonObject : filterFieldJson) {
                    if (filterJsonObject.containsKey(CRMViewPropertyKeys.FILTERS)) {
                        final JsonObject filterValueObject = filterJsonObject.getJsonObject(CRMViewPropertyKeys.FILTERS);

                        if (Objects.equals(filterOrder, filterValueObject.getLong(CRMViewPropertyKeys.ORDER))) {
                            if (filterValueObject.containsKey(CRMViewPropertyKeys.VALUES)) {
                                setFilterValue(filterValueJson, filterValueObject.getJsonArray(CRMViewPropertyKeys.VALUES));
                                isValueSet = true;
                            }
                        } else if (!isExistingOrder && filterJsonArray.size() == 0) {
                            setFilterValue(filterValueJson, filterJsonArray);
                            isValueSet = true;
                        }
                    }
                }

                if (!isValueSet) {
                    setFilterValue(filterValueJson, filterJsonArray);
                }

                if (!isExistingOrder) {
                    populateFilterJson(reportModularField, filterOrder, filterJson, fieldMapper,
                            filterField.getOperatorType(), filterJsonArray, isPeriodicFilter,
                            jsonObject, filterYMapping);
                }
            } else {
                setFilterValue(filterValueJson, filterJsonArray);
                populateFilterJson(reportModularField, filterOrder, filterJson, fieldMapper,
                        filterField.getOperatorType(), filterJsonArray, isPeriodicFilter,
                        jsonObject, filterYMapping);
            }
        } else {
            populateFilterJson(reportModularField, filterOrder, filterJson, fieldMapper,
                    filterField.getOperatorType(), filterJsonArray, isPeriodicFilter,
                    jsonObject, filterYMapping);
        }

        orderList.add(filterOrder);
        filterFields.remove(filterField);
    }

    /**
     * Builds the filter value JSON based on periodic filters, function mappers, range values,
     * or comparator-driven value resolution.
     *
     * @param orgId                the organization ID
     * @param filterField          the filter field being processed
     * @param filterFunctionMapper the function mapper for the filter (may be null)
     * @param filterValue          the raw filter value
     * @param isPeriodicFilter     whether this is a periodic filter
     * @param reportModularField   the report modular field metadata
     * @param filterJson           the filter JSON to set function ID on
     * @param filterValueJson      the filter value JSON to populate
     * @param fieldInfoMap         pre-fetched map of crmFieldId to FieldInfo
     * @param i18nResolver         resolver for internationalization
     * @param userIds              the collection of user IDs to populate for owner-type filters
     */
    private void buildFilterValueJson(final Long orgId,
                                      final FilterField filterField,
                                      final FilterFunctionMapper filterFunctionMapper,
                                      final Object filterValue,
                                      final boolean isPeriodicFilter,
                                      final ReportModularField reportModularField,
                                      final JsonObject filterJson,
                                      final JsonObject filterValueJson,
                                      final Map<Long, FieldInfo> fieldInfoMap,
                                      final I18nResolver i18nResolver,
                                      final Collection<Long> userIds) {
        final Long fromValue = filterField.getFrom();
        final Long toValue = filterField.getTo();

        if (isPeriodicFilter || Objects.nonNull(filterFunctionMapper)) {
            if (Objects.nonNull(fromValue) && Objects.nonNull(toValue)) {
                buildRangeFilterValue(fromValue, toValue, filterValueJson);
            } else if (Objects.nonNull(filterFunctionMapper)) {
                final String functionName = filterFunctionMapper.getFunctionName();

                filterJson.put(CRMReportKeys.FUNCTION_ID, CRMFunction.getFunctionByName(functionName.toLowerCase()).getTypeCode());
                if (Objects.nonNull(filterValue)) {
                    filterValueJson.put(CRMViewPropertyKeys.FILTER_VALUE, filterValue);
                }
            }
        } else {
            if (Objects.nonNull(fromValue) && Objects.nonNull(toValue)) {
                buildRangeFilterValue(fromValue, toValue, filterValueJson);
            } else {
                final OperatorType operatorType = filterField.getOperatorType();

                if (Objects.nonNull(operatorType) && operatorType.isHasValue()) {
                    setFilterValues(orgId, reportModularField, filterField, filterValueJson, fieldInfoMap, i18nResolver, userIds);
                }
            }
        }
    }

    /**
     * Builds a range filter value JSON with from/to values and puts it into the filter value JSON.
     *
     * @param fromValue       the range start value
     * @param toValue         the range end value
     * @param filterValueJson the filter value JSON to populate
     */
    private void buildRangeFilterValue(final Long fromValue,
                                       final Long toValue,
                                       final JsonObject filterValueJson) {
        final JsonObject valueJson = Json.object();

        valueJson.put(CommonKeys.FROM, fromValue);
        valueJson.put(CommonKeys.TO, toValue);
        filterValueJson.put(CRMViewPropertyKeys.FILTER_VALUE, valueJson);
    }

    /**
     * Processes a non-filter report column — routes to X-axis or Y-axis processing based on the
     * report field's axis type.
     *
     * @param orgId                 the organization ID
     * @param fieldMapper           the view field mapper being processed
     * @param modularFieldId        the modular field ID from the mapper
     * @param reportFieldMap        pre-built lookup map for report fields
     * @param reportFields          the mutable collection of report fields (for removal after processing)
     * @param reportModularFieldMap pre-fetched map of modularFieldId to ReportModularField
     * @param bucketUnitMap         pre-fetched map of viewFieldId to FieldBucketUnit
     * @param isKPI                 whether the view is a KPI type
     * @param xArray                the JSON array accumulating X-axis fields
     * @param yAxis                 the JSON array accumulating Y-axis fields
     * @param underlyingConfigArray the JSON array accumulating underlying config entries
     * @param xFields               tracks processed X-axis modular field IDs to avoid duplicates
     * @param chartProp             the chart properties JSON object
     * @param crmResponse           the CRM response for i18n placeholder resolution
     * @param i18nResolver          resolver for internationalization
     */
    private void processReportColumn(final Long orgId,
                                     final CRMRViewFieldMapper fieldMapper,
                                     final Long modularFieldId,
                                     final Map<ReportFieldKey, ReportField> reportFieldMap,
                                     final Collection<ReportField> reportFields,
                                     final Map<Long, ReportModularField> reportModularFieldMap,
                                     final Map<Long, FieldBucketUnit> bucketUnitMap,
                                     final boolean isKPI,
                                     final JsonArray xArray,
                                     final JsonArray yAxis,
                                     final JsonArray underlyingConfigArray,
                                     final Set<Long> xFields,
                                     final JsonObject chartProp,
                                     final CrmResponse crmResponse,
                                     final I18nResolver i18nResolver) {
        final ReportFieldKey key = new ReportFieldKey(fieldMapper.getAnaActualColumnId(), fieldMapper.getModularFieldId(),
                fieldMapper.getAxis());
        final ReportField reportField = reportFieldMap.get(key);

        if (Objects.isNull(reportField)) {
            return;
        }

        reportFields.remove(reportField);

        if (reportField.getAxis() == Axis.X_AXIS) {
            processXAxisField(orgId, fieldMapper, modularFieldId, reportField,
                    bucketUnitMap, isKPI, xArray, xFields, chartProp, i18nResolver);
        } else {
            processYAxisField(orgId, fieldMapper, modularFieldId, reportField,
                    reportModularFieldMap, yAxis, underlyingConfigArray, crmResponse);
        }
    }

    /**
     * Processes an X-axis report field — builds the X-axis JSON with field details, bucket configuration,
     * and function/time-period metadata.
     *
     * @param orgId          the organization ID
     * @param fieldMapper    the view field mapper being processed
     * @param modularFieldId the modular field ID
     * @param reportField    the report field details
     * @param bucketUnitMap  pre-fetched map of viewFieldId to FieldBucketUnit
     * @param isKPI          whether the view is a KPI type
     * @param xArray         the JSON array accumulating X-axis fields
     * @param xFields        tracks processed X-axis modular field IDs to avoid duplicates
     * @param chartProp      the chart properties JSON object (for KPI duration)
     * @param i18nResolver   resolver for internationalization
     */
    private void processXAxisField(final Long orgId,
                                   final CRMRViewFieldMapper fieldMapper,
                                   final Long modularFieldId,
                                   final ReportField reportField,
                                   final Map<Long, FieldBucketUnit> bucketUnitMap,
                                   final boolean isKPI,
                                   final JsonArray xArray,
                                   final Set<Long> xFields,
                                   final JsonObject chartProp,
                                   final I18nResolver i18nResolver) {
        if (xFields.contains(modularFieldId)) {
            return;
        }

        final JsonObject xJson = Json.object();

        xJson.put(CRMViewPropertyKeys.ORDER, reportField.getOrder());
        xJson.put(CRMViewPropertyKeys.MODULE_ID, reportField.getModuleId());
        xJson.put(CRMViewPropertyKeys.SUB_MODULE_ID, reportField.getSubModuleId());
        xJson.put(CRMViewPropertyKeys.LABEL, reportField.getLabel());
        xJson.put(CRMViewPropertyKeys.TIME_PERIOD_FIELDS_ID, reportField.getTimePeriodId());

        if (!fieldMapper.isPeriodicFilterField()) {
            xJson.put(CRMReportKeys.FIELD_ID, modularFieldId);
        }
        if (Objects.nonNull(reportField.getFunctionName())) {
            xJson.put(CRMReportKeys.FUNCTION_ID, CRMFunction.getFunctionByName(reportField.getFunctionName().toLowerCase()).getTypeCode());
        }
        if (fieldMapper.isBuckedApplied()) {
            buildBucketJson(fieldMapper, reportField, bucketUnitMap, xJson, i18nResolver);
        }

        if (isKPI) {
            final OperationType operationName = reportField.getOperationType();

            xJson.put(CRMViewPropertyKeys.TIME_UNIT_ID, operationName.getTypeCode());

            chartProp.put(CRMViewPropertyKeys.DURATION, xJson);
        } else {
            xArray.put(xJson);
        }

        xFields.add(modularFieldId);
    }

    /**
     * Builds the bucket JSON for an X-axis field, including unit and custom range details.
     *
     * @param fieldMapper   the view field mapper being processed
     * @param reportField   the report field details
     * @param bucketUnitMap pre-fetched map of viewFieldId to FieldBucketUnit
     * @param xJson         the X-axis JSON object to attach the bucket to
     * @param i18nResolver  resolver for internationalization
     */
    private void buildBucketJson(final CRMRViewFieldMapper fieldMapper,
                                 final ReportField reportField,
                                 final Map<Long, FieldBucketUnit> bucketUnitMap,
                                 final JsonObject xJson,
                                 final I18nResolver i18nResolver) {
        final JsonObject bucketJson = Json.object();
        final ReportFieldCustomRange reportFieldCustomRange = reportField.getReportFieldCustomRange();

        if (fieldMapper.isUnitApplied()) {
            final FieldBucketUnit bucketUnit = bucketUnitMap.get(fieldMapper.id());

            if (Objects.nonNull(bucketUnit)) {
                final int unitId = bucketUnit.getUnitId();

                bucketJson.put(CRMViewPropertyKeys.UNIT_ID, unitId);
                bucketJson.put(CRMViewPropertyKeys.UNIT_NAME, i18nResolver.get(BucketUnit.getUnitName(unitId)));

                xJson.put(CRMViewPropertyKeys.BUCKET, bucketJson);
            }
        }

        if (Objects.nonNull(reportFieldCustomRange)) {
            bucketJson.put(CommonKeys.FROM, reportFieldCustomRange.getFrom());
            bucketJson.put(CommonKeys.TO, reportFieldCustomRange.getTo());

            xJson.put(CRMViewPropertyKeys.BUCKET, bucketJson);
        }
    }

    /**
     * Processes a Y-axis report field — builds the Y-axis JSON with field, module, and measure details,
     * and appends to the underlying config array.
     *
     * @param orgId                 the organization ID
     * @param fieldMapper           the view field mapper being processed
     * @param modularFieldId        the modular field ID
     * @param reportField           the report field details
     * @param reportModularFieldMap pre-fetched map of modularFieldId to ReportModularField
     * @param yAxis                 the JSON array accumulating Y-axis fields
     * @param underlyingConfigArray the JSON array accumulating underlying config entries
     * @param crmResponse           the CRM response for i18n placeholder resolution
     */
    private void processYAxisField(final Long orgId,
                                   final CRMRViewFieldMapper fieldMapper,
                                   final Long modularFieldId,
                                   final ReportField reportField,
                                   final Map<Long, ReportModularField> reportModularFieldMap,
                                   final JsonArray yAxis,
                                   final JsonArray underlyingConfigArray,
                                   final CrmResponse crmResponse) {
        final JsonObject yJson = Json.object();
        final ReportModularField reportModularField = reportModularFieldMap.get(modularFieldId);
        final ModularField modularField = reportModularField != null
                ? reportModularField.getModularField() : null;
        final OperationType measureType = Objects.equals(reportField.getOperationType(), OperationType.COUNT) ? OperationType.TOTAL
                : reportField.getOperationType();
        final JsonObject underlyingConfigJson = Json.object();

        if (Objects.nonNull(modularField)) {
            final JsonObject moduleJson = Json.object();
            final Integer moduleId = modularField.getModuleId();

            yJson.put(CRMReportKeys.MODULE_ID, moduleId);

            moduleJson.put(CRMViewPropertyKeys.ID, moduleId);
            moduleJson.put(CRMViewPropertyKeys.NAME, crmResponse.resolveI18nPlaceholder(ModuleType.getModuleType(moduleId)
                    .getI18nKey()));

            underlyingConfigJson.put(CRMViewPropertyKeys.MODULE, moduleJson);
        }

        yJson.put(CRMReportKeys.FIELD_ID, modularFieldId);
        yJson.put(CRMViewPropertyKeys.COLUMN_ID, fieldMapper.getAnaActualColumnId());
        yJson.put(CRMViewPropertyKeys.MEASURE_ID, measureType.getTypeCode());
        yJson.put(CRMViewPropertyKeys.LABEL, reportField.getLabel());
        yJson.put(CRMViewPropertyKeys.ORDER, fieldMapper.getOrder());

        underlyingConfigJson.put(CRMViewPropertyKeys.CHILD_VIEW_ID, fieldMapper.getViewId());

        yAxis.put(yJson);
        underlyingConfigArray.put(underlyingConfigJson);
    }

    /**
     * After processing all field mappers, attaches accumulated filter JSON objects
     * to their corresponding Y-axis entries.
     *
     * @param yAxis          the JSON array of Y-axis fields
     * @param filterYMapping the mapping of filter JSON objects keyed by field ID and order
     */
    private void attachFiltersToYAxis(final JsonArray yAxis,
                                      final Map<Long, Map<Long, Collection<JsonObject>>> filterYMapping) {
        for (int index = 0; index < yAxis.size(); index++) {
            final JsonObject y = yAxis.getJsonObject(index);
            final Long order = (long) y.getInt(CRMReportKeys.ORDER);
            final long fieldId = y.getLong(CRMReportKeys.FIELD_ID);
            final Collection<JsonObject> filters = new HashSet<>(Optional.ofNullable(filterYMapping.get(fieldId))
                    .map(inner -> inner.get(order))
                    .orElse(Collections.emptyList()));

            if (!filters.isEmpty()) {
                final JsonArray filterResponse = Json.array();

                for (final JsonObject filter : filters) {
                    if (filter.containsKey(CRMViewPropertyKeys.FILTERS)) {
                        filterResponse.put(filter.get(CRMViewPropertyKeys.FILTERS));
                    }

                    if (filter.containsKey(CRMViewPropertyKeys.PERIODIC_FILTERS)) {
                        y.put(CRMViewPropertyKeys.PERIODIC_FILTERS, filter.get(CRMViewPropertyKeys.PERIODIC_FILTERS));
                    }
                }
                y.put(CRMViewPropertyKeys.FILTERS, filterResponse);
            }
        }
    }

    /**
     * Populates filter values for the given filter field and updates the provided JSON object.
     *
     * @param orgId             The organization ID to which the filter values belong.
     * @param modularField      A {@link ReportModularField} object containing metadata about the field for which filters are being set.
     * @param filterField       A {@link FilterField} object representing the field in the filter criteria.
     * @param filterValueJson   A {@link JsonObject} that will be populated with the filter values.
     * @param fieldInfoMap      Pre-fetched map of crmFieldId to FieldInfo
     * @param i18nResolver      An {@link I18nResolver} instance used to resolve internationalized values for display.
     * @param userIds           A {@link Collection} of user IDs (if applicable) that may be updated based on the filter values.
     */
    private void setFilterValues(final Long orgId,
                                 final ReportModularField modularField,
                                 final FilterField filterField,
                                 final JsonObject filterValueJson,
                                 final Map<Long, FieldInfo> fieldInfoMap,
                                 final I18nResolver i18nResolver,
                                 final Collection<Long> userIds) {
        if (Objects.isNull(modularField)) {
            return;
        }

        final ModularField resolvedModularField = modularField.getModularField();
        final Long crmFieldId = resolvedModularField != null ? resolvedModularField.getCrmFieldId() : null;
        final Object value = filterField.getValue();

        if (Objects.isNull(crmFieldId) || Objects.isNull(value)) {
            filterValueJson.put(FilterFieldKeys.VALUE, value);
            return;
        }

        final FieldInfo fieldInfo = fieldInfoMap.get(crmFieldId);

        if (Objects.isNull(fieldInfo)) {
            return;
        }

        final StoreType storeType = fieldInfo.storeType();

        if (StoreType.isSelectorType(storeType)) {
            handleSelectorType(orgId, fieldInfo, filterField, filterValueJson, i18nResolver, modularField.getModularField().getModuleType());
        } else if (storeType.isEntityType() || StoreType.TAGS == storeType || StoreType.TERRITORY == storeType || StoreType.DEAL == storeType) {
            resolveEntityTypeFields(orgId, filterField, filterValueJson, storeType);
        } else if (storeType.isOwnerType()) {
            final Long userId = NumberUtils.parseNumber(value)
                    .longValue();

            userIds.add(userId);
            filterValueJson.put(CRMReportKeys.USER_ID, userId);
        } else {
            final Collection<?> values = ValueNormalizer.normalizeToCollection(value);

            if (values.size() > 1) {
                final JsonArray jsonArray = Json.array();

                for (final Object val : values) {
                    final JsonObject jsonObject = Json.object();
                    jsonObject.put(FilterFieldKeys.VALUE, val);
                    jsonArray.put(jsonObject);
                }

                filterValueJson.put(FilterFieldKeys.VALUES, jsonArray);
            } else {
                filterValueJson.put(FilterFieldKeys.VALUE, values);
            }
        }
    }

    /**
     * Resolves the entity type fields based on the provided organization ID, filter field, and store type.
     *
     * @param orgId           the ID of the organization to which the entity belongs.
     * @param filterField     the filter field containing the value to resolve.
     * @param filterValueJson the JSON object to populate with the resolved entity id and name.
     * @param storeType       the store type determining which entity manager to use.
     */
    private void resolveEntityTypeFields(final Long orgId, final FilterField filterField,
                                         final JsonObject filterValueJson,
                                         final StoreType storeType) {
        final Object rawValue = filterField.getValue();
        final Collection<?> ids = ValueNormalizer.normalizeToCollection(rawValue);
        final JsonArray resolvedArray = Json.array();

        for (final Object value : ids) {
            final JsonObject entry = Json.object();
            final Long id;
            try {
                id = NumberUtils.parseNumber(value).longValue();
            } catch (final Exception e) {
                entry.put(CommonKeys.NAME, String.valueOf(value));
                final JsonObject wrapper = Json.object();
                wrapper.put(FilterFieldKeys.VALUE, entry);
                resolvedArray.put(wrapper);
                continue;
            }

            entry.put(CRMReportKeys.ID, id);

            switch (storeType) {
                case COMPANY, COMPANY_ONBOARD ->
                        entry.put(CommonKeys.NAME, companyManager.getCompanyName(orgId, id));
                case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                        entry.put(CommonKeys.NAME, contactManager.getContactFullName(orgId, id));
                case DEAL -> entry.put(CommonKeys.NAME, dealManager.partialGet(orgId, id)
                        .getTitle());
                case TAGS -> entry.put(CommonKeys.NAME, tagManager.partialGet(orgId, id)
                        .getName());
                case TERRITORY -> entry.put(CommonKeys.NAME, territoryManager.get(orgId, id)
                        .getName());
            }

            final JsonObject wrapper = Json.object();
            wrapper.put(FilterFieldKeys.VALUE, entry);
            resolvedArray.put(wrapper);
        }

        if (resolvedArray.size() == 1) {
            final JsonObject single = resolvedArray.getJsonObject(0);
            single.keySet().forEach(key -> filterValueJson.put(key, single.get(key)));
        } else {
            filterValueJson.put(FilterFieldKeys.VALUES, resolvedArray);
        }
    }

    /**
     * <p>
     * Handles the processing of selector-type fields for a filter by resolving option IDs and names
     * based on the field's dropdown type. Delegates to {@link #handleHiddenOption} for hidden dropdowns
     * and to {@link #handleActivityOptions} for activity module fields.
     * </p>
     *
     * @param orgId           the organization id used to fetch field details or options.
     * @param fieldInfo       the metadata of the field, including its ID, alias name, and preset configuration.
     * @param filterField     the filter field containing the value to be resolved.
     * @param filterValueJson the JSON object to populate with the resolved ID and name.
     * @param i18nResolver    the resolver for internationalized text, used to fetch localized names.
     * @param moduleType      the {@link ModuleType} of the field, used to determine if activity-specific
     *                        option handling should be applied.
     */
    private void handleSelectorType(final Long orgId,
                                    final FieldInfo fieldInfo,
                                    final FilterField filterField,
                                    final JsonObject filterValueJson,
                                    final I18nResolver i18nResolver,
                                    final ModuleType moduleType) {
        final Object rawValue = filterField.getValue();
        final PresetConfig presetConfig = fieldInfo.presetConfig();

        if (Objects.isNull(presetConfig)) {
            return;
        }

        final DropdownType dropdownType = presetConfig.dropdownType();
        final boolean isHidden = DropdownType.HIDDEN_OPTION.equals(dropdownType);
        final boolean isActivity = Objects.equals(ModuleType.ACTIVITY, moduleType);
        final Collection<?> ids = ValueNormalizer.normalizeToCollection(rawValue);

        final JsonArray resolvedArray = Json.array();

        for (final Object value : ids) {
            final JsonObject entry = Json.object();
            final Long id;

            try {
                id = NumberUtils.parseNumber(value).longValue();
            } catch (final Exception e) {
                final String optionName = String.valueOf(value);
                final String localizedOptionName = i18nResolver.get(optionName);

                if (isHidden) {
                    handleHiddenOption(orgId, fieldInfo.aliasName(), null, optionName, entry);
                } else if (isActivity) {
                    handleActivityOptions(fieldInfo.aliasName(), null, optionName, entry, i18nResolver);
                } else {
                    Option option = optionService.getOption(orgId, fieldInfo, optionName);

                    if (Objects.isNull(option) && !optionName.equals(localizedOptionName)) {
                        option = optionService.getOption(orgId, fieldInfo, localizedOptionName);
                    }

                    if (Objects.nonNull(option)) {
                        entry.put(CRMReportKeys.ID, option.id());
                        entry.put(CommonKeys.NAME, i18nResolver.get(option.getOption()));
                    } else {
                        entry.put(CommonKeys.NAME, optionName);
                    }
                }

                final JsonObject wrapper = Json.object();
                wrapper.put(FilterFieldKeys.VALUE, entry);
                resolvedArray.put(wrapper);
                continue;
            }

            if (isHidden) {
                handleHiddenOption(orgId, fieldInfo.aliasName(), id, null, entry);
            } else if (isActivity) {
                handleActivityOptions(fieldInfo.aliasName(), id, null, entry, i18nResolver);
            } else {
                final Option option = optionService.get(orgId, fieldInfo.id(), id);
                if (Objects.isNull(option)) entry.put(CRMReportKeys.ID, id);
                else {
                    entry.put(CRMReportKeys.ID, option.id());
                    entry.put(CommonKeys.NAME, i18nResolver.get(option.getOption()));
                }
            }

            final JsonObject wrapper = Json.object();
            wrapper.put(FilterFieldKeys.VALUE, entry);
            resolvedArray.put(wrapper);
        }

        if (resolvedArray.size() == 1) {
            final JsonObject single = resolvedArray.getJsonObject(0);
            single.keySet().forEach(key -> filterValueJson.put(key, single.get(key)));
        } else {
            filterValueJson.put(FilterFieldKeys.VALUES, resolvedArray);
        }
    }

    /**
     * Handles the assignment of values for hidden dropdown options based on the provided alias name.
     * Fetches the entity by alias, and populates the filter value JSON with the entity's id and name.
     *
     * @param orgId           the organization id for fetching the entity details
     * @param aliasName       the alias name representing the type of entity
     * @param id              the id of the entity to fetch
     * @param name            the raw name string; may be {@code null} if resolving by id
     * @param filterValueJson the JSON object to populate with the entity's id and name
     */
    private void handleHiddenOption(final Long orgId,
                                    final String aliasName,
                                    final Long id,
                                    final String name,
                                    final JsonObject filterValueJson) {
        final String resolvedInputName = normalizeName(name);
        final boolean isKnownAlias = switch (aliasName) {
            case "pipeline", "pipelineStage", "lifeCycleStage", "lifeCycleStatus", "status", "typeId" -> true;
            default -> false;
        };

        if (!isKnownAlias) {
            return;
        }

        final ResolvedValue resolved = switch (aliasName) {
            case "pipeline" -> resolveEntity(
                    () -> Objects.nonNull(id) ? pipelineManager.get(orgId, id)
                            : Objects.nonNull(resolvedInputName) ? pipelineManager.getPipelineByName(orgId, null, resolvedInputName)
                            : null,
                    entity -> new ResolvedValue(entity.id(), entity.getName()));
            case "pipelineStage" -> resolveEntity(
                    () -> Objects.nonNull(id) ? pipelineStageManager.get(orgId, id)
                            : findPipelineStageByName(orgId, resolvedInputName),
                    entity -> new ResolvedValue(entity.id(), entity.getName()));
            case "lifeCycleStage" -> resolveEntity(
                    () -> Objects.nonNull(id) ? lifeCycleStageManager.get(orgId, id)
                            : Objects.nonNull(resolvedInputName) ? findByName(lifeCycleStageManager.getLifeCycleStages(orgId, List.of(resolvedInputName)),
                                    LifeCycleStage::getName, resolvedInputName) : null,
                    entity -> new ResolvedValue(entity.id(), entity.getName()));
            case "lifeCycleStatus" -> resolveEntity(
                    () -> Objects.nonNull(id) ? lifeCycleStatusManager.get(orgId, id)
                            : Objects.nonNull(resolvedInputName) ? findByName(lifeCycleStatusManager.getLifeCycleStatuses(orgId, List.of(resolvedInputName)),
                                    LifeCycleStatus::getName, resolvedInputName) : null,
                    entity -> new ResolvedValue(entity.id(), entity.getName()));
            case "status" -> resolveEntity(
                    () -> resolveDealState(id, resolvedInputName),
                    entity -> new ResolvedValue(entity.id(), entity.getDisplayName()));
            case "typeId" -> resolveEntity(
                    () -> Objects.nonNull(id) ? activityTypeManager.get(orgId, id)
                            : Objects.nonNull(resolvedInputName) ? findActivityTypeByName(orgId, resolvedInputName) : null,
                    entity -> new ResolvedValue(entity.id(), entity.getName()));
            default -> null;
        };

        putResolvedOrFallback(filterValueJson, resolved, id, resolvedInputName);
    }

    /**
     * <p>
     * Handles activity-specific option resolution for filter fields such as status, priority, and availability.
     * Resolves the option either by numeric ID or by name using the provided {@link I18nResolver},
     * and populates the filter JSON with the resolved ID and localized display name.
     * </p>
     *
     * @param aliasName       the alias name identifying the activity field type (e.g., "status", "priority", "availability").
     * @param id              the numeric id of the activity option; may be {@code null} if resolving by name.
     * @param name            the name of the activity option; may be {@code null} if resolving by id.
     * @param filterValueJson the JSON object to populate with the resolved ID and localized name.
     * @param i18nResolver    the resolver for internationalized text, used to fetch localized display names.
     */
    private void handleActivityOptions(final String aliasName,
                                       final Long id, final String name,
                                       final JsonObject filterValueJson,
                                       final I18nResolver i18nResolver) {
        final String resolvedInputName = normalizeName(name);
        final int optionId = Objects.nonNull(id) ? Math.toIntExact(id) : 0;

        final ResolvedValue resolved = switch (aliasName) {
            case "status" -> resolveActivityEnum(resolvedInputName, optionId, Status.values(),
                    Status::getId, Status::getDisplayName, Status::get, i18nResolver);
            case "priority" -> resolveActivityEnum(resolvedInputName, optionId, Priority.values(),
                    Priority::getId, Priority::getDisplayName, Priority::get, i18nResolver);
            case "availability" -> resolveActivityEnum(resolvedInputName, optionId, Availability.values(),
                    Availability::getId, Availability::getDisplayName, Availability::get, i18nResolver);
            default -> null;
        };

        if (Objects.isNull(resolved)) {
            if (Objects.nonNull(id)) {
                filterValueJson.put(CRMReportKeys.ID, id);
            }
            if (Objects.nonNull(resolvedInputName)) {
                filterValueJson.put(CommonKeys.NAME, resolvedInputName);
            }
            return;
        }

        filterValueJson.put(CRMReportKeys.ID, resolved.id());
        filterValueJson.put(CommonKeys.NAME, resolved.name());
    }

    /**
     * Holds a resolved entity's id and name.
     */
    private record ResolvedValue(long id, String name) {
    }

    /**
     * Resolves an entity using the given supplier and mapper. Returns {@code null} if the entity is not found.
     *
     * @param entitySupplier supplies the entity (may return null)
     * @param mapper         maps the entity to a {@link ResolvedValue}
     * @param <E>            the entity type
     * @return the resolved value, or {@code null} if entity not found
     */
    private <E> ResolvedValue resolveEntity(final java.util.function.Supplier<E> entitySupplier,
                                            final java.util.function.Function<E, ResolvedValue> mapper) {
        final E entity = entitySupplier.get();
        return Objects.nonNull(entity) ? mapper.apply(entity) : null;
    }

    /**
     * Resolves an activity enum (Status, Priority, Availability) by name or id.
     * Collapses the identical resolve pattern for all activity enums.
     *
     * @param name              the display name to match; may be {@code null} if resolving by id
     * @param id                the numeric id to resolve; used only when name is null
     * @param values            the enum values array
     * @param idGetter          extracts the numeric id from an enum constant
     * @param displayNameGetter extracts the display name from an enum constant
     * @param idLookup          looks up an enum constant by id
     * @param i18nResolver      resolver for internationalized text
     * @param <E>               the enum type
     * @return the resolved value, or {@code null} if not found
     */
    private <E extends Enum<E>> ResolvedValue resolveActivityEnum(
            final String name, final int id,
            final E[] values,
            final java.util.function.ToIntFunction<E> idGetter,
            final java.util.function.Function<E, String> displayNameGetter,
            final java.util.function.IntFunction<E> idLookup,
            final I18nResolver i18nResolver) {
        if (Objects.nonNull(name)) {

            for (final E entry : values) {

                if (isNameMatch(name, entry.name(), displayNameGetter.apply(entry), i18nResolver)) {
                    return new ResolvedValue(idGetter.applyAsInt(entry), i18nResolver.get(displayNameGetter.apply(entry)));
                }
            }
            return null;
        }

        try {
            final E entry = idLookup.apply(id);

            return Objects.nonNull(entry) ? new ResolvedValue(idGetter.applyAsInt(entry), i18nResolver.get(displayNameGetter.apply(entry))) : null;
        } catch (final Exception cause) {
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_CONFIGURATION);
        }
    }

    /**
     * Puts the resolved id and name into the filter JSON, or falls back to the raw id/name if unresolved.
     *
     * @param filterValueJson the JSON object to populate
     * @param resolved        the resolved value (may be null)
     * @param fallbackId      the raw/fallback id
     * @param fallbackName    the raw/fallback name
     */
    private void putResolvedOrFallback(final JsonObject filterValueJson,
                                       final ResolvedValue resolved,
                                       final Long fallbackId,
                                       final String fallbackName) {
        if (Objects.nonNull(resolved)) {
            filterValueJson.put(CRMReportKeys.ID, resolved.id());
            filterValueJson.put(CommonKeys.NAME, resolved.name());
        } else {
            if (Objects.nonNull(fallbackId)) {
                filterValueJson.put(CRMReportKeys.ID, fallbackId);
            }
            if (Objects.nonNull(fallbackName)) {
                filterValueJson.put(CommonKeys.NAME, fallbackName);
            }
        }
    }

    /**
     * Finds a Deal.State by id or name.
     */
    private Deal.State resolveDealState(final Long id, final String name) {
        if (Objects.nonNull(id)) {
            return Deal.State.getStateById(id.intValue());
        }

        if (Objects.nonNull(name)) {
            for (final Deal.State state : Deal.State.values()) {
                if (state.getName().equalsIgnoreCase(name) || state.getDisplayName().equalsIgnoreCase(name)) {
                    return state;
                }
            }
        }

        return null;
    }

    /**
     * Finds an entity by name from a collection using a case-insensitive match.
     *
     * @param entities   the collection to search (may be null)
     * @param nameGetter extracts the name from the entity
     * @param name       the name to match (may be null)
     * @param <E>        the entity type
     * @return the matching entity, or {@code null} if not found
     */
    private <E> E findByName(final Collection<E> entities,
                             final java.util.function.Function<E, String> nameGetter,
                             final String name) {
        if (Objects.isNull(name) || Objects.isNull(entities)) {
            return null;
        }

        return entities.stream()
                .filter(entity -> Objects.nonNull(nameGetter.apply(entity)) && nameGetter.apply(entity).equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * <p>
     * Finds an {@link ActivityType} for the given organization by matching the provided name.
     * First searches using {@link ActivityTypeManager#fetchActivityTypeByName}, then falls back
     * to a full lookup via {@link ActivityTypeManager#getActivityTypes} if not found.
     * </p>
     *
     * @param orgId the organization id used to scope the search.
     * @param name  the name of the activity type to search for (case-insensitive).
     * @return the matching {@link ActivityType}, or {@code null} if none is found.
     */
    private ActivityType findActivityTypeByName(final Long orgId, final String name) {
        ActivityType activityType = findByName(
                activityTypeManager.fetchActivityTypeByName(orgId, List.of(name)),
                ActivityType::getName, name);

        if (Objects.isNull(activityType)) {
            activityType = findByName(
                    activityTypeManager.getActivityTypes(orgId, true),
                    ActivityType::getName, name);
        }

        return activityType;
    }

    /**
     * <p>
     * Finds a {@link PipelineStage} for the given organization by matching the provided stage name
     * across all pipelines. Iterates through all pipelines and their stages to find a case-insensitive match.
     * </p>
     *
     * @param orgId     the organization id used to fetch all pipelines.
     * @param stageName the name of the pipeline stage to search for (case-insensitive).
     * @return the matching {@link PipelineStage}, or {@code null} if none is found.
     */
    private PipelineStage findPipelineStageByName(final Long orgId, final String stageName) {
        if (Objects.isNull(stageName)) {
            return null;
        }

        final Collection<Pipeline> pipelines = pipelineManager.getAllPipelineByOrg(orgId);

        if (Objects.isNull(pipelines)) {
            return null;
        }

        return pipelines.stream()
                .map(Pipeline::getPipelineStages)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(stage -> Objects.nonNull(stage.getName()) && stage.getName().equalsIgnoreCase(stageName))
                .findFirst()
                .orElse(null);
    }


    /**
     * <p>
     * Checks whether the given input name matches an enum entry by comparing against its enum constant name,
     * display name, or the internationalized form of the display name. All comparisons are case-insensitive.
     * </p>
     *
     * @param inputName    the name provided as input to match against.
     * @param enumName     the raw enum constant name (e.g., {@code "OPEN"}).
     * @param displayName  the human-readable display name or i18n key of the enum entry.
     * @param i18nResolver the resolver for internationalized text, used to compare the localized form of the display name.
     * @return {@code true} if the input name matches any of the three representations; {@code false} otherwise.
     */
    private boolean isNameMatch(final String inputName,
                                final String enumName,
                                final String displayName,
                                final I18nResolver i18nResolver) {
        return inputName.equalsIgnoreCase(enumName)
                || inputName.equalsIgnoreCase(displayName)
                || inputName.equalsIgnoreCase(i18nResolver.get(displayName));
    }

    /**
     * <p>
     * Normalizes the given name by trimming leading and trailing whitespace.
     * Returns {@code null} if the input is {@code null} or results in an empty string after trimming.
     * </p>
     *
     * @param name the raw name string to normalize; may be {@code null}.
     * @return the trimmed name, or {@code null} if the input is {@code null} or blank.
     */
    private String normalizeName(final String name) {
        if (Objects.isNull(name)) {
            return null;
        }

        final String trimmedName = name.trim();
        return trimmedName.isEmpty() ? null : trimmedName;
    }

    /**
     * Populates the given filter JSON object with the necessary filter properties and
     * adds it to the filter-to-Y-axis mapping.
     *
     * @param modularField     The field representing modular field details, used to set the field id
     * @param filterOrder      The order of the filter, used for defining filter execution order
     * @param filterJson       The JSON object to be populated with filter details
     * @param fieldMapper      The mapper containing field-related metadata such as column ids
     * @param operatorType     The operator type for the filter, used to fetch comparator id
     * @param filterJsonArray  The JSON array representing the filter values
     * @param isPeriodicFilter A flag indicating if the filter is periodic
     * @param jsonObject       The parent JSON object to which the filter JSON will be added
     * @param filterYMapping   A map that associates the filter JSON object with the corresponding Y-column id
     */
    private void populateFilterJson(final ReportModularField modularField,
                                    final Long filterOrder,
                                    final JsonObject filterJson,
                                    final CRMRViewFieldMapper fieldMapper,
                                    final OperatorType operatorType,
                                    final JsonArray filterJsonArray,
                                    final boolean isPeriodicFilter,
                                    final JsonObject jsonObject,
                                    final Map<Long, Map<Long, Collection<JsonObject>>> filterYMapping) {
        if (Objects.nonNull(modularField)) {
            filterJson.put(CRMReportKeys.FIELD_ID, modularField.id());
        }
        filterJson.put(CRMViewPropertyKeys.ORDER, filterOrder);
        filterJson.put(CRMViewPropertyKeys.COLUMN_ID, fieldMapper.getAnaActualColumnId());
        filterJson.put(CRMReportKeys.COMPARATOR_ID, operatorType.getTypeCode());

        if (filterJsonArray.size() > 0) {
            filterJson.put(CRMViewPropertyKeys.FILTER_VALUES, filterJsonArray);
        }

        if (filterJson.size() > 0) {
            jsonObject.put(isPeriodicFilter ? CRMViewPropertyKeys.PERIODIC_FILTERS
                            : CRMViewPropertyKeys.FILTERS,
                    filterJson);

            filterYMapping.computeIfAbsent(fieldMapper.getAnaYColumnID(), yColumnId -> new HashMap<>())
                    .computeIfAbsent((long) fieldMapper.getOrder(), order -> new ArrayList<>())
                    .add(jsonObject);
        }
    }

    /**
     * Adds the provided filter JSON object to the specified JSON array.
     *
     * @param filterJson The JSON object representing the filter details to be added
     * @param jsonArray  The JSON array to which the filter JSON will be added
     */
    private void setFilterValue(final JsonObject filterJson, final JsonArray jsonArray) {
        if (filterJson.containsKey(CRMViewPropertyKeys.VALUE)) {
            jsonArray.put(filterJson);
        } else if (filterJson.containsKey(CRMViewPropertyKeys.VALUES)) {
            jsonArray.append(filterJson.getJsonArray(CRMViewPropertyKeys.VALUES));
        } else {
            final JsonObject valueJson = Json.object();

            valueJson.put(CRMViewPropertyKeys.FILTER_VALUE, filterJson);
            jsonArray.put(valueJson);
        }
    }

    /**
     * Composite key for filter field lookup by actual column ID and order.
     */
    private record FilterFieldKey(Long actualColumnId, Long order) {
    }

    /**
     * Composite key for report field lookup by column ID, field ID, and axis type.
     * Including axisType as the third discriminator prevents HashMap collision when
     * the same field is used on both X_AXIS (View By) and Y_AXIS (Metrics) simultaneously.
     */
    private record ReportFieldKey(Long columnId, Long fieldId, Integer axisType) {
    }

    /**
     * Builds a lookup map from filter fields, keyed by (actualColumnId, order).
     *
     * @param filterFields the collection of filter fields
     * @return a map for O(1) filter field lookup
     */
    private Map<FilterFieldKey, FilterField> buildFilterFieldMap(final Collection<FilterField> filterFields) {
        final Map<FilterFieldKey, FilterField> map = new HashMap<>();

        for (final FilterField ff : filterFields) {
            map.put(new FilterFieldKey(ff.getActualColumnId(), ff.getOrder()), ff);
        }

        return map;
    }

    /**
     * Builds a lookup map from report fields, keyed by (columnId, fieldId, axisType).
     * The axisType (from ReportField) is the third discriminator — when the same field
     * is configured on both X_AXIS and Y_AXIS, each gets its own unique map entry,
     * eliminating the collision that caused one axis to be silently skipped.
     *
     * @param reportFields the collection of report fields
     * @return a map for O(1) report field lookup
     */
    private Map<ReportFieldKey, ReportField> buildReportFieldMap(final Collection<ReportField> reportFields) {
        final Map<ReportFieldKey, ReportField> map = new HashMap<>();

        for (final ReportField rf : reportFields) {
            map.put(new ReportFieldKey(rf.getColumnId(), rf.getFieldId(), rf.getAxisType()), rf);
        }

        return map;
    }

    abstract protected @NonNull Collection<T> getModels(final Long orgId, final Collection<Long> viewIds);

    abstract protected void setViewSpecificProperties(final Optional<T> model, final JsonObject response);
}
