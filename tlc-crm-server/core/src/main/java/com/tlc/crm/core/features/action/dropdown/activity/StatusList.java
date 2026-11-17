package com.tlc.crm.core.features.action.dropdown.activity;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Manages completion type related dropdown actions.
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "activityStatus")
public class StatusList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param request Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest request) {
        final I18nResolver resolver = request.i18nResolver();
        final DropdownLimit dropdownLimit = request.getLimit();
        final Collection<Status> statuses = applyCriteria(List.of(Status.values()), request.getCriteria(),
                List.of(status -> resolver.get(status.getDisplayName())), Optional.empty());
        final long totalRecords = statuses.size();
        final int start = dropdownLimit.start();
        final OptionContainer container = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Status status : applyLimit(statuses, start, dropdownLimit.limit())) {
                container.addOption(status.getId(), resolver.get(status.getDisplayName()));
            }
        }

        return container;
    }
}
