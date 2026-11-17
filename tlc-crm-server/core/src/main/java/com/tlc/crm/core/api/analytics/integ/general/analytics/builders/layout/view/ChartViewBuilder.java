package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.view;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;

/**
 * Construct {@link ChartView} objects for both KPIs and general charts
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ChartViewBuilder
{
    private static class Instance
    {
        private static final ChartViewBuilder INSTANCE = new ChartViewBuilder();
    }

    public static ChartViewBuilder INSTANCE()
    {
        return ChartViewBuilder.Instance.INSTANCE;
    }

    /**
     * Creates a {@link ChartView} configured for a Key Performance Indicator (KPI).
     *
     * @param orgId          The ID of the organization this KPI belongs to.
     * @param rootTableId    The ID of the root table associated with the KPI.
     * @param viewDetails    The details regarding the view of this KPI.
     * @param title          The title to be displayed for the KPI.
     * @param graphType      The type of graph to be used for the KPI.
     * @param reportFields   A collection of report fields associated with the KPI.
     * @param filterFields   A collection of filter fields applicable to this KPI.
     * @param KPIComparators A collection of comparators for KPI evaluation.
     * @return A configured {@link ChartView} object representing the KPI.
     */
    protected static @NonNull ChartView getKPI( final Long orgId,
                                                final Long rootTableId,
                                                final ViewDetails viewDetails,
                                                final String title,
                                                final Integer graphType,
                                                final Collection<ReportField> reportFields,
                                                final Collection<FilterField> filterFields,
                                                final Collection<KPIComparator> KPIComparators )
    {
        return ChartView.builder()
                        .orgId( orgId )
                        .rootTableId( rootTableId )
                        .viewDetails( viewDetails )
                        .title( title )
                        .graphType( graphType )
                        .reportFields( reportFields )
                        .filterFields( filterFields )
                        .build();
    }

    /**
     * Creates a {@link KPIComparator} with the specified parameters.
     *
     * @param orgId          The ID of the organization this comparator belongs to.
     * @param comparatorType The type of the comparator.
     * @param comparatorSide The side of the comparator (e.g., left, right).
     * @param createdTime    The time when this comparator was created.
     * @param modifiedTime   The last time this comparator was modified.
     * @param from           The starting value for the comparator.
     * @param to             The ending value for the comparator.
     * @return A configured {@link KPIComparator} object.
     */
    @NonNull
    protected static KPIComparator getKPIComparator( final Long orgId,
                                                           final Integer comparatorType,
                                                           final Integer comparatorSide,
                                                           final Long createdTime,
                                                           final Long modifiedTime,
                                                           final Long from,
                                                           final Long to )
    {
        return KPIComparator.builder()
                            .orgId( orgId )
                            .comparatorType( comparatorType )
                            .comparatorSide( comparatorSide )
                            .createdTime( createdTime )
                            .modifiedTime( modifiedTime )
                            .from( from )
                            .to( to )
                            .build();
    }

    /**
     * Creates a {@link ChartView} for a general chart.
     *
     * @param orgId          The ID of the organization this chart belongs to.
     * @param rootTableId    The ID of the root table associated with the chart.
     * @param viewDetails     The details regarding the view of this chart.
     * @param title          The title to be displayed for the chart.
     * @param graphType      The type of graph to be used for the chart.
     * @param reportFields   A collection of report fields associated with the chart.
     * @param filterFields   A collection of filter fields applicable to this chart.
     * @return A configured {@link ChartView} object representing the chart.
     */
    protected static @NonNull ChartView getChart( final Long orgId,
                                                  final Long rootTableId,
                                                  final ViewDetails viewDetails,
                                                  final String title,
                                                  final Integer graphType,
                                                  final Collection<ReportField> reportFields,
                                                  final Collection<FilterField> filterFields )
    {
        return ChartView.builder()
                        .orgId( orgId )
                        .rootTableId( rootTableId )
                        .viewDetails( viewDetails )
                        .title( title )
                        .graphType( graphType )
                        .reportFields( reportFields )
                        .filterFields( filterFields )
                        .build();
    }

}
