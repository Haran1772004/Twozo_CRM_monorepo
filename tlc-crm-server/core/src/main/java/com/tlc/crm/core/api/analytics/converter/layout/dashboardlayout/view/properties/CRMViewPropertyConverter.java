package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.properties;

import com.tlc.analytics.config.model.view.report.chart.Axis;

import com.tlc.crm.core.api.analytics.converter.general.fields.CRMViewFieldsConverter;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Duration;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Metrics;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.ViewBy;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.CustomFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.PeriodicFilter;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * convert {@link JsonObject} into corresponding objects such as {@link Metrics},
 * {@link ViewBy}, {@link Duration}, {@link CustomFilter}, and {@link PeriodicFilter}
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
public class CRMViewPropertyConverter extends CRMReportsIntegrationConverter
{
    private final CRMViewFieldsConverter crmViewFieldsConverter;
    private final CRMReportManagerAPI crmManager;

    private CRMViewPropertyConverter()
    {
        crmViewFieldsConverter = CRMViewFieldsConverter.INSTANCE();
        this.crmManager = CRMReportManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final CRMViewPropertyConverter INSTANCE = new CRMViewPropertyConverter();
    }

    public static CRMViewPropertyConverter INSTANCE()
    {
        return CRMViewPropertyConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Metrics} detail
     * </p>
     *
     * @param orgId      Represents an organisation id
     * @param useId      Represents the user id
     * @param accessTime Represents the access time
     * @param metric     The {@link JsonObject} represents the metric configuration
     * @return The {@link Metrics} details
     */
    public Metrics convertMetrics(final @NonNull Long orgId,
                                  final @NonNull Long useId,
                                  final @NonNull Long accessTime,
                                  final @NonNull JsonObject metric,
                                  final Collection<Long> involvedColumnIds,
                                  final Map<Long, ReportModularField> reportFieldMap) throws Exception
    {
        final ANAField metricsFields = crmViewFieldsConverter.covert( orgId, accessTime,
                                                                     metric.getLong( CRMReportKeys.FIELD_ID ), null,
                                                                     (long) Axis.Y_AXIS.getType(),
                                                                    false, metric, involvedColumnIds, reportFieldMap );

        final Collection<CustomFilter> customFilters = new ArrayList<>();
        final Collection<PeriodicFilter> periodicFilters = new ArrayList<>();

        // Handling Periodic filter
        if( metric.containsKey( CRMViewPropertyKeys.PERIODIC_FILTERS ) )
        {
            final JsonObject periodicFilterConfig = metric.getJsonObject( CRMViewPropertyKeys.PERIODIC_FILTERS );
            final PeriodicFilter periodicFilter = convertPeriodicFilter( orgId, useId, accessTime, periodicFilterConfig, involvedColumnIds, reportFieldMap );
            periodicFilters.add( periodicFilter );
        }

        // Handling Custom Filter
        if( metric.containsKey( CRMViewPropertyKeys.FILTERS ) )
        {
            final JsonArray customFilterConfigs = metric.getJsonArray( CRMViewPropertyKeys.FILTERS );
            final int filterSize = customFilterConfigs.size();

            for(int filter_index = 0; filter_index < filterSize; filter_index++ )
            {
                final JsonObject customFilterConfig = customFilterConfigs.getJsonObject( filter_index );
                final CustomFilter customFilter = convertCustomFilter( orgId, useId, accessTime, customFilterConfig, involvedColumnIds, reportFieldMap );
                customFilters.add( customFilter );
            }
        }

        return Metrics.builder()
                      .metricsInfo( metricsFields )
                      .customFilters( customFilters )
                      .periodicFilter( periodicFilters )
                      .isDeleted( false )
                      .build();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link ViewBy} details
     * </p>
     *
     * @param orgId           Represents an organisation id
     * @param userId          Represents the user id
     * @param accessTime      Represents the access time
     * @param metricsModuleId Represents the {@link Metrics} ID
     * @param viewByConfig    The {@link JsonObject} represents the view by configuration
     * @return The {@link ViewBy} details
     */
    public ViewBy convertViewBy( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull Integer metricsModuleId,
                                 final @NonNull Long accessTime,
                                 final @NonNull JsonObject viewByConfig,
                                 final @NonNull Metrics metrics,
                                 final int metricIndex, final Collection<Long> involvedColumnIds,
                                 final Map<Long, ReportModularField> reportFieldMap) throws Exception
    {
        final Long fieldId;
        boolean isPeriodicFilterField = false;

        if ( viewByConfig.containsKey(CRMReportKeys.FIELD_ID) )
        {
            fieldId = viewByConfig.getLong(CRMReportKeys.FIELD_ID);
        } else
        {
            fieldId = metrics.getPeriodicFilter().stream()
                      .map(periodicFilter -> periodicFilter.getMetricsInfo().getCrmFieldId())
                      .findFirst().orElse(null);

            isPeriodicFilterField = true;
        }

        final ANAField viewBy = crmViewFieldsConverter.covert( orgId, accessTime,
                                                              fieldId, metricsModuleId, 1L,
                                                              false, viewByConfig, involvedColumnIds, reportFieldMap );

        FieldBucketUnit fieldBucketUnit = null;
        boolean isBucketApplied = false;

        if( viewByConfig.containsKey( CRMViewPropertyKeys.BUCKET ) )
        {
            final JsonObject bucketJson = viewByConfig.getJsonObject( CRMViewPropertyKeys.BUCKET );

            if( bucketJson.containsKey( CRMViewPropertyKeys.UNIT_ID ) )
            {
                fieldBucketUnit = FieldBucketUnit.builder()
                                                 .orgId( orgId )
                                                 .unitId( bucketJson.getInt( CRMViewPropertyKeys.UNIT_ID ) ).build();
            }

            if( bucketJson.containsKey( CommonKeys.FROM ) && bucketJson.containsKey( CommonKeys.TO ) )
            {
                isBucketApplied = true;
            }
        }

        return ViewBy.builder()
                     .metricsInfo( viewBy )
                     .fieldBucketUnit( fieldBucketUnit )
                     .isBucketApplied( isBucketApplied )
                     .isPeriodicFilterField( isPeriodicFilterField )
                     .isDuplicate( 0 < metricIndex )
                     .isDeleted( false ).build();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Duration} details
     * </p>
     *
     * @param orgId          Represents an organisation id
     * @param userId         Represents the user id
     * @param accessTime     Represents the access time
     * @param durationConfig The {@link JsonObject} represents the duration configuration
     * @return The {@link Duration} details
     */
    public @NonNull Duration convertDuration( final @NonNull Long orgId,
                                              final @NonNull Long userId,
                                              final @NonNull Long accessTime,
                                              final @NonNull JsonObject durationConfig,
                                              final Collection<Long> involvedColumnIds,
                                              final Map<Long, ReportModularField> reportFieldMap) throws Exception
    {

        final Long filedId = durationConfig.getLong( CRMReportKeys.FIELD_ID );

        final ANAField durationFields = crmViewFieldsConverter.covert( orgId, accessTime,
                                                                      filedId, null, 1L,
                                                             false, durationConfig, involvedColumnIds, reportFieldMap);

        return Duration.builder()
                       .metricsInfo( durationFields )
                       .isDeleted( false ).build();


    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link PeriodicFilter} details
     * </p>
     *
     * @param orgId                Represents an organisation id
     * @param userId               Represents the user id
     * @param accessTime           Represents the access time
     * @param periodicFilterConfig The {@link JsonObject} represents the periodic filter configuration
     * @return The {@link PeriodicFilter} details
     */
    public final @NonNull PeriodicFilter convertPeriodicFilter( final @NonNull Long orgId,
                                                                final @NonNull Long userId,
                                                                final @NonNull Long accessTime,
                                                                final @NonNull JsonObject periodicFilterConfig,
                                                                final Collection<Long> involvedColumnIds,
                                                                final @NonNull Map<Long, ReportModularField> reportFieldMap) throws Exception
    {
        final Long filterId = periodicFilterConfig.getLong( CRMReportKeys.FIELD_ID );

        final ANAField filterField = crmViewFieldsConverter.covert( orgId, accessTime, filterId, null,
                                                                   (long) Axis.Y_AXIS.getType(), true,
                                                                   periodicFilterConfig, involvedColumnIds, reportFieldMap);

        return PeriodicFilter.builder()
                             .metricsInfo( filterField )
                             .isDeleted( false )
                             .build();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link CustomFilter} details
     * </p>
     *
     * @param orgId              Represents an organisation id
     * @param userId             Represents the user id
     * @param accessTime         Represents the access time
     * @param customFilterConfig The {@link JsonObject} represents the custom filter configuration
     * @return The {@link CustomFilter} details
     */
    public CustomFilter convertCustomFilter( final @NonNull Long orgId,
                                             final @NonNull Long userId,
                                             final @NonNull Long accessTime,
                                             final @NonNull JsonObject customFilterConfig,
                                             final Collection<Long> involvedColumnIds,
                                             final @NonNull Map<Long, ReportModularField> reportFieldMap) throws Exception
    {
        final Long filterId = customFilterConfig.getLong( CRMReportKeys.FIELD_ID );
        final ANAField filterField = crmViewFieldsConverter.covert( orgId, accessTime, filterId, null, (long) Axis.Y_AXIS.getType(),
                                                                   true, customFilterConfig, involvedColumnIds, reportFieldMap);

        return CustomFilter.builder()
                           .metricsInfo( filterField )
                           .isDeleted( false ).build();
    }
}
