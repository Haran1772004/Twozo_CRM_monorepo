package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.dashboard;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;

/**
 * Implementation of the {@link DashboardBuilder} interface.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class DashboardBuilderImpl
{

    private static class Instance
    {
        private static final DashboardBuilderImpl INSTANCE = new DashboardBuilderImpl();
    }

    public static DashboardBuilderImpl INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Creates a {@link DashboardLayout} using the specified organization ID and view details.
     *
     * @param orgId        The ID of the organization for which the dashboard layout is being created.
     * @param viewDetails  The details of the view to be included in the dashboard layout.
     * @return A {@link DashboardLayout} object configured with the provided organization ID and view details.
     */
    @NonNull
    protected static DashboardLayout getDashboardLayout( final Long orgId, final ViewDetails viewDetails )
    {
        return DashboardLayout.builder()
                              .orgId( orgId )
                              .viewDetails( viewDetails )
                              .isSharable( false )
                              .build();
    }

    /**
     * Creates a {@link DashboardLayout} using the specified organization ID, view details, and a collection of parent views.
     *
     * @param orgId        The ID of the organization for which the dashboard layout is being created.
     * @param viewDetails  The details of the view to be included in the dashboard layout.
     * @param parentViews  A collection of parent views associated with the dashboard layout.
     * @return A {@link DashboardLayout} object configured with the provided parameters.
     */
    @NonNull
    protected static DashboardLayout getDashboardLayout( final Long orgId,
                                                         final ViewDetails viewDetails,
                                                         final Collection<DashboardParentView> parentViews )
    {
        return DashboardLayout.builder()
                              .orgId( orgId )
                              .viewDetails( viewDetails )
                              .parentViews( parentViews )
                              .isSharable( false )
                              .build();
    }

    /**
     * Constructs a {@link DashboardParentView} using the specified parameters.
     *
     * @param orgId        The ID of the organization for which the parent view is being created.
     * @param rowIdx       The row index for the parent view in the dashboard layout.
     * @param colIdx       The column index for the parent view in the dashboard layout.
     * @param title        The title of the parent view.
     * @param childViewId  The ID of the child view associated with this parent view.
     * @param reportType   The type of report associated with this parent view.
     * @param width        The width of the parent view.
     * @param height       The height of the parent view.
     * @return A {@link DashboardParentView} object configured with the provided parameters.
     */
    protected static DashboardParentView getDashboardParentView( final Long orgId,
                                                                 final Long rowIdx,
                                                                 final Long colIdx,
                                                                 final String title,
                                                                 final Long childViewId,
                                                                 final Integer reportType,
                                                                 final Long width,
                                                                 final Long height,
                                                                 final Long dashId,
                                                                 final Long id )
    {
        return DashboardParentView.builder()
                                  .orgId( orgId )
                                  .rowIdx( rowIdx )
                                  .colIdx( colIdx )
                                  .title( title )
                                  .childViewId( childViewId )
                                  .reportType( reportType )
                                  .width( width )
                                  .height( height )
                                  .dashId( dashId )
                                  .id( id )
                                  .build();
    }

}
