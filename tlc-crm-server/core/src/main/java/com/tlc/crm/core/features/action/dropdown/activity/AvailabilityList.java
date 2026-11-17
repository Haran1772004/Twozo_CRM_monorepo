package com.tlc.crm.core.features.action.dropdown.activity;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Manages the availability related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "activityAvailability")
public class AvailabilityList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final I18nResolver resolver = dropdownRequest.i18nResolver();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Collection<Availability> availabilities = applyCriteria(List.of(Availability.values()), dropdownRequest.getCriteria(),
                List.of(availability -> resolver.get(availability.getDisplayName())), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = availabilities.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Availability availability : applyLimit(availabilities, start, dropdownLimit.limit())) {
                optionContainer.addOption(availability.getId(), resolver.get(availability.getDisplayName()));
            }
        }

        return optionContainer;
    }
}
