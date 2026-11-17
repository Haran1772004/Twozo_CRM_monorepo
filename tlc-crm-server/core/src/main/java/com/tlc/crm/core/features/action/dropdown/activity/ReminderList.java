package com.tlc.crm.core.features.action.dropdown.activity;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Manages the reminder related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "activityReminder")
public class ReminderList extends AbstractDropdownAction {

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
        final Collection<Reminder> reminders = applyCriteria(List.of(Reminder.values()), request.getCriteria(),
                List.of(reminder -> resolver.get(reminder.getDisplayName())), Optional.empty());
        final long totalRecords = reminders.size();
        final int start = dropdownLimit.start();
        final OptionContainer container = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Reminder reminder : applyLimit(reminders, start, dropdownLimit.limit())) {
                container.addOption(reminder.getId(), resolver.get(reminder.getDisplayName()));
            }
        }

        return container;
    }
}
