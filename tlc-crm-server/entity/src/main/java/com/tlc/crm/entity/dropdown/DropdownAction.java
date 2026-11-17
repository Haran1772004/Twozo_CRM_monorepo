package com.tlc.crm.entity.dropdown;

/**
 * <p>
 * Provides an abstract way to fetch the data for a dropdown field
 * </p>
 *
 * @author Selvakumar
 */
public interface DropdownAction {

    /**
     * <p>
     * Fetches the {@link OptionContainer} for the requested dropdown values.
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    OptionContainer fetchData(final DropdownRequest dropdownRequest);

    /**
     * <p>
     * Fetches the {@link OptionContainer} for the requested dropdown values of mobile
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    default OptionContainer fetchMobileData(final DropdownRequest dropdownRequest) {
        return fetchData(dropdownRequest);
    }

    /**
     * Fetches Zapier-related data based on the provided dropdown request.
     * <p>
     * <span style="color:red"><b>Note:</b></span>
     * This method is a default implementation that returns an {@link OptionContainer}
     * containing options to populate a dynamic dropdown in the Zapier integration.
     * </p>
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    default OptionContainer fetchZapierData(final DropdownRequest dropdownRequest) {
        return fetchData(dropdownRequest);
    }

    /**
     * Fetches public rest API data based on the provided dropdown request.
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    default OptionContainer fetchRestAPIData(final DropdownRequest dropdownRequest) {
        return fetchData(dropdownRequest);
    }
}
