package com.tlc.crm.entity.dropdown;

import com.tlc.crm.common.action.CrmResponse;

/**
 * <p>
 * Provides an abstract way to fetch the data for a async dropdown field
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface AsyncDropdownAction {

    /**
     * <p>
     * Fetches and send the {@link OptionContainer} for the requested dropdown values.
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse);
}
