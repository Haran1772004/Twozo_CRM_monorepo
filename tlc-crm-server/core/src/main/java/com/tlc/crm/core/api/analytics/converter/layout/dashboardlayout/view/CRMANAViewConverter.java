package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.ViewType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.GraphType;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;

import com.tlc.analytics.sql.resource.ANATABLEDETAILS;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.lookup.CRMLookUp;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.properties.CRMViewPropertyConverter;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAFieldsWrapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Duration;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Metrics;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.ViewBy;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.CustomFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.PeriodicFilter;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nResolver;
import com.tlc.sql.common.api.dml.Criteria;

import com.tlc.analytics.service.column.ColumnDetailsService;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the abstract way to converts the CRM analytics view configurations to ANA view configurations.
 * </p>
 *
 * @author james
 * @version 1.0
 */
abstract class CRMANAViewConverter<T extends AnalyticsReport> extends CRMViewResponseConverter<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CRMANAViewConverter.class );

    private final CRMViewPropertyConverter crmViewPropertyConverter;
    private final CRMLookUp crmLookUp;
    private final ColumnDetailsService columnDetailsService;

    protected CRMANAViewConverter()
    {
        this.crmViewPropertyConverter = CRMViewPropertyConverter.INSTANCE();
        this.crmLookUp = CRMLookUp.INSTANCE();
        this.columnDetailsService = ColumnDetailsService.getInstance();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into a map of model-to-{@link ANAFieldsWrapper} entries.
     * Handles both KPI and chart-type views by delegating per-metric processing to
     * {@link #buildMetricView}.
     * </p>
     *
     * @param analyticsRequest Request the {@link AnalyticsRequest} details
     * @param createdUserId    Represents the user id
     * @param createdTime      Represents the created time
     * @param modifiedTime     Represents the updated time
     * @param request          Represents the {@link JsonObject} details
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return The map of model to {@link ANAFieldsWrapper}
     */
    protected Map<T, ANAFieldsWrapper> convert( final @NonNull AnalyticsRequest analyticsRequest,
                                                final @NonNull Long createdUserId,
                                                final @NonNull Long createdTime,
                                                final @NonNull Long modifiedTime,
                                                final @NonNull JsonObject request,
                                                final @NonNull I18nResolver i18nResolver,
                                                final Map<Long, ReportModularField> reportFieldMap )
    {
        final Long orgId = analyticsRequest.orgId();
        final Long userId = analyticsRequest.userId();

        final Map<T, ANAFieldsWrapper> crmViewConfigMap = new LinkedHashMap<>();

        try
        {
            if( !request.containsKey( CRMChildViewKeys.VIEW_PROPERTIES ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.VIEW_PROPERTIES_NOT_FOUND,
                            "i18n.analytics.view.properties.not.found" );
            }

            final int viewTypeId = request.getInt(CRMReportKeys.VIEW_TYPE_ID);
            final CRMViewType viewType = CRMViewType.getCrmViewType( viewTypeId );

            final JsonObject viewProperties = request.getJsonObject( CRMChildViewKeys.VIEW_PROPERTIES );
            final JsonArray metrics = viewProperties.getJsonArray( CRMChildViewKeys.METRICS );
            final JsonArray viewArray = viewProperties.optJsonArray(CRMViewPropertyKeys.VIEW_BY);
            final JsonArray underlyingConfigArray = viewProperties.containsKey(CRMViewPropertyKeys.UNDERLYING_CONFIG) ?
                    viewProperties.getJsonArray(CRMViewPropertyKeys.UNDERLYING_CONFIG) : null;

            final String baseViewName = request.getString( CRMChildViewKeys.VIEW_NAME );
            final String description = request.optString( CRMDashboardKeys.DESCRIPTION, null );
            final Long chartUniqueId = getChartUniqueId( underlyingConfigArray );
            final Integer anaViewTypeId = getANAViewTypeId( viewType );

            if( Objects.equals( CRMViewType.KPI, viewType ) )
            {
                final Collection<Long> involvedColumnIds = new LinkedHashSet<>();
                final JsonObject durationConfig = viewProperties.getJsonObject( CRMViewPropertyKeys.DURATION );
                final Duration duration = crmViewPropertyConverter.convertDuration( orgId, userId, modifiedTime, durationConfig, involvedColumnIds, reportFieldMap );
                final int size = metrics.size();

                for(int index = NumericalConstant.ZERO; index < size; index++ )
                {
                    final JsonObject metric = metrics.getJsonObject( index );
                    final Metrics metricField = crmViewPropertyConverter.convertMetrics( orgId, userId, modifiedTime, metric, involvedColumnIds, reportFieldMap );

                    final ANAFieldsWrapper fieldsWrapper = ANAFieldsWrapper.builder()
                                                                           .metrics( metricField )
                                                                           .duration( duration )
                                                                           .order( index + NumericalConstant.ONE )
                                                                           .build();

                    buildMetricView( orgId, userId, createdUserId, createdTime, modifiedTime,
                                    metric, baseViewName, description, chartUniqueId, anaViewTypeId,
                                    viewType, fieldsWrapper, involvedColumnIds, request, crmViewConfigMap );
                }
            }
            else
            {
                for( int index = NumericalConstant.ZERO; index < metrics.size(); index++ )
                {
                    final Collection<Long> involvedColumnIds = new LinkedHashSet<>();
                    final JsonObject metric = metrics.getJsonObject( index );
                    final Metrics metricField = crmViewPropertyConverter.convertMetrics( orgId, userId, modifiedTime, metric, involvedColumnIds, reportFieldMap );

                    final Collection<ViewBy> viewByList = new ArrayList<>();

                    for( int viewIndex = 0; viewIndex < viewArray.size(); viewIndex++ )
                    {
                        final JsonObject viewByConfig = viewArray.getJsonObject( viewIndex );
                        final Integer moduleId = metric.getInt(CRMReportKeys.MODULE_ID);

                        viewByList.add( crmViewPropertyConverter.convertViewBy( orgId, userId, moduleId, modifiedTime,
                                                                                viewByConfig, metricField, index, involvedColumnIds, reportFieldMap ) );
                    }

                    final ANAFieldsWrapper fieldsWrapper = ANAFieldsWrapper.builder()
                                                                           .metrics( metricField )
                                                                           .viewBy( viewByList )
                                                                           .order( index + NumericalConstant.ONE )
                                                                           .build();

                    buildMetricView( orgId, userId, createdUserId, createdTime, modifiedTime,
                                    metric, baseViewName, description, chartUniqueId, anaViewTypeId,
                                    viewType, fieldsWrapper, involvedColumnIds, request, crmViewConfigMap );
                }
            }
        }
        catch( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to create view for orgId: {}", orgId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.VIEW_CREATE_ACTION_FAILED,
                                 "i18n.analytics.view.create.action.failed" );
        }

        return crmViewConfigMap;
    }

    /**
     * Builds a single metric view entry — resolves root table, constructs {@link ViewDetails},
     * populates fields, and adds the model to the output map. Shared by both KPI and chart branches.
     *
     * @param orgId              the organization ID
     * @param userId             the current user ID
     * @param createdUserId      the user ID who created this view
     * @param createdTime        the creation timestamp
     * @param modifiedTime       the modification timestamp
     * @param metric             the metric JSON object containing the module ID
     * @param baseViewName       the base view name to suffix with index
     * @param description        the view description (may be null)
     * @param chartUniqueId      the unique chart ID from underlying config
     * @param anaViewTypeId      the resolved ANA view type ID
     * @param viewType           the CRM view type
     * @param fieldsWrapper      the pre-built fields wrapper (with either duration or viewBy)
     * @param involvedColumnIds  the collection of involved column IDs
     * @param request            the original request JSON
     * @param crmViewConfigMap   the output map to populate
     */
    private void buildMetricView( final Long orgId,
                                  final Long userId,
                                  final Long createdUserId,
                                  final Long createdTime,
                                  final Long modifiedTime,
                                  final JsonObject metric,
                                  final String baseViewName,
                                  final String description,
                                  final Long chartUniqueId,
                                  final Integer anaViewTypeId,
                                  final CRMViewType viewType,
                                  final ANAFieldsWrapper fieldsWrapper,
                                  final Collection<Long> involvedColumnIds,
                                  final JsonObject request,
                                  final Map<T, ANAFieldsWrapper> crmViewConfigMap )
    {
        final Integer moduleId = metric.getInt(CRMReportKeys.MODULE_ID);
        final String rootTableName = crmLookUp.getRootTable( moduleId ).toLowerCase();
        final Long rootTableId = AnalyticsConfigManagerImpl.INSTANCE()
                                                         .getTableManager()
                                                         .getPKValue( orgId,
                                                                      Criteria.eq( AnalyticsConfigManagerImpl.INSTANCE()
                                                                                                           .getTableManager()
                                                                                                           .getTable()
                                                                                                           .getColumn(ANATABLEDETAILS.TABLE_NAME),
                                                                                   rootTableName ) );
        final String viewName = String.format( "%s_%d", baseViewName, fieldsWrapper.getOrder() - 1 );

        final ViewDetails viewDetails = ViewDetails.builder()
                                                   .description( description )
                                                   .displayName( viewName )
                                                   .createdBy( createdUserId )
                                                   .modifiedBy( userId )
                                                   .createdTime( createdTime )
                                                   .modifiedTime( modifiedTime )
                                                   .viewTypeId( anaViewTypeId )
                                                   .orgId( orgId )
                                                   .build();

        setConfigResponse( rootTableId, chartUniqueId, viewType, fieldsWrapper,
                           new ArrayList<>(), new ArrayList<>(), request,
                           crmViewConfigMap, viewDetails,
                           getInvolvedTableDetails( involvedColumnIds ) );
    }

    /**
     * Resolves the involved {@link TableDetail} from the given column IDs, preserving insertion order.
     *
     * @param involvedColumnIds  the collection of column IDs to resolve
     * @return an ordered collection of unique {@link TableDetail}
     */
    private Collection<TableDetail> getInvolvedTableDetails(final Collection<Long> involvedColumnIds )
    {
        final Collection<ColumnDetail> columnDetails = columnDetailsService.getAll( (Set<Long>) involvedColumnIds );
        final Map<Long, ColumnDetail> columnDetailsMap = columnDetails.stream().collect( Collectors.toMap( ColumnDetail::id, Function.identity() ) );
        final Collection<TableDetail> involvedTableDetails = new LinkedHashSet<>();

        for( final Long columnId : involvedColumnIds )
        {
            final ColumnDetail colDetails = columnDetailsMap.get( columnId );

            if( colDetails != null )
            {
                involvedTableDetails.add( colDetails.getTableDetail() );
            }
        }

        return involvedTableDetails;
    }

    /**
     * <p>
     * Gets the ANA graph type id based on the {@link CRMViewType}.
     * Since {@link CRMViewType} and {@link GraphType} share identical enum constant names,
     * the view type name is used directly for lookup.
     * </p>
     *
     * @param viewType Represents the {@link CRMViewType} details
     * @return The ANA graph type id
     */
    protected int getANAGraphTypeId(final CRMViewType viewType) {
        return GraphType.valueOf(viewType.name()).getTypeCode();
    }

    /**
     * <p>
     * Constructs and sets the response model for a given configuration.
     * </p>
     *
     * @param rootTableId        Represents the root table id
     * @param chartUniqueId      A unique id of a chart
     * @param viewType           Represents the {@link CRMViewType}
     * @param fieldsWrapper      Represents the {@link ANAFieldsWrapper} details
     * @param reportFields       Contains the collection of {@link ReportField} details
     * @param filterFields       Contains the collection of {@link FilterField} details
     * @param request            Holds the {@link JsonObject} details
     * @param crmViewConfigMap   Represents the map of model and {@link ANAFieldsWrapper}
     * @param viewDetails        Holds the {@link ViewDetails} details
     * @param involvedTableDetails Contains the collection of involved {@link TableDetail}
     */
    private void setConfigResponse( final Long rootTableId, final Long chartUniqueId, final CRMViewType viewType,
                                    final ANAFieldsWrapper fieldsWrapper, final Collection<ReportField> reportFields,
                                    final Collection<FilterField> filterFields, final JsonObject request,
                                    final Map<T, ANAFieldsWrapper> crmViewConfigMap, final ViewDetails viewDetails,
                                    final Collection<TableDetail> involvedTableDetails )
    {
        final Long orgId = viewDetails.orgId();
        final Collection<Long> columnIds = new HashSet<>();

        collectFields( fieldsWrapper, reportFields, filterFields, columnIds, viewType );

        final ReportPathMapping reportPathMapping = crmLookUp.getReportPathMapping( orgId, rootTableId, columnIds, involvedTableDetails );
        final int graphTypeId = getANAGraphTypeId(viewType);

        final T model = getModel( orgId, viewDetails.getCreatedBy(), viewDetails.getModifiedBy(),
                                  viewDetails.getCreatedTime(), viewDetails.getModifiedTime(), chartUniqueId, viewDetails, reportFields,
                                  filterFields, rootTableId, reportPathMapping,
                                  graphTypeId, request );

        crmViewConfigMap.put( model, fieldsWrapper );
    }

    /**
     * <p>
     * Constructs the {@link ReportField} and {@link FilterField} collections
     * from the given {@link ANAFieldsWrapper}.
     * </p>
     *
     * @param fieldsWrapper Represents the {@link ANAFieldsWrapper} details
     * @param reportFields  Contains the collection of {@link ReportField} details
     * @param filterFields  Contains the collection of {@link FilterField} details
     * @param columnIds     Contains the collection of column ids
     * @param viewType      Represents the {@link CRMViewType}
     */
    private void collectFields( final ANAFieldsWrapper fieldsWrapper,
                                final Collection<ReportField> reportFields,
                                final Collection<FilterField> filterFields,
                                final Collection<Long> columnIds,
                                final CRMViewType viewType )
    {
        final Metrics metric = fieldsWrapper.getMetrics();
        final ANAField anaField = metric.getMetricsInfo();
        final Collection<CustomFilter> customFilters = metric.getCustomFilters();
        final Collection<PeriodicFilter> periodicFilters = metric.getPeriodicFilter();

        if( Objects.nonNull( customFilters ) && !customFilters.isEmpty() )
        {
            for( final CustomFilter customFilter : customFilters )
            {
                extractANAFieldComponents( customFilter.getMetricsInfo(), reportFields, filterFields, columnIds );
            }
        }

        if( Objects.nonNull( periodicFilters ) && !periodicFilters.isEmpty() )
        {
            for( final PeriodicFilter periodFilter : periodicFilters )
            {
                extractANAFieldComponents( periodFilter.getMetricsInfo(), reportFields, filterFields, columnIds );
            }
        }

        extractANAFieldComponents( anaField, reportFields, filterFields, columnIds );

        if( viewType == CRMViewType.KPI )
        {
            final Duration duration = fieldsWrapper.getDuration();

            if( Objects.nonNull( duration ) )
            {
                extractANAFieldComponents( duration.getMetricsInfo(), reportFields, filterFields, columnIds );
            }
        }
        else
        {
            final Collection<ViewBy> viewByList = fieldsWrapper.getViewBy();

            if( Objects.nonNull( viewByList ) )
            {
                for( final ViewBy viewBy : viewByList )
                {
                    extractANAFieldComponents( viewBy.getMetricsInfo(), reportFields, filterFields, columnIds );
                }
            }
        }
    }

    /**
     * Extracts report fields, filter fields, and column IDs from the given {@link ANAField}
     * and adds them to the respective collections. Safely handles null input.
     *
     * @param anaField     the ANA field to extract from (may be null)
     * @param reportFields the collection to receive report fields
     * @param filterFields the collection to receive filter fields
     * @param columnIds    the collection to receive column IDs
     */
    private void extractANAFieldComponents( final ANAField anaField,
                                            final Collection<ReportField> reportFields,
                                            final Collection<FilterField> filterFields,
                                            final Collection<Long> columnIds )
    {
        if( Objects.isNull( anaField ) )
        {
            return;
        }

        reportFields.addAll( anaField.getReportFields() );
        filterFields.addAll( anaField.getFilterFields() );
        columnIds.addAll( anaField.getColumnIds() );
    }

    /**
     * <p>
     * Gets the ANA view type id based on the {@link CRMViewType}.
     * </p>
     *
     * @param crmViewType Represents the {@link CRMViewType} details
     * @return The ANA view type id
     */
    private Integer getANAViewTypeId( final CRMViewType crmViewType )
    {
        return CRMViewType.isChartType( crmViewType ) ? ViewType.CHART.getTypeCode()
                                                      : ViewType.WIDGET.getTypeCode();
    }

    /**
     * <p>
     * Gets the model based on the {@link JsonObject}.
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param createdUserId     Represents the created user id
     * @param modifiedUserId    Represents the modified user id
     * @param createdTime       Represents the created time
     * @param modificationTime  Represents the modification time
     * @param chartUniqueId     A unique id of a chart
     * @param viewDetails       Represents the {@link ViewDetails}
     * @param reportFields      Contains the collection of {@link ReportField}
     * @param filterFields      Contains the collection of {@link FilterField}
     * @param rootTableId       Represents the root table id
     * @param reportPathMapping Represents the {@link ReportPathMapping} details
     * @param graphTypeId       Represents the graph type id
     * @param request           Holds the {@link JsonObject} details
     * @return The model
     */
    abstract @NonNull T getModel( final @NonNull Long orgId,
                                  final @NonNull Long createdUserId,
                                  final @NonNull Long modifiedUserId,
                                  final @NonNull Long createdTime,
                                  final @NonNull Long modificationTime,
                                  final Long chartUniqueId,
                                  final @NonNull ViewDetails viewDetails,
                                  final @NonNull Collection<ReportField> reportFields,
                                  final Collection<FilterField> filterFields,
                                  final @NonNull Long rootTableId,
                                  final @NonNull ReportPathMapping reportPathMapping,
                                  final @NonNull int graphTypeId,
                                  final @NonNull JsonObject request );

    /**
     * Extracts the unique chart id from the first element of the underlyingConfig array.
     *
     * @param underlyingConfigArray Array that contains underlyingConfig Object (may be null)
     * @return unique chart id, or null if the array is null or empty
     */
    private Long getChartUniqueId( final JsonArray underlyingConfigArray )
    {
        final JsonObject underlyingConfigObject = Objects.nonNull( underlyingConfigArray )
                                                  ? underlyingConfigArray.getJsonObject( 0 )
                                                  : null;

        return Objects.nonNull( underlyingConfigObject )
               ? underlyingConfigObject.getLong( CRMReportKeys.CHILD_VIEW_ID )
               : null;
    }
}
