package com.tlc.crm.core.features.action.dropdown.activity;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Activity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Manages activity completion status related dropdown actions.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
@StaticDropdown(name = "completionStateList")
public class CompletionStateList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param request Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest request) {
        final DropdownLimit dropdownLimit = request.getLimit();
        final Collection<Activity.CompletionStatus> activityCompletionStatuses = applyCriteria(List.of(Activity.CompletionStatus.values()),
                request.getCriteria(), List.of(Enum::name), Optional.empty());
        final long totalRecords = activityCompletionStatuses.size();
        final int start = dropdownLimit.start();
        final OptionContainer container = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Activity.CompletionStatus completionStatus : applyLimit(activityCompletionStatuses, start, dropdownLimit.limit())) {
                container.addOption(completionStatus.getId(), completionStatus.name());
            }
        }

        return container;
    }
}
