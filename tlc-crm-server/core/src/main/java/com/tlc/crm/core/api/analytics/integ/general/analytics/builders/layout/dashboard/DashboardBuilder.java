package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.dashboard;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;

import java.util.Collection;
import java.util.List;

/**
 * Building and managing dashboard layouts within the CRM reporting framework.
 *
 * @author jamesh
 * @version 1.0
 */
public interface DashboardBuilder
{

    /**
     * Retrieves a dashboard layout based on the organization ID and view details.
     *
     * @param orgId        The ID of the organization for which the dashboard is being built.
     * @param viewDetails  The details of the view to be used for constructing the dashboard layout.
     * @return A {@link DashboardLayout} object representing the constructed dashboard layout.
     */
    default DashboardLayout getDashboardBuilder( final Long orgId, final ViewDetails viewDetails )
    {
        return DashboardBuilderImpl.getDashboardLayout( orgId, viewDetails );
    }

    /**
     * Retrieves a dashboard layout based on the organization ID and view details.
     *
     * @param orgId        The ID of the organization for which the dashboard is being built.
     * @param viewDetails  The details of the view to be used for constructing the dashboard layout.
     * @return A {@link DashboardLayout} object representing the constructed dashboard layout.
     */
    default DashboardLayout getDashboardLayout( final Long orgId,
                                                final ViewDetails viewDetails )
    {
        return DashboardBuilderImpl.getDashboardLayout( orgId, viewDetails, List.of() );
    }

    /**
     * Retrieves a dashboard layout based on the organization ID, view details, and a collection of parent views.
     *
     * @param orgId        The ID of the organization for which the dashboard is being built.
     * @param viewDetails  The details of the view to be used for constructing the dashboard layout.
     * @param parentViews  A collection of parent views associated with the dashboard layout.
     * @return A {@link DashboardLayout} object representing the constructed dashboard layout.
     */
    default DashboardLayout getDashboardLayout( final Long orgId,
                                                final ViewDetails viewDetails,
                                                final Collection<DashboardParentView> parentViews )
    {
        return DashboardBuilderImpl.getDashboardLayout( orgId, viewDetails, parentViews );
    }

    /**
     * Constructs a {@link DashboardParentView} based on the specified parameters.
     *
     * @param orgId        The ID of the organization for which the parent view is being created.
     * @param rowIdx       The row index for the parent view in the dashboard layout.
     * @param colIdx       The column index for the parent view in the dashboard layout.
     * @param title        The title of the parent view.
     * @param childViewId  The ID of the child view associated with this parent view.
     * @param reportType   The type of report associated with this parent view.
     * @param width        The width of the parent view.
     * @param height       The height of the parent view.
     * @return A {@link DashboardParentView} object representing the constructed parent view.
     */
    default DashboardParentView getDashboardParentView( final Long orgId,
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
        return DashboardBuilderImpl.getDashboardParentView( orgId, rowIdx, colIdx, title,
                                                            childViewId, reportType, width, height, dashId, id );
    }
}
