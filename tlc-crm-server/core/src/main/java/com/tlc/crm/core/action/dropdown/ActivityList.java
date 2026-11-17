package com.tlc.crm.core.action.dropdown;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.fields.option.OptionKeys;

import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;

import java.util.Collection;

/**
 * Manages the business type dropdown field options.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "activityList")
public class ActivityList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final OptionContainer optionContainer = new OptionContainer(OptionKeys.ID, OptionKeys.NAME);
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final SourceActivityManager activityManager = SourceActivityManager.getInstance();
        final long totalRecords = activityManager.getTotalCount(orgId, dropdownCriteria);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Activity> activities = activityManager.getActivityDropdownList(orgId, dropdownLimit, dropdownCriteria);

            for (final Activity activity : activities) {
                optionContainer.addOption(activity.id(), activity.getTitle());
            }
        }

        return optionContainer;
    }
}
