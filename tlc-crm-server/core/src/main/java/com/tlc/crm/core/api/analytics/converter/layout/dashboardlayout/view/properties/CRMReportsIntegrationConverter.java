package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.properties;

import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.chart.Axis;

import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.entity.config.function.FieldPeriodicFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAFieldsWrapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Duration;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Metrics;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.ViewBy;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.CustomFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.PeriodicFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRMapperColumn;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRViewFieldMapper;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.commons.code.ErrorCode;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides common functionality for persisting CRM view field integration metadata
 * (field mappers, bucket units, periodic filters) into the data container.
 *
 * @author jamesh
 * @version 1.0
 */
public class CRMReportsIntegrationConverter
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CRMReportsIntegrationConverter.class );

    private final CRMReportManagerAPI crmReportManagerAPI;

    protected CRMReportsIntegrationConverter()
    {
        crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
    }

    /**
     * Persists all view field integration metadata for a given analytics view — including
     * view-by / duration fields, metric fields, custom filters, and periodic filters.
     *
     * @param orgId          the organization ID
     * @param viewId         the analytics view ID
     * @param crmChildViewId the CRM child view ID
     * @param reportFields   the report fields from the analytics model
     * @param filterFields   the filter fields from the analytics model
     * @param fieldsWrapper  the fields wrapper containing metrics, duration, and view-by
     * @param viewType       the CRM view type (KPI or chart)
     * @param dataContainer  the data container to load rows into
     */
    public void persistFields( final @NonNull Long orgId,
                               final Long viewId,
                               final @NonNull Long crmChildViewId,
                               final Collection<ReportField> reportFields,
                               final Collection<FilterField> filterFields,
                               final ANAFieldsWrapper fieldsWrapper,
                               final CRMViewType viewType,
                               final DataContainer dataContainer )
    {
        final int order = fieldsWrapper.getOrder();
        final Map<Long, ReportField> reportFieldMap = buildReportFieldMap( reportFields );
        final Map<Long, FilterField> filterFieldMap = buildFilterFieldMap( filterFields );

        if( !viewType.equals( CRMViewType.KPI ) )
        {
            persistViewByFields( viewId, crmChildViewId, orgId, order, fieldsWrapper.getViewBy(),
                                 reportFields, filterFields, reportFieldMap, filterFieldMap, dataContainer );
        }
        else
        {
            persistDurationField( viewId, crmChildViewId, orgId, order, fieldsWrapper.getDuration(),
                                  reportFields, filterFields, reportFieldMap, filterFieldMap, dataContainer );
        }

        persistMetricFields( viewId, crmChildViewId, orgId, order, fieldsWrapper.getMetrics(),
                             reportFields, filterFields, reportFieldMap, filterFieldMap, dataContainer );
    }

    /**
     * Persists view-by field mappers for non-KPI chart views.
     */
    private void persistViewByFields( final Long viewId,
                                      final Long crmChildViewId,
                                      final Long orgId,
                                      final int order,
                                      final Collection<ViewBy> viewByList,
                                      final Collection<ReportField> reportFields,
                                      final Collection<FilterField> filterFields,
                                      final Map<Long, ReportField> reportFieldMap,
                                      final Map<Long, FilterField> filterFieldMap,
                                      final DataContainer dataContainer )
    {
        for( final ViewBy viewBy : viewByList )
        {
            if( viewBy.isDuplicate() )
            {
                continue;
            }

            final FieldBucketUnit fieldBucketUnit = viewBy.getFieldBucketUnit();
            final ANAField anaField = viewBy.getMetricsInfo();

            saveNonFilterMeta( viewId, anaField.getCrmFieldId(), crmChildViewId,
                               anaField.getAnaColumnId(),
                               resolveMapperColumns( anaField, reportFields, filterFields, reportFieldMap, filterFieldMap ),
                               orgId, getTypeCode( anaField.getOperationType() ), null,
                               fieldBucketUnit, viewBy.isBucketApplied(), Objects.nonNull( fieldBucketUnit ),
                               order, viewBy.isPeriodicFilterField(), Axis.X_AXIS.getType(), dataContainer );
        }
    }

    /**
     * Persists the duration field mapper for KPI views.
     */
    private void persistDurationField( final Long viewId,
                                       final Long crmChildViewId,
                                       final Long orgId,
                                       final int order,
                                       final Duration duration,
                                       final Collection<ReportField> reportFields,
                                       final Collection<FilterField> filterFields,
                                       final Map<Long, ReportField> reportFieldMap,
                                       final Map<Long, FilterField> filterFieldMap,
                                       final DataContainer dataContainer )
    {
        final ANAField anaField = duration.getMetricsInfo();

        saveNonFilterMeta( viewId, anaField.getCrmFieldId(), crmChildViewId,
                           anaField.getAnaColumnId(),
                           resolveMapperColumns( anaField, reportFields, filterFields, reportFieldMap, filterFieldMap ),
                           orgId, getTypeCode( anaField.getOperationType() ), null,
                           null, false, false,
                           order, false, Axis.X_AXIS.getType(), dataContainer );
    }

    /**
     * Persists metric field mappers along with their associated custom and periodic filters.
     */
    private void persistMetricFields( final Long viewId,
                                      final Long crmChildViewId,
                                      final Long orgId,
                                      final int order,
                                      final Metrics metrics,
                                      final Collection<ReportField> reportFields,
                                      final Collection<FilterField> filterFields,
                                      final Map<Long, ReportField> reportFieldMap,
                                      final Map<Long, FilterField> filterFieldMap,
                                      final DataContainer dataContainer )
    {
        if( metrics.isDuplicate() )
        {
            return;
        }

        final ANAField anaField = metrics.getMetricsInfo();
        final Long fieldId = anaField.getCrmFieldId();
        final Long comparatorId = getTypeCode( anaField.getOperationType() );

        saveNonFilterMeta( viewId, fieldId, crmChildViewId,
                           anaField.getAnaColumnId(),
                           resolveMapperColumns( anaField, reportFields, filterFields, reportFieldMap, filterFieldMap ),
                           orgId, null, comparatorId,
                           null, false, false,
                           order, false, Axis.Y_AXIS.getType(), dataContainer );

        final Collection<CustomFilter> customFilters = metrics.getCustomFilters();

        for( final CustomFilter customFilter : customFilters)
        {
            persistFilterEntries( viewId, crmChildViewId, orgId, order, fieldId,
                                  customFilter.getMetricsInfo(), false,
                                  reportFields, filterFields, reportFieldMap, filterFieldMap,
                                  AnalyticsErrorCodes.FILTER_CREATE_ACTION_FAILED,
                                  "i18n.analytics.filter.create.action.failed",
                                  dataContainer );
        }

        final Collection<PeriodicFilter> periodicFilters = metrics.getPeriodicFilter();

        for( final PeriodicFilter periodicFilter : periodicFilters)
        {
            persistFilterEntries( viewId, crmChildViewId, orgId, order, fieldId,
                                  periodicFilter.getMetricsInfo(), true,
                                  reportFields, filterFields, reportFieldMap, filterFieldMap,
                                  AnalyticsErrorCodes.PERIODIC_FILTER_CREATE_ACTION_FAILED,
                                  "i18n.analytics.periodic.filter.create.action.failed",
                                  dataContainer );
        }
    }

    /**
     * Persists filter entries (either custom or periodic) for a metric's filter ANAField.
     * Shared logic for both custom filter and periodic filter processing.
     *
     * @param viewId                the analytics view ID
     * @param crmChildViewId        the CRM child view ID
     * @param orgId                 the organization ID
     * @param metricsOrder          the metric order
     * @param yFieldId              the parent metric's field ID (Y-axis reference)
     * @param filterANAField        the filter's ANAField containing filter metadata
     * @param isPeriodFilterApplied whether this is a periodic filter
     * @param reportFields          the report fields from the analytics model
     * @param filterFields          the filter fields from the analytics model
     * @param reportFieldMap        pre-built report field lookup map
     * @param filterFieldMap        pre-built filter field lookup map
     * @param errorCode             the error code to throw on failure
     * @param errorI18nKey          the i18n key for the error message
     * @param dataContainer         the data container to load rows into
     */
    private void persistFilterEntries( final Long viewId,
                                       final Long crmChildViewId,
                                       final Long orgId,
                                       final int metricsOrder,
                                       final Long yFieldId,
                                       final ANAField filterANAField,
                                       final boolean isPeriodFilterApplied,
                                       final Collection<ReportField> reportFields,
                                       final Collection<FilterField> filterFields,
                                       final Map<Long, ReportField> reportFieldMap,
                                       final Map<Long, FilterField> filterFieldMap,
                                       final AnalyticsErrorCodes errorCode,
                                       final String errorI18nKey,
                                       final DataContainer dataContainer )
    {
        final Collection<CRMRMapperColumn> filterMapperColumns = resolveMapperColumns( filterANAField, reportFields, filterFields, reportFieldMap, filterFieldMap );
        final Long filterFieldId = filterANAField.getCrmFieldId();
        final Long filterActualColumnId = filterANAField.getAnaColumnId();
        final Long filterComparatorId = getTypeCode( filterANAField.getOperationType() );
        final Collection<FilterField> anaFilters = filterANAField.getFilterFields();

        try
        {
            CodeBlockExecutor.forEachSafely( anaFilters, anaFilter ->
            {
                final FilterFunctionMapper filterFunctionMapper = anaFilter.getFilterFunctionMapper();
                final Integer functionId = resolveFunctionId( filterFunctionMapper );

                saveFilterMeta( viewId, filterFieldId, crmChildViewId,
                                anaFilter.isCustomFilter(), filterActualColumnId,
                                filterMapperColumns, orgId,
                                filterComparatorId, yFieldId,
                                isPeriodFilterApplied, functionId,
                                anaFilter.getOrder(), metricsOrder, dataContainer );
            });
        }
        catch( final Exception exception )
        {
            LOGGER.error( "CRM Analytics: Failed to persist filter. Reason: {}", exception.getMessage(), exception );
            throw ErrorCode.get( errorCode, errorI18nKey );
        }
    }

    /**
     * Saves a non-filter (report) field mapper and optionally its bucket unit.
     *
     * @param viewId               the analytics view ID
     * @param fieldId              the modular field ID
     * @param crmViewId            the CRM child view ID
     * @param actualColumnId       the analytics actual column ID
     * @param mapperColumns        the mapper columns
     * @param orgId                the organization ID
     * @param operationId          the operation type code (may be null)
     * @param comparatorId         the comparator type code (may be null)
     * @param fieldBucketUnit      the bucket unit (may be null)
     * @param isBucketApplied      whether bucket is applied
     * @param isUnitApplied        whether unit is applied
     * @param metricsOrder         the metric order
     * @param isPeriodicFilterField whether this is a periodic filter field
     * @param dataContainer        the data container to load rows into
     */
    private void saveNonFilterMeta( final Long viewId,
                                    final Long fieldId,
                                    final Long crmViewId,
                                    final Long actualColumnId,
                                    final Collection<CRMRMapperColumn> mapperColumns,
                                    final Long orgId,
                                    final Long operationId,
                                    final Long comparatorId,
                                    final FieldBucketUnit fieldBucketUnit,
                                    final boolean isBucketApplied,
                                    final boolean isUnitApplied,
                                    final int metricsOrder,
                                    final boolean isPeriodicFilterField,
                                    final int axis,
                                    final DataContainer dataContainer )
    {
        try
        {
            final CRMRViewFieldMapper crmViewFieldMapper = CRMRViewFieldMapper.builder()
                                                                              .crmViewId( crmViewId )
                                                                              .viewId( viewId )
                                                                              .modularFieldId( fieldId )
                                                                              .isFilterColumn( false )
                                                                              .anaActualColumnId( actualColumnId )
                                                                              .anaActualOperationId( operationId )
                                                                              .mapperColumns( mapperColumns )
                                                                              .isBuckedApplied( isBucketApplied )
                                                                              .isUnitApplied( isUnitApplied )
                                                                              .orgId( orgId )
                                                                              .order( metricsOrder )
                                                                              .isPeriodicFilterField( isPeriodicFilterField )
                                                                              .axis( axis )
                                                                              .build();

            crmReportManagerAPI.getViewFieldMapper().loadDataIntoContainer( List.of( crmViewFieldMapper ), dataContainer );
            LOGGER.info( "CRM Analytics: Successfully created CRM view field mapper for orgId: {}, fieldId: {}, columnId: {}", orgId, fieldId, actualColumnId );

            if( isUnitApplied )
            {
                final FieldBucketUnit bucket = FieldBucketUnit.builder()
                                                              .viewFieldId( crmViewFieldMapper.id() )
                                                              .orgId( orgId )
                                                              .unitId( fieldBucketUnit.getUnitId() )
                                                              .build();

                crmReportManagerAPI.getFieldBucketUnitManager().loadDataIntoContainer( List.of( bucket ), dataContainer );
                LOGGER.info( "CRM Analytics: Successfully created CRM view field bucket unit mapper for orgId: {}, fieldId: {}, columnId: {}", orgId, fieldId, actualColumnId );
            }
        }
        catch( final Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to set non-filter integration meta. Reason: {}", error.getMessage(), error );
            throw ErrorCode.get( AnalyticsErrorCodes.SET_INTEGRATION_META_ACTION_FAILED,
                                 "i18n.analytics.set.integration.meta.action.failed" );
        }
    }

    /**
     * Saves a filter field mapper and optionally its periodic filter function mapping.
     *
     * @param viewId                the analytics view ID
     * @param fieldId               the modular field ID
     * @param crmViewId             the CRM child view ID
     * @param isCustomFilter        whether this is a custom filter
     * @param actualColumnId        the analytics actual column ID
     * @param mapperColumns         the mapper columns
     * @param orgId                 the organization ID
     * @param comparatorId          the comparator type code (may be null)
     * @param yFieldId              the Y-axis field ID reference
     * @param isPeriodFilterApplied whether periodic filter is applied
     * @param functionId            the periodic filter function ID (may be null)
     * @param filterOrder           the filter order
     * @param metricsOrder          the metric order
     * @param dataContainer         the data container to load rows into
     */
    private void saveFilterMeta( final Long viewId,
                                 final Long fieldId,
                                 final Long crmViewId,
                                 final boolean isCustomFilter,
                                 final Long actualColumnId,
                                 final Collection<CRMRMapperColumn> mapperColumns,
                                 final Long orgId,
                                 final Long comparatorId,
                                 final Long yFieldId,
                                 final boolean isPeriodFilterApplied,
                                 final Integer functionId,
                                 final Long filterOrder,
                                 final int metricsOrder,
                                 final DataContainer dataContainer )
    {
        try
        {
            final CRMRViewFieldMapper crmViewFieldMapper = CRMRViewFieldMapper.builder()
                                                                              .crmViewId( crmViewId )
                                                                              .viewId( viewId )
                                                                              .modularFieldId( fieldId )
                                                                              .isFilterColumn( isCustomFilter )
                                                                              .anaActualColumnId( actualColumnId )
                                                                              .anaActualComparatorId( comparatorId )
                                                                              .anaYColumnID( yFieldId )
                                                                              .mapperColumns( mapperColumns )
                                                                              .isPeriodFilterApplied( isPeriodFilterApplied )
                                                                              .orgId( orgId )
                                                                              .filterOrder( filterOrder )
                                                                              .order( metricsOrder )
                                                                              .isPeriodicFilterField( false )
                                                                              .build();

            crmReportManagerAPI.getViewFieldMapper().loadDataIntoContainer( List.of( crmViewFieldMapper ), dataContainer );
            LOGGER.info( "CRM Analytics: Successfully created CRM filter field mapper for orgId: {}, fieldId: {}, columnId: {}", orgId, fieldId, actualColumnId );

            if( Objects.nonNull( functionId ) )
            {
                final FieldPeriodicFilter fieldPeriodicFilter = FieldPeriodicFilter.builder()
                                                                                   .orgId( orgId )
                                                                                   .viewFieldId( crmViewFieldMapper.id() )
                                                                                   .functionId( functionId )
                                                                                   .build();

                crmReportManagerAPI.getFieldPeriodicFilterManager()
                                   .loadDataIntoContainer( List.of( fieldPeriodicFilter ), dataContainer );
                LOGGER.info( "CRM Analytics: Successfully created CRM periodic filter mapper for orgId: {}, fieldId: {}, columnId: {}", orgId, fieldId, actualColumnId );
            }
        }
        catch( final Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to set filter integration meta. Reason: {}", error.getMessage(), error );
            throw ErrorCode.get( AnalyticsErrorCodes.SET_INTEGRATION_META_ACTION_FAILED,
                                 "i18n.analytics.set.integration.meta.action.failed" );
        }
    }

    // ========================= Helper methods =========================

    /**
     * Safely extracts the type code from an {@link OperationType} as a Long, returning null if the type is null.
     *
     * @param operationType the operation type (may be null)
     * @return the type code as Long, or null
     */
    private Long getTypeCode( final OperationType operationType )
    {
        return Objects.isNull( operationType ) ? null : (long) operationType.getTypeCode();
    }

    /**
     * Resolves the function ID from a {@link FilterFunctionMapper}, returning null if the mapper is null.
     *
     * @param filterFunctionMapper the function mapper (may be null)
     * @return the function ID, or null
     */
    private Integer resolveFunctionId( final FilterFunctionMapper filterFunctionMapper )
    {
        if( Objects.isNull( filterFunctionMapper ) )
        {
            return null;
        }

        return CRMFunction.getFunctionByName( filterFunctionMapper.getFunctionName().toLowerCase()).getTypeCode();
    }

    /**
     * Resolves mapper columns for a given ANAField using pre-built lookup maps.
     *
     * @param anaField       the ANAField to resolve mapper columns for
     * @param reportFields   the report fields collection
     * @param filterFields   the filter fields collection
     * @param reportFieldMap pre-built report field lookup map by order
     * @param filterFieldMap pre-built filter field lookup map by order
     * @return collection of {@link CRMRMapperColumn}
     */
    private Collection<CRMRMapperColumn> resolveMapperColumns( final @NonNull ANAField anaField,
                                                               final Collection<ReportField> reportFields,
                                                               final Collection<FilterField> filterFields,
                                                               final Map<Long, ReportField> reportFieldMap,
                                                               final Map<Long, FilterField> filterFieldMap )
    {
        final Collection<CRMRMapperColumn> mapperFields = new ArrayList<>();
        final Long actualFieldId = anaField.getAnaColumnId();
        final Collection<ReportField> reportFieldsList = anaField.getReportFields();

        try
        {
            for( final ReportField mapperRepField : reportFieldsList)
            {
                final ReportField reportField = reportFieldMap.get( mapperRepField.getOrder() );

                if( Objects.nonNull( reportField ) )
                {
                    final Long reportColumnId = reportField.getColumnId();

                    if( !Objects.equals( reportColumnId, actualFieldId ) )
                    {
                        mapperFields.add( CRMRMapperColumn.builder()
                                                          .isFilterColumn( false )
                                                          .anaMapperColumn( reportColumnId )
                                                          .build() );
                    }
                }
            }

            if( Objects.nonNull( filterFields ) )
            {
                final Collection<FilterField> filterFieldsList = anaField.getFilterFields();

                for( final FilterField mapperFilField : filterFieldsList)
                {
                    final FilterField filterField = filterFieldMap.get( mapperFilField.getOrder() );

                    if( Objects.nonNull( filterField ) )
                    {
                        final Long filterColumnId = filterField.getColumnId();

                        if( !Objects.equals( filterColumnId, actualFieldId ) )
                        {
                            mapperFields.add( CRMRMapperColumn.builder()
                                                              .isFilterColumn( true )
                                                              .anaMapperColumn( filterColumnId )
                                                              .build() );
                        }
                    }
                }
            }
        }
        catch( final Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to set field configuration for actualFieldId '{}'. Reason: {}", actualFieldId, error.getMessage(), error );
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_FIELD_CONFIGURATION,
                                 "i18n.analytics.invalid.field.configuration" );
        }

        return mapperFields;
    }

    /**
     * Builds a lookup map from report fields, keyed by order.
     * Uses last-wins merge strategy for duplicate orders.
     *
     * @param reportFields the report fields collection
     * @return map keyed by order
     */
    private Map<Long, ReportField> buildReportFieldMap( final Collection<ReportField> reportFields )
    {
        return reportFields.stream()
                           .collect( Collectors.toMap( ReportField::getOrder,
                                                      Function.identity(),
                                                      ( existing, current ) -> current ) );
    }

    /**
     * Builds a lookup map from filter fields, keyed by order.
     * Returns an empty map if filterFields is null.
     * Uses last-wins merge strategy for duplicate orders.
     *
     * @param filterFields the filter fields collection (may be null)
     * @return map keyed by order
     */
    private Map<Long, FilterField> buildFilterFieldMap( final Collection<FilterField> filterFields )
    {
        if( Objects.isNull( filterFields ) )
        {
            return Collections.emptyMap();
        }

        return filterFields.stream()
                           .collect( Collectors.toMap( FilterField::getOrder,
                                                      Function.identity(),
                                                      ( existing, current ) -> current ) );
    }
}
