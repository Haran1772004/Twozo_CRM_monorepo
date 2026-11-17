package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.view;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.fav.Favourite;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import lombok.NonNull;

import java.util.Collection;

/**
 * Contract for building view within the CRM reporting framework.
 *
 * @author james
 * @version 1.0
 */
public interface ViewBuilder
{

    /**
     * Creates and returns a {@link Favourite} object.
     *
     * @param viewId the ID of the view to mark as favorite
     * @param orgId  the ID of the organization to which the view belongs
     * @param userId the ID of the user marking the view as favorite
     * @return a Favourite object with the specified parameters
     */
    @NonNull
    default Favourite getFavView( final @NonNull Long viewId, final @NonNull Long orgId, final @NonNull Long userId )
    {
        return FavBuilder.getFav( viewId, orgId, userId );
    }

    /**
     * Creates and returns a {@link ViewDetails} object
     *
     * @param orgId        the organization ID
     * @param createdBy    the ID of the user who created the view
     * @param modifiedBy   the ID of the user who last modified the view
     * @param createdTime  the time when the view was created
     * @param modifiedTime the time when the view was last modified
     * @param viewTypeId   the type ID of the view
     * @param displayName  the display name of the view
     * @param description  a description of the view
     * @return a ViewDetails object containing the provided view information
     */
    @NonNull
    default ViewDetails getViewDetails( final @NonNull Long orgId,
                                        final @NonNull Long createdBy,
                                        final @NonNull Long modifiedBy,
                                        final @NonNull Long createdTime,
                                        final @NonNull Long modifiedTime,
                                        final @NonNull Integer viewTypeId,
                                        final @NonNull String displayName,
                                        final String description,
                                        final boolean moveToTrash )
    {
        return ViewDetailsBuilder.getViewDetails( orgId, createdBy, modifiedBy, createdTime,
                                                  modifiedTime, viewTypeId, displayName, description, moveToTrash );
    }

    /**
     * Creates and returns a KPI (Key Performance Indicator) chartView.
     *
     * @param orgId             the organization ID
     * @param rootTableId       the root table ID for the KPI
     * @param viewDetails       the details of the view
     * @param title             the title of the KPI
     * @param graphType         the type of graph (e.g., bar, line)
     * @param reportFields      the collection of report fields
     * @param filterFields      the collection of filter fields
     * @param widgetComparators the collection of KPI comparators
     * @return a ChartView object representing the KPI
     */
    default @NonNull ChartView getKPI( final @NonNull Long orgId,
                                       final @NonNull Long rootTableId,
                                       final @NonNull ViewDetails viewDetails,
                                       final @NonNull String title,
                                       final @NonNull Integer graphType,
                                       final @NonNull Collection<ReportField> reportFields,
                                       final Collection<FilterField> filterFields,
                                       final Collection<KPIComparator> widgetComparators )
    {
        return ChartViewBuilder.getKPI( orgId, rootTableId, viewDetails, title,
                                        graphType, reportFields, filterFields, widgetComparators );
    }

    /**
     * Creates and returns a {@link ChartView} for a given organization and view.
     *
     * @param orgId        the organization ID
     * @param rootTableId  the root table ID for the chart
     * @param viewDetails  the details of the view
     * @param title        the title of the chart
     * @param graphType    the type of graph (e.g., bar, line)
     * @param reportFields the collection of report fields
     * @param filterFields the collection of filter fields
     * @return a ChartView object representing the chart
     */
    default @NonNull ChartView getChartView( final @NonNull Long orgId,
                                             final @NonNull Long rootTableId,
                                             final @NonNull ViewDetails viewDetails,
                                             final @NonNull String title,
                                             final @NonNull Integer graphType,
                                             final @NonNull Collection<ReportField> reportFields,
                                             final Collection<FilterField> filterFields )
    {
        return ChartViewBuilder.getChart( orgId, rootTableId, viewDetails, title,
                                          graphType, reportFields, filterFields );
    }

    /**
     * Creates and returns a {@link KPIComparator} object for a given organization.
     *
     * @param orgId          the organization ID
     * @param comparatorType the type of comparator (e.g., target vs actual)
     * @param comparatorSide the side (e.g., left or right)
     * @param createdTime    the time when the comparator was created
     * @param modifiedTime   the time when the comparator was last modified
     * @param from           the starting value for comparison
     * @param to             the ending value for comparison
     * @return a KPIComparator object for the specified parameters
     */
    default @NonNull KPIComparator getWidgetComparator( final @NonNull Long orgId,
                                                        final @NonNull Integer comparatorType,
                                                        final @NonNull Integer comparatorSide,
                                                        final @NonNull Long createdTime,
                                                        final @NonNull Long modifiedTime,
                                                        final @NonNull Long from,
                                                        final @NonNull Long to )
    {
        return ChartViewBuilder.getKPIComparator( orgId, comparatorType, comparatorSide,
                                                  createdTime, modifiedTime, from, to );
    }

}
