package com.tlc.crm.core.action.dropdown.analytics;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import com.tlc.commons.Counter;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.manager.dropdown.DashboardLayoutListManager;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Manages the {@link DashboardLayout} related drop-down actions
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@StaticDropdown( name = "dashboardList" )
public class DashboardList extends AbstractDropdownAction
{
    private final DashboardLayoutListManager dashboardLayoutListManager;

    public DashboardList()
    {
        this.dashboardLayoutListManager = DashboardLayoutListManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return The {@link OptionContainer}
     */
    @Override
    public OptionContainer fetchData( final @NonNull DropdownRequest dropdownRequest )
    {
        final Collection<CRMDashboardDetails> crmDashboardDetails = dashboardLayoutListManager.getAll( dropdownRequest.getOrgId(),
                                                                                                       dropdownRequest.getUserId(),
                                                                                                       dropdownRequest.getLimit(),
                                                                                                       dropdownRequest.getCriteria() );

        final OptionContainer optionContainer = new OptionContainer();
        final Counter counter = new Counter();

        for( final CRMDashboardDetails dashboardDetails : crmDashboardDetails )
        {
            optionContainer.addOption( dashboardDetails.id(),
                                       dashboardDetails.getName() );
            optionContainer.setTotalRecords( counter.incrementAndGet() );
        }

        return optionContainer;
    }
}
