package com.tlc.crm.core.api.analytics.converter.general.fields;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.report.FieldCategory;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.Filter;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.analytics.config.model.view.report.AnalyticsCustomFieldConfig;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.crm.core.api.analytics.entity.integ.reports.FieldColumnMapping;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.CRMReportsBuilder;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.service.module.CRMModuleConnectorService;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldColumnMappingService;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldService;
import com.tlc.crm.core.api.analytics.service.module.CRMCustomModularFieldColumnMappingService;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static com.tlc.analytics.config.model.table.AnalyticsDataType.NUMERICAL;
import static com.tlc.analytics.config.model.table.AnalyticsDataType.STRING;

/**
 * It responsible for converting the view fields
 *
 * @author jamesh
 * @version 1.0
 */
public class CRMViewFieldsConverter {
    private final CRMReportManagerAPI crmManager;
    private final CRMReportsBuilder crmReportsBuilder;
    private final ColumnDetailsService columnDetailsService;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final OptionService optionService;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final TagManager tagManager;
    private final TerritoryManager territoryManager;
    private final DealManager dealManager;
    private final ActivityTypeManager activityTypeManager;
    private final CRMModuleConnectorService crmModuleConnectorService;
    private final CRMSystemModularFieldColumnMappingService systemColumnMappingService;
    private final CRMSystemModularFieldService systemModularFieldService;
    private final CRMCustomModularFieldColumnMappingService customColumnMappingService;
    private final AnalyticsFieldService analyticsFieldService;

    private CRMViewFieldsConverter() {
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.crmReportsBuilder = CRMReportsBuilder.INSTANCE();
        this.columnDetailsService = ColumnDetailsService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.optionService = OptionService.getInstance();
        this.lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.tagManager = TagManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.crmModuleConnectorService = CRMModuleConnectorService.getInstance();
        this.systemColumnMappingService = CRMSystemModularFieldColumnMappingService.getInstance();
        this.systemModularFieldService = CRMSystemModularFieldService.getInstance();
        this.customColumnMappingService = CRMCustomModularFieldColumnMappingService.getInstance();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
    }

    private static class Instance {
        private static final CRMViewFieldsConverter INSTANCE = new CRMViewFieldsConverter();
    }

    public static CRMViewFieldsConverter INSTANCE() {
        return Instance.INSTANCE;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMViewFieldsConverter.class);

    /**
     * Converts the input field data into an {@link ANAField} object based on various mappings and configurations.
     *
     * @param orgId         The organization ID for which the conversion is being performed.
     * @param accessTime    The access time used for operations like report generation.
     * @param fieldId       The field ID being processed.
     * @param axis          Axis along which the field data is being processed.
     * @param isFilterField A flag indicating whether the field is a filter field or not.
     * @param fieldObject   A JsonObject containing the field properties and details for the conversion.
     * @return ANAField object containing the processed report fields, filter fields, column IDs, and other properties.
     * @throws Exception if the field category does not match any predefined types, or if there are issues during the conversion.
     */
    @NonNull
    public final ANAField covert(final @NonNull Long orgId,
                                 final @NonNull Long accessTime,
                                 final @NonNull Long fieldId,
                                 final Integer metricsModuleId,
                                 final @NonNull Long axis,
                                 final boolean isFilterField,
                                 final @NonNull JsonObject fieldObject,
                                 final Collection<Long> involvedColumnIds,
                                 final Map<Long, ReportModularField> reportFieldMap) throws Exception {
        final ReportModularField reportModularField = reportFieldMap.get(fieldId);
        final ModularField modularField = reportModularField.getModularField();
        final Integer viewBySelectorFieldModuleId = modularField.getModuleId();
        final Collection<Long> columnIds = new HashSet<>();
        final Collection<ReportField> reportFields = new ArrayList<>();
        final Collection<FilterField> filterFields = new ArrayList<>();
        final Long order = fieldObject.optLong(CRMViewPropertyKeys.ORDER, NumericalConstant.ZERO);
        final Long crmFieldId = modularField.getCrmFieldId();
        final FieldInfo resolvedFieldInfo = Objects.nonNull(crmFieldId) ? fieldService.getField(orgId, crmFieldId) : null;

        if (fieldObject.containsKey(CRMReportKeys.MODULE_ID)) {
            final Integer moduleId = fieldObject.getInt(CRMReportKeys.MODULE_ID);

            if (!Objects.equals(metricsModuleId, moduleId)) {
                involvedColumnIds.addAll(crmModuleConnectorService.getColumnIds(metricsModuleId, moduleId));
            }

            if (fieldObject.containsKey(CRMReportKeys.SUB_MODULE_ID)) {
                involvedColumnIds.addAll(crmModuleConnectorService.getColumnIds(moduleId, viewBySelectorFieldModuleId));
            }
        }

        CRMAnaFieldMeta lastMeta = null;

        if (reportModularField.isCustom()) {
            final StoreType storeType = reportModularField.getIsActive() && Objects.nonNull(resolvedFieldInfo)
                    ? resolvedFieldInfo.storeType() : StoreType.DROPDOWN;
            final Collection<CRMCustomModularFieldColumnMapping> customModularFieldColumnMappings =
                    customColumnMappingService.getCustomColumnMappings(viewBySelectorFieldModuleId,
                            fieldTypeService.getFieldTypeByStoreType(storeType).id());

            for (final CRMCustomModularFieldColumnMapping customModularFieldColumnMapping : customModularFieldColumnMappings) {
                columnIds.add(customModularFieldColumnMapping.getColumnId());

                lastMeta = setCustomSelectorField(orgId, accessTime, order, fieldId, isFilterField, fieldObject, axis,
                        customModularFieldColumnMapping, reportFields, filterFields, involvedColumnIds, resolvedFieldInfo, reportFieldMap);
            }
        } else {
            final Collection<CRMSystemModularFieldColumnMapping> systemModularFieldColumnMappings = systemColumnMappingService.getSystemColumnMappings(reportModularField.getSystemFieldId());

            for (final CRMSystemModularFieldColumnMapping systemModularFieldColumnMapping : systemModularFieldColumnMappings) {
                loadColumnIds(columnIds, systemModularFieldColumnMapping);
                loadColumnIds(involvedColumnIds, systemModularFieldColumnMapping);

                switch (systemModularFieldColumnMapping.getFieldCategory()) {
                    case MAPPER_FIELD, PRIMARY_FIELD, FILTER_FIELD -> {
                        if (Objects.nonNull(systemModularFieldColumnMapping.getFilterFieldId())) {
                            createFilterField(orgId, accessTime, order, fieldId, systemModularFieldColumnMapping, filterFields,
                                    null, involvedColumnIds, resolvedFieldInfo);
                        }
                    }
                    case ACTUAL_FIELD -> {
                        if (Objects.nonNull(systemModularFieldColumnMapping.getFilterFieldId())) {
                            createFilterField(orgId, accessTime, order, fieldId, systemModularFieldColumnMapping, filterFields,
                                    null, involvedColumnIds, resolvedFieldInfo);
                        }

                        if (isFilterField || Objects.nonNull(systemModularFieldColumnMapping.getSelectorFieldId())) {
                            final Integer havingOpType = systemModularFieldColumnMapping.getHavingOperationType();
                            lastMeta = setSelectorField(orgId, accessTime, order, fieldId, isFilterField, fieldObject, axis,
                                    systemModularFieldColumnMapping, reportFields, filterFields, havingOpType, involvedColumnIds, resolvedFieldInfo,
                                    systemModularFieldColumnMappings, reportFieldMap);
                        }
                    }
                    default -> {
                        throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_CATEGORY_TYPE,
                                "i18n.analytics.invalid.field.category.type");
                    }
                }
            }
        }

