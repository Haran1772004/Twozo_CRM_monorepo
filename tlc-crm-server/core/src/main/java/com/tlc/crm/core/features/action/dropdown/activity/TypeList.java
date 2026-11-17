package com.tlc.crm.core.features.action.dropdown.activity;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.field.activity.ActivityTypeFields;

import java.util.Collection;

/**
 * Manages {@link ActivityType} related dropdown actions.
 *
 * @author Sathish
 * @version 1.0
 */
@Dropdown(name = "activityTypeList", fieldAlias = "activityType")
public class TypeList extends AbstractDropdownAction {

    private final ActivityTypeManager activityTypeManager;

    public TypeList() {
        activityTypeManager = ActivityTypeManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest request) {
        final Long orgId = request.getOrgId();
        final DropdownLimit limit = request.getLimit();
        final DropdownCriteria dropdownCriteria = request.getCriteria();
        final int start = limit.start();
        final long totalRecords = activityTypeManager.getTotalCount(orgId, dropdownCriteria);
        final OptionContainer container = new OptionContainer(ActivityTypeFields.VALUE.getName(), ActivityTypeFields.NAME.getName(), totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<ActivityType> activityTypes = activityTypeManager.getTypeList(orgId, start, limit.limit(), true, dropdownCriteria);

            for (final ActivityType activityType : activityTypes) {
                container.addOption(activityType.id(), activityType.getName());
            }
        }

        return container;
    }
}
