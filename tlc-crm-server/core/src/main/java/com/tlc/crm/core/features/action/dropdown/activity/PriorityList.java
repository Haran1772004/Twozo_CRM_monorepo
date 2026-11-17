package com.tlc.crm.core.features.action.dropdown.activity;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Manages {@link Activity} {@link Priority} related dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "activityPriority")
public class PriorityList extends AbstractDropdownAction {

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
        final Collection<Priority> priorities = applyCriteria(List.of(Priority.values()), request.getCriteria(),
                List.of(priority -> resolver.get(priority.getDisplayName())), Optional.empty());
        final long totalRecords = priorities.size();
        final int start = dropdownLimit.start();
        final OptionContainer container = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Priority priority : applyLimit(priorities, start, dropdownLimit.limit())) {
                container.addOption(priority.getId(), resolver.get(priority.getDisplayName()));
            }
        }

        return container;
    }
}