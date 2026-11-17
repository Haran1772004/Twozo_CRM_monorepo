package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Manages phone type related dropdown actions.
 *
 * @author Maharaja S
 * @version 1.0
 */
@StaticDropdown(name = "phoneTypeList")
public class PhoneTypeList extends AbstractDropdownAction {

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
        final Collection<Phone.Type> phoneTypes = applyCriteria(List.of(Phone.Type.values()), request.getCriteria(),
                List.of(type -> resolver.get(type.getDisplayName())), Optional.empty());
        final long totalRecords = phoneTypes.size();
        final int start = dropdownLimit.start();
        final OptionContainer container = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            for (final Phone.Type type : applyLimit(phoneTypes, start, dropdownLimit.limit())) {
                container.addOption(type.getId(), resolver.get(type.getDisplayName()));
            }
        }

        return container;
    }
}