        return buildANAField(fieldId, columnIds, reportFields, filterFields, isFilterField, fieldObject, lastMeta);
    }

    /**
     * <p>
     * Loads the column IDs from the {@link FieldColumnMapping} object.
     * </p>
     *
     * @param columnIds                       the collection where the column IDs will be loaded
     * @param systemModularFieldColumnMapping the system modular field column mapping containing column IDs
     */
    private void loadColumnIds(final Collection<Long> columnIds, final @NonNull CRMSystemModularFieldColumnMapping systemModularFieldColumnMapping) {
        Stream.of(systemModularFieldColumnMapping.getFilterFieldId(),
                systemModularFieldColumnMapping.getJoinFieldId(),
                systemModularFieldColumnMapping.getGroupByFieldId(),
                systemModularFieldColumnMapping.getSelectorFieldId()).filter(Objects::nonNull).forEach(columnIds::add);
    }

    /**
     * For a filter request whose ACTUAL_FIELD selector points to a derived/bridged column
     * (e.g. {@code OACDIMCONTACT.FULL_NAME}), pick the first sibling {@code MAPPER_FIELD} mapping's
     * {@code joinFieldId}. By convention, that join column is the foreign key on the referencing
     * table (e.g. {@code OACDimDeal.PRIMARY_CONTACT}) — directly filterable, no joins required in
     * the CTE.
     *
     * @param siblings all column mappings of the same {@code CRMSystemModularField}
     * @return the FK column id to filter on, or {@code null} if none is suitable
     */
    private Long resolveFilterColumnFromSiblings(final Collection<CRMSystemModularFieldColumnMapping> siblings) {
        if (siblings == null) {
            return null;
        }

        for (final CRMSystemModularFieldColumnMapping sibling : siblings) {
            if (sibling.getFieldCategory() == com.tlc.crm.core.api.analytics.entity.integ.reports.FieldCategory.MAPPER_FIELD
                    && sibling.getJoinFieldId() != null && sibling.getFilterFieldId() == null) {
                return sibling.getJoinFieldId();
            }
        }

        return null;
    }

    /**
     * Resolves the column id to use for a filter request, in priority order:
     * <ol>
     *     <li>{@code mapperField.dropdownFilterId} — explicit (transient today, kept for forward-compat).</li>
     *     <li>Sibling {@code MAPPER_FIELD}'s {@code joinFieldId} — by convention the FK on the referencing
     *         table (e.g. {@code OACDimDeal.PRIMARY_CONTACT}), directly filterable in the CTE without joins.</li>
     *     <li>{@code selectorFieldId} — original behaviour, last-resort fallback (may itself be a derived
     *         display column like {@code OACDIMCONTACT.FULL_NAME} on a bridged dimension).</li>
     * </ol>
     * Each tier may legitimately be {@code null} (e.g. periodic filters have no {@code MAPPER_FIELD}
     * siblings), so plain null-checks are used rather than {@code Objects.requireNonNullElse}, which
     * throws NPE if the default argument is null.
     */
    private Long resolveFilterFieldId(final CRMSystemModularFieldColumnMapping mapperField,
                                      final Collection<CRMSystemModularFieldColumnMapping> siblingMappings,
                                      final Long selectorFieldId) {
        final Long dropdownFilterId = mapperField.getDropdownFilterId();

        if (Objects.nonNull(dropdownFilterId)) {
            return dropdownFilterId;
        }

        final Long siblingJoinFieldId = resolveFilterColumnFromSiblings(siblingMappings);

        return Objects.nonNull(siblingJoinFieldId) ? siblingJoinFieldId : selectorFieldId;
    }

    /**
     * <p>
     * Sets either a {@link FilterField} or a {@link ReportField} for a system modular field based on field type
     * and adds it to the corresponding collection.
     * </p>
     *
     * @param orgId              the organization ID
     * @param accessTime         the timestamp representing when the data is accessed
     * @param order              the order in which the field appears in the report
     * @param reportFieldId      the report modular field ID
     * @param isFilterField      whether the field is a filter field
     * @param fields             the JSON object containing field properties
     * @param axis               the axis (X or Y) along which the field is plotted
     * @param mapperField        the {@link CRMSystemModularFieldColumnMapping} containing mapping data
     * @param reportFields       the collection to which created {@link ReportField} will be added
     * @param filterFields       the collection to which created {@link FilterField} will be added
     * @param havingOperationType the {@link OperationType} ID for HAVING clause
     * @param involvedColumnIds  the collection of involved column IDs to populate
     * @param resolvedFieldInfo  the resolved {@link FieldInfo} for the CRM field (may be null)
     * @param siblingMappings    all column mappings of the same system modular field
     * @param reportFieldMap     the map of report field IDs to {@link ReportModularField}
     * @return the {@link CRMAnaFieldMeta} containing root table ID and actual column ID
     * @throws Exception if any error occurs during field processing
     */
    private CRMAnaFieldMeta setSelectorField( final @NonNull Long orgId,
                                   final @NonNull Long accessTime,
                                   final @NonNull Long order,
                                   final @NonNull Long reportFieldId,
                                   final boolean isFilterField,
                                   final @NonNull JsonObject fields,
                                   final @NonNull Long axis,
                                   final @NonNull CRMSystemModularFieldColumnMapping mapperField,
                                   final @NonNull Collection<ReportField> reportFields,
                                   final @NonNull Collection<FilterField> filterFields,
                                   final Integer havingOperationType,
                                   final Collection<Long> involvedColumnIds,
                                   final FieldInfo resolvedFieldInfo,
                                   final Collection<CRMSystemModularFieldColumnMapping> siblingMappings,
                                   final @NonNull Map<Long, ReportModularField> reportFieldMap) throws Exception {
        final Long selectorFieldId = mapperField.getSelectorFieldId();
        final String configAsString = mapperField.getConfigAsString();
        final Long fieldId = isFilterField ? resolveFilterFieldId(mapperField, siblingMappings, selectorFieldId)
                : selectorFieldId;
        LOGGER.info("Analytics Cache: Thread id : {}", Thread.currentThread().getId());
        final ColumnDetail columnInfo = columnDetailsService.get( fieldId );
        assert columnInfo != null;
        final Long rootTableId = columnInfo.getTableDetail().id();
        involvedColumnIds.add(selectorFieldId);

        if( isFilterField )
        {
            final String functionName = resolveFilterFunctionName(fields);

            final FilterField filterField = fields.containsKey( CRMViewPropertyKeys.VALUES )
                    ? extractFilterField( orgId, fields, columnInfo, fieldId, selectorFieldId, accessTime, order,
                    functionName, havingOperationType, reportFieldId, mapperField.getConfigAsString(), null, resolvedFieldInfo, reportFieldMap )
                    : generateFilterField( orgId, columnInfo, fieldId, selectorFieldId, order, accessTime,
                    null, null, fields, null, functionName, null, reportFieldId, configAsString, null, resolvedFieldInfo, reportFieldMap);
            filterFields.add( filterField );
        }
        else
        {
            final FieldCategory fieldType = columnInfo.getDataType() != NUMERICAL ? FieldCategory.CATEGORICAL : FieldCategory.AGGREGATE;
            final Long from = optBucketValue(fields, CommonKeys.FROM);
            final Long to = optBucketValue(fields, CommonKeys.TO);

            final CRMSystemModularField systemModularField = systemModularFieldService.get(mapperField.getModularFieldId());
            final Long crmFieldId = systemModularField.getCrmFieldId();
            final Long timePeriodFieldId = optNullableLong(fields, CRMReportKeys.TIME_PERIOD_FIELD_ID);
            final String functionName = resolveTimePeriodFunctionName(orgId, timePeriodFieldId);
            final Integer moduleId = optNullableInt(fields, CRMReportKeys.MODULE_ID);
            final Integer subModuleId = optNullableInt(fields, CRMReportKeys.SUB_MODULE_ID);
            final boolean isAnalyticsField = Objects.isNull(crmFieldId);
            final AnalyticsField analyticsField = isAnalyticsField ? analyticsFieldService.getAnalyticsFieldById(systemModularField.getAnaFieldId()) : null;

            final ReportField reportField = getReportField(orgId, selectorFieldId, accessTime, order,
                    getOperationType(fields, mapperField), axis == 1L ? Axis.X_AXIS : Axis.Y_AXIS,
                    fieldType, fields.optString(CRMViewPropertyKeys.LABEL, ""), isAnalyticsField
                            ? fieldTypeService.get(analyticsField.getFieldTypeId()).storeType().getType()
                            : resolvedFieldInfo.storeType().getType(), from, to, mapperField.getGroupByFieldId(),
                    reportFieldId, functionName, timePeriodFieldId, moduleId, subModuleId, mapperField.getCaseWhenConfig(),
                    Objects.nonNull(resolvedFieldInfo) ? resolvedFieldInfo.aliasName() : null, configAsString,
                    systemModularField.getModularFieldConfig().isPreCalculated());
            reportFields.add( reportField );
        }

        return new CRMAnaFieldMeta(rootTableId, selectorFieldId);
    }

    /**
     * <p>
     * Sets either a {@link FilterField} or a {@link ReportField} for a custom modular field based on field type
     * and adds it to the corresponding collection.
     * </p>
     *
     * @param orgId             the organization ID
     * @param accessTime        the timestamp representing when the data is accessed
     * @param order             the order in which the field appears in the report
     * @param reportFieldId     the report modular field ID
     * @param isFilterField     whether the field is a filter field
     * @param fields            the JSON object containing field properties
     * @param axis              the axis (X or Y) along which the field is plotted
     * @param mapperField       the {@link CRMCustomModularFieldColumnMapping} containing mapping data
     * @param reportFields      the collection to which created {@link ReportField} will be added
     * @param filterFields      the collection to which created {@link FilterField} will be added
     * @param involvedColumnIds the collection of involved column IDs to populate
     * @param resolvedFieldInfo the resolved {@link FieldInfo} for the CRM field (may be null)
     * @param reportFieldMap    the map of report field IDs to {@link ReportModularField}
     * @return the {@link CRMAnaFieldMeta} containing root table ID and actual column ID
     * @throws Exception if any error occurs during field processing
     */
    private CRMAnaFieldMeta setCustomSelectorField( final @NonNull Long orgId,
                                         final @NonNull Long accessTime,
                                         final @NonNull Long order,
                                         final @NonNull Long reportFieldId,
                                         final boolean isFilterField,
                                         final @NonNull JsonObject fields,
                                         final @NonNull Long axis,
                                         final @NonNull CRMCustomModularFieldColumnMapping mapperField,
                                         final @NonNull Collection<ReportField> reportFields,
                                         final @NonNull Collection<FilterField> filterFields, final Collection<Long> involvedColumnIds,
                                         final FieldInfo resolvedFieldInfo,
                                         final @NonNull Map<Long, ReportModularField> reportFieldMap) throws Exception {
        final Long selectorFieldId = mapperField.getColumnId();
        final ColumnDetail columnInfo = columnDetailsService.get(selectorFieldId);
        involvedColumnIds.add(selectorFieldId);
        assert columnInfo != null;
        final Long rootTableId = columnInfo.getTableDetail().id();
        final ReportModularField reportModField = reportFieldMap.get(reportFieldId);
        final CRMCustomModularField customField = reportModField != null ? reportModField.getCustomModularField() : null;
        final Long crmFieldId = customField != null ? customField.getCrmFieldId() : null;
        final String customFieldConfig = reportModField.getIsActive() && Objects.nonNull(resolvedFieldInfo) ? new AnalyticsCustomFieldConfig(resolvedFieldInfo.columnIndex()).toJsonAsString() : null;

        if (isFilterField) {
            final String functionName = resolveFilterFunctionName(fields);

            final FilterField filterField = fields.containsKey( CRMViewPropertyKeys.VALUES )
                    ? extractFilterField( orgId, fields, columnInfo, selectorFieldId, selectorFieldId,
                    accessTime, order, functionName, null, reportFieldId, null, customFieldConfig, resolvedFieldInfo, reportFieldMap)
                    : generateFilterField( orgId, columnInfo, selectorFieldId, selectorFieldId, order, accessTime,
                    null, null, fields, null, functionName,
                    null, reportFieldId, null, customFieldConfig, resolvedFieldInfo, reportFieldMap);
            filterFields.add( filterField );
        }
        else
        {
            final FieldCategory fieldType = columnInfo.getDataType() != NUMERICAL ? FieldCategory.CATEGORICAL : FieldCategory.AGGREGATE;
            final Long from = optBucketValue(fields, CommonKeys.FROM);
            final Long to = optBucketValue(fields, CommonKeys.TO);

            final Long timePeriodFieldId = optNullableLong(fields, CRMReportKeys.TIME_PERIOD_FIELD_ID);
            final String functionName = resolveTimePeriodFunctionName(orgId, timePeriodFieldId);
            final Integer moduleId = optNullableInt(fields, CRMReportKeys.MODULE_ID);
            final Integer subModuleId = optNullableInt(fields, CRMReportKeys.SUB_MODULE_ID);

            final ReportField reportField = getReportField(orgId, selectorFieldId, accessTime, order,
                    getOperationType(fields, null),
                    axis == 1L ? Axis.X_AXIS : Axis.Y_AXIS,
                    fieldType, fields.optString(CRMViewPropertyKeys.LABEL, ""), Objects.nonNull(resolvedFieldInfo) ? resolvedFieldInfo.storeType().getType() : null,
                    from, to, null,
                    reportFieldId, functionName, timePeriodFieldId, moduleId, subModuleId,
                    null, Objects.nonNull(resolvedFieldInfo) ? resolvedFieldInfo.aliasName() : null, customFieldConfig,
                    false);
            reportFields.add( reportField );
        }

        return new CRMAnaFieldMeta(rootTableId, selectorFieldId);
    }

    /**
     * Extracts a {@link FilterField} from the given JSON input containing filter values.
     * Handles both single and multivalued filter scenarios from the {@code FILTER_VALUES} key in the provided {@code fields} JSON.
     *
     * @return a fully constructed {@link FilterField} instance ready to be applied to the query
     */
    private FilterField extractFilterField( final Long orgId,
                                            final JsonObject fields,
                                            final ColumnDetail columnInfo,
                                            final Long fieldId,
                                            final Long selectorFieldId,
                                            final Long accessTime,
                                            final Long order,
                                            final String functionName,
                                            final Integer havingOperationType,
                                            final Long modularField,
                                            final String configAsString,
                                            final String customFieldConfigAsString,
                                            final FieldInfo resolvedFieldInfo,
                                            final @NonNull Map<Long, ReportModularField> reportFieldMap) throws Exception {

        final JsonArray jsonArray = fields.getJsonArray( CRMViewPropertyKeys.FILTER_VALUES );
        final int totalSize = jsonArray.size();

        Object filterValue = null;
        Collection<Object> filterValues = null;
        Long from = null;
        Long to = null;

        Long crmFieldId = null;
        FieldInfo crmFieldInfo = null;
        final ReportModularField reportModularField = reportFieldMap.get(modularField);
        final ModularField resolvedField = reportModularField.getModularField();
        if (Objects.nonNull(resolvedField)) {
            crmFieldId = resolvedField.getCrmFieldId();
        }
        if (Objects.nonNull(crmFieldId)) {
            crmFieldInfo = resolvedFieldInfo;
        }

        if (totalSize == 1) {
            final JsonObject jsonObject = jsonArray.getJsonObject(0);
            filterValue = jsonObject.opt(CRMViewPropertyKeys.FILTER_VALUE);

            if (Objects.nonNull(crmFieldInfo) && Objects.nonNull(filterValue)) {
                filterValue = resolveFilterValue(orgId, crmFieldId, crmFieldInfo, filterValue);
            }

            if (filterValue instanceof Map<?, ?> map) {
                from = NumberUtils.parseNumber(map.get(CommonKeys.FROM)).longValue();
                to = NumberUtils.parseNumber(map.get(CommonKeys.TO)).longValue();
            }
        } else {
            filterValues = new ArrayList<>(totalSize);

            for (int i = 0; i < totalSize; i++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(i);
                Object value = jsonObject.opt(CRMViewPropertyKeys.FILTER_VALUE);

                if (value instanceof Map<?, ?> map) {
                    from = NumberUtils.parseNumber(map.get(CommonKeys.FROM)).longValue();
                    to = NumberUtils.parseNumber(map.get(CommonKeys.TO)).longValue();
                }

                if (Objects.nonNull(crmFieldInfo) && Objects.nonNull(value)) {
                    value = resolveFilterValue(orgId, crmFieldId, crmFieldInfo, value);
                }

                filterValues.add(value);
            }
        }

        final Object filterFieldValue = ( Objects.nonNull( filterValues ) ) ? filterValues : filterValue;

        return generateFilterField( orgId, columnInfo, fieldId, selectorFieldId, order, accessTime,
                from, to, fields, filterFieldValue, functionName, havingOperationType,
                modularField, configAsString, customFieldConfigAsString, resolvedFieldInfo, reportFieldMap );
    }

    /**
     * Resolves a filter value from its numeric ID to the corresponding display name
     * based on the field's {@link StoreType}.
     * <p>
     * For selector types (DROPDOWN, MULTI_SELECT), delegates to {@link #resolveDropdownValue}.
     * For entity types (TAGS, TERRITORY, COMPANY, CONTACT, DEAL), delegates to {@link #resolveEntityValue}.
     * </p>
     *
     * @param orgId        the organization ID
     * @param crmFieldId   the CRM field ID
     * @param crmFieldInfo the {@link FieldInfo} of the CRM field
     * @param rawValue     the raw filter value (numeric ID) to resolve
     * @return the resolved display name if resolution succeeds, otherwise the original value
     */
    private Object resolveFilterValue ( final Long orgId, final Long crmFieldId,
        final FieldInfo crmFieldInfo, final Object rawValue){
            final StoreType storeType = crmFieldInfo.storeType();

            if (StoreType.isSelectorType(storeType)) {
                return resolveDropdownValue(orgId, crmFieldId, crmFieldInfo, rawValue);
            } else if (storeType.isEntityType() || StoreType.TAGS == storeType
                    || StoreType.TERRITORY == storeType || StoreType.DEAL == storeType) {
                return resolveEntityValue(orgId, storeType, rawValue);
            } else if (storeType == StoreType.MONETARY) {
                return rawValue.toString();
            }
            return rawValue;
        }

        /**
         * Resolves a dropdown filter value from its numeric ID to the corresponding display name.
         * <p>
         * For hidden dropdown types (lifecycle status/stage, pipeline, pipeline stage), dedicated managers are used.
         * For regular dropdown/multi-select fields, {@link OptionService} is used to resolve the option name.
         * </p>
         *
         * @param orgId        the organization ID
         * @param crmFieldId   the CRM field ID
         * @param crmFieldInfo the {@link FieldInfo} of the CRM field
         * @param rawValue     the raw filter value (numeric ID) to resolve
         * @return the resolved display name if resolution succeeds, otherwise the original value
         */
        private Object resolveDropdownValue ( final Long orgId, final Long crmFieldId,
        final FieldInfo crmFieldInfo, final Object rawValue){
            try {
                final long optionId = NumberUtils.parseNumber(rawValue).longValue();
                final DropdownType dropdownType = crmFieldInfo.presetConfig().dropdownType();

                if (DropdownType.HIDDEN_OPTION == dropdownType) {
                    final String aliasName = crmFieldInfo.aliasName();
                    return switch (aliasName) {
                        case "lifeCycleStatus" -> lifeCycleStatusManager.get(orgId, optionId).getName();
                        case "lifeCycleStage" -> lifeCycleStageManager.get(orgId, optionId).getName();
                        case "pipeline" -> pipelineManager.get(orgId, optionId).getName();
                        case "pipelineStage" -> pipelineStageManager.getPipelineStageName(orgId, optionId);
                        case "status" ->
                                Deal.State.getStateById(Integer.parseInt(String.valueOf(optionId))).getDisplayName().toLowerCase();
                        case "typeId" -> activityTypeManager.get(orgId, optionId).getName();
                        default -> rawValue;
                    };
                } else {
                    final Option option = optionService.get(orgId, crmFieldId, optionId);
                    if (Objects.nonNull(option)) {
                        return option.getOption();
                    } else {
                        return switch (crmFieldInfo.aliasName()) {
                            case "status" -> Status.get((int) optionId).getDisplayName();
                            case "priority" -> Priority.get((int) optionId).getDisplayName();
                            case "availability" -> Availability.get((int) optionId).getDisplayName();
                            default -> rawValue;
                        };
                    }
                }
            } catch (final Exception e) {
                LOGGER.warn("Analytics: Field Converter - Failed to resolve dropdown value for orgId: {}, fieldId: {}, value: {}",
                        orgId, crmFieldId, rawValue, e);
            }
            return rawValue;
        }

        /**
         * Resolves an entity-type filter value from its numeric ID to the corresponding display name.
         * <p>
         * Handles TAGS, TERRITORY, COMPANY, CONTACT, and DEAL store types by fetching the
         * entity name from its respective manager.
         * </p>
         *
         * @param orgId     the organization ID
         * @param storeType the {@link StoreType} of the field
         * @param rawValue  the raw filter value (numeric ID) to resolve
         * @return the resolved entity name if resolution succeeds, otherwise the original value
         */
        private Object resolveEntityValue ( final Long orgId, final StoreType storeType, final Object rawValue){
            try {
                final long id = NumberUtils.parseNumber(rawValue).longValue();
                return switch (storeType) {
                    case TAGS -> tagManager.partialGet(orgId, id).getName();
                    case TERRITORY -> territoryManager.get(orgId, id).getName();
                    case DEAL -> dealManager.partialGet(orgId, id).getTitle();
                    default -> rawValue;
                };
            } catch (final Exception e) {
                LOGGER.warn("Analytics: Field Converter - Failed to resolve entity value for orgId: {}, storeType: {}, value: {}",
                        orgId, storeType, rawValue, e);
            }
            return rawValue;
        }

        /**
         * Retrieves the {@link OperationType} into the {@link JsonObject}
         *
         * @param fieldsJson the {@link JsonObject} containing potential operation identifiers
         * @return the {@link OperationType} details
         */
        private OperationType getOperationType ( final JsonObject fieldsJson,
        final CRMSystemModularFieldColumnMapping fieldColumnMapping )
        {
            final Integer operationTypeId = Objects.isNull(fieldColumnMapping) ? null : fieldColumnMapping.getOperationTypeId();
            final boolean hasMeasureId = fieldsJson.containsKey(CRMReportKeys.MEASURE_ID);

            if (Objects.nonNull(operationTypeId)) {
                return OperationType.getOperationType(operationTypeId);
            } else if (hasMeasureId) {
                return OperationType.getOperationType(fieldsJson.getInt(CRMReportKeys.MEASURE_ID));
            } else if (fieldsJson.containsKey(CRMViewPropertyKeys.TIME_UNIT_ID)) {
                return OperationType.getOperationType(fieldsJson.getInt(CRMViewPropertyKeys.TIME_UNIT_ID));
            } else {
                return OperationType.ACTUAL;
            }
        }

        /**
         * Creates a {@link FilterField} object based on the provided mapping and adds it to the given filterFields collection.
         *
         * @param orgId         The organization ID for which the filter field is being created.
         * @param accessTime    The timestamp representing when the data is accessed.
         * @param order         The order in which the filter field appears in the report.
         * @param mapperField   The {@link FieldColumnMapping} object containing the necessary field mapping data.
         * @param filterFields  The collection to which the created {@link FilterField} will be added
         */
        private void createFilterField( final @NonNull Long orgId,
                                    final @NonNull Long accessTime,
                                    final @NonNull Long order,
                                    final @NonNull Long reportFieldId,
                                    final @NonNull CRMSystemModularFieldColumnMapping mapperField,
                                    final @NonNull Collection<FilterField> filterFields,
                                    final String customFieldConfig,
                                    final Collection<Long> involvedColumIds,
                                    final FieldInfo resolvedFieldInfo) throws Exception {
        final Long anaColumnId = mapperField.getFilterFieldId();
        involvedColumIds.add(anaColumnId);
        final ColumnDetail columnDetail = columnDetailsService.get( anaColumnId );
        Object filterValue;
        assert columnDetail != null;
        if( Objects.nonNull( mapperField.getFilterValue() ) )
        {
            filterValue = mapperField.getFilterValue();
        }
        else
        {
            filterValue = mapperField.getCrmFieldValue();
        }
        final CRMSystemModularField modularField = systemModularFieldService.get(mapperField.getModularFieldId());
        final Long crmFieldId = modularField.getCrmFieldId();
        final boolean isAnalyticsField = Objects.isNull(crmFieldId);
        final AnalyticsField analyticsField = isAnalyticsField ? analyticsFieldService.getAnalyticsFieldById(modularField.getAnaFieldId()) : null;

        final FilterField filterField = crmReportsBuilder.getFilterField(orgId, anaColumnId, anaColumnId, accessTime,
                accessTime, order,
                OperationType.ACTUAL.getTypeCode(),
                STRING.getTypeCode() == columnDetail.getDataType().getTypeCode()
                                        ? Filter.TEXT.getTypeCode()
                                        : Filter.NUMERIC.getTypeCode(),
                OperatorType.IS.getTypeCode(),
                isAnalyticsField ? fieldTypeService.get(analyticsField.getFieldTypeId()).storeType().getType()
                        : resolvedFieldInfo.storeType().getType(),
                filterValue, null, null, null, false,
                reportFieldId, null, mapperField.getConfigAsString(),
                Objects.nonNull(resolvedFieldInfo) ? resolvedFieldInfo.aliasName() : null, customFieldConfig);
        filterFields.add( filterField );
    }

    /**
     * Retrieves the {@link ReportField} entity.
     *
     * @param orgId         Represents the organization ID.
     * @param columnId      The column ID that identifies the field.
     * @param accessTime    The timestamp representing when the data is accessed.
     * @param order         The order in which the field appears in the report.
     * @param operationType The operation type (e.g., SUM, COUNT) to apply on the field.
     * @param axis          The axis (X or Y) along which the field data is plotted.
     * @param fieldCategory     The type of field (e.g., CATEGORICAL, NUMERICAL).
     * @param label         A string label for the report field.
     * @return the {@link ReportField} object.
     */
    @NonNull
    private ReportField getReportField ( final @NonNull Long orgId,
            final @NonNull Long columnId,
            final @NonNull Long accessTime,
            final @NonNull Long order,
            final @NonNull OperationType operationType,
            final @NonNull Axis axis,
            final @NonNull FieldCategory fieldCategory,
            final @NonNull String label,
            final Integer storeTypeId,
            final Long from,
            final Long to,
            final Long groupByColumnId,
            final Long modularFieldId,
            final String functionName, final Long timePeriodId, final Integer moduleId, final Integer subModuleId,
            final String caseWhenConfigAsString,
            final String fieldName,
            final String customFieldConfig,
            final boolean isPreCalculated)
            {
                return crmReportsBuilder.getReportField(orgId, columnId,
                        accessTime, accessTime, order,
                        operationType.getTypeCode(),
                        axis.getType(),
                        fieldCategory.getTypeCode(),
                        label, storeTypeId, from, to, groupByColumnId, modularFieldId, functionName, timePeriodId, moduleId, subModuleId, caseWhenConfigAsString, fieldName, customFieldConfig, isPreCalculated);
            }

            /**
             * <p>
             * Generates a {@link FilterField} based on various parameters and the column's data type
             * </p>
             *
             * @param orgId           Represents an organisation id
             * @param columnDetail    The details of the column, including its data type, for selecting appropriate filter types
             * @param fieldId         The field id to be used in the filter
             * @param selectorFieldId The selector field id, which may affect the filter criteria
             * @param order           The order of the filter in relation to other filters
             * @param accessTime      The access time for the filter, used for tracking when the filter is applied
             * @param from            The starting range value (used for date ranges)
             * @param to              The ending range value (used for date ranges)
             * @param fieldJson       The JSON object representing field-specific metadata, including comparator id
             * @param filterValue     The value to filter on, which could be a text, numeric, or date value
             * @param functionName    The name of the function to apply, if applicable
             * @param havingOperationType Represents the {@link OperationType} ID.
             * @return The generated {@link FilterField} with the appropriate filter details
             */
            private FilterField generateFilterField(final Long orgId, final ColumnDetail columnDetail,
                                            final Long fieldId, final Long selectorFieldId, final Long order,
                                            final Long accessTime, final Long from, final Long to,
                                            final JsonObject fieldJson, final Object filterValue, final String functionName,
                                            final Integer havingOperationType, final Long modularFieldId, final String configAsString,
                                            final String customFieldConfig, final FieldInfo resolvedFieldInfo,
                                            final @NonNull Map<Long, ReportModularField> reportFieldMap) {
        final int operationId = OperationType.ACTUAL.getTypeCode();
        final Integer comparatorId = fieldJson.containsKey(CRMViewPropertyKeys.COMPARATOR_ID)
                ? fieldJson.getInt( CRMViewPropertyKeys.COMPARATOR_ID )
                : OperatorType.IS.getTypeCode();

        final Long crmFieldId;
        final ReportModularField reportModularField = reportFieldMap.get(modularFieldId);
        final ModularField resolvedModField = reportModularField.getModularField();
        final CRMSystemModularField systemModularField = (!reportModularField.isCustom() && resolvedModField instanceof CRMSystemModularField sys) ? sys : null;
        crmFieldId = Objects.nonNull(resolvedModField) ? resolvedModField.getCrmFieldId() : null;

        final boolean isAnalyticsField = Objects.isNull(crmFieldId);
        final AnalyticsField analyticsField = isAnalyticsField ? analyticsFieldService.getAnalyticsFieldById(systemModularField.getAnaFieldId()) : null;
        final StoreType storeType;
        final FieldInfo fieldInfo;

        if (isAnalyticsField) {
            storeType = fieldTypeService.get(analyticsField.getFieldTypeId()).storeType();
            fieldInfo = null;
        } else {
            fieldInfo = resolvedFieldInfo;
            storeType = fieldInfo.storeType();
        }

        final Integer filterType = switch (columnDetail.getDataType(storeType)) {
            case STRING -> Filter.TEXT.getTypeCode();
            case NUMERICAL, DATE -> {
                if (Objects.nonNull(from) && Objects.nonNull(to)) {
                    yield Filter.RANGE.getTypeCode();
                } else {
                    yield Filter.NUMERIC.getTypeCode();
                }
            }
        };

        return crmReportsBuilder.getFilterField(orgId, fieldId, selectorFieldId, accessTime, accessTime, order, operationId,
                filterType, comparatorId, storeType.getType(), filterValue, from, to, functionName, true,
                modularFieldId, havingOperationType, configAsString, Objects.isNull(fieldInfo) ? null : fieldInfo.aliasName(), customFieldConfig);
    }

    /**
     * Carries the {@code rootTableId} and {@code actualColumnId} produced while
     * resolving a single selector mapping. A fresh instance is returned per
     * call so that no state is shared between fields, loop iterations, or
     * concurrent saves.
     */
    private record CRMAnaFieldMeta(Long rootTableId, Long actualColumnId) {}

    /**
     * Builds an {@link ANAField} from the collected report/filter fields and metadata.
     *
     * @param fieldId       the report modular field ID
     * @param columnIds     the collected column IDs
     * @param reportFields  the collected report fields
     * @param filterFields  the collected filter fields
     * @param isFilterField whether this is a filter field
     * @param fieldObject   the JSON object containing field properties
     * @param lastMeta      the metadata captured from the last selector mapping (may be null)
     * @return the constructed {@link ANAField}
     */
    private ANAField buildANAField ( final Long fieldId,
                final Collection<Long> columnIds,
                final Collection<ReportField> reportFields,
                final Collection<FilterField> filterFields,
                final boolean isFilterField,
                final JsonObject fieldObject,
                final CRMAnaFieldMeta lastMeta){
                    final ANAField.ANAFieldBuilder builder = ANAField.builder()
                            .reportFields(reportFields)
                            .filterFields(filterFields)
                            .rootTableId(lastMeta != null ? lastMeta.rootTableId() : null)
                            .crmFieldId(fieldId)
                            .anaColumnId(lastMeta != null ? lastMeta.actualColumnId() : null)
                            .columnIds(columnIds);

                    if (isFilterField) {
                        builder.operatorType(OperatorType.getOperatorType(OperatorType.IS.getTypeCode()));
                    } else if (fieldObject.containsKey(CRMReportKeys.MEASURE_ID)) {
                        builder.operationType(OperationType.getOperationType(fieldObject.getInt(CRMReportKeys.MEASURE_ID)));
                    }

                    return builder.build();
                }

                /**
                 * Returns a Long value from the JSON object if the key exists, otherwise null.
                 *
                 * @param json the JSON object
                 * @param key  the key to look up
                 * @return the Long value or null
                 */
                private static Long optNullableLong ( final JsonObject json, final String key){
                    return json.containsKey(key) ? json.getLong(key) : null;
                }

                /**
                 * Returns an Integer value from the JSON object if the key exists, otherwise null.
                 *
                 * @param json the JSON object
                 * @param key  the key to look up
                 * @return the Integer value or null
                 */
                private static Integer optNullableInt ( final JsonObject json, final String key){
                    return json.containsKey(key) ? json.getInt(key) : null;
                }

                /**
                 * Extracts a bucket boundary value (FROM or TO) from the field's BUCKET JSON, if present.
                 *
                 * @param fields    the JSON object containing field properties
                 * @param boundKey  the boundary key ({@link CommonKeys#FROM} or {@link CommonKeys#TO})
                 * @return the bucket boundary value, or null if not present
                 */
                private static Long optBucketValue ( final JsonObject fields, final String boundKey){
                    if (fields.containsKey(CRMViewPropertyKeys.BUCKET)) {
                        final JsonObject bucketJson = fields.getJsonObject(CRMViewPropertyKeys.BUCKET);
                        return bucketJson.containsKey(boundKey) ? bucketJson.getLong(boundKey) : null;
                    }
                    return null;
                }

                /**
                 * Resolves the filter function name from the FUNCTION_ID in the field JSON, if present.
                 *
                 * @param fields the JSON object containing field properties
                 * @return the lowercase function name, or null if no FUNCTION_ID is present
                 */
                private static String resolveFilterFunctionName ( final JsonObject fields){
                    if (fields.containsKey(CRMReportKeys.FUNCTION_ID)) {
                        return CRMFunction.getFunctionById(fields.getInt(CRMReportKeys.FUNCTION_ID)).name().toLowerCase();
                    }
                    return null;
                }

                /**
                 * Resolves the time period function name for the given time period field ID.
                 *
                 * @param orgId            the organization ID
                 * @param timePeriodFieldId the time period field ID (may be null)
                 * @return the function name, or null if timePeriodFieldId is null
                 */
                private String resolveTimePeriodFunctionName ( final Long orgId, final Long timePeriodFieldId){
                    return Objects.isNull(timePeriodFieldId) ? null
                            : CRMFunction.getFunctionById(crmManager.getTimePeriodFieldManager()
                                                       .get(orgId, timePeriodFieldId).getFunctionId()).getFunctionName();
                }

            }
