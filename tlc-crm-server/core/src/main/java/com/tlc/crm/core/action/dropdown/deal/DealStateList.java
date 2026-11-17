package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Manages deal status related dropdown actions.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
@StaticDropdown(name = "dealStateList")
public class DealStateList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     *
     * @param request Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest request) {
        final DropdownLimit dropdownLimit = request.getLimit();
        final Collection<Deal.State> dealStates = applyCriteria(List.of(Deal.State.values()), request.getCriteria(),
                List.of(Enum::name), Optional.empty());
        final long totalRecords = dealStates.size();
        final int start = dropdownLimit.start();
        final OptionContainer container = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Deal.State state : applyLimit(dealStates, start, dropdownLimit.limit())) {
                container.addOption(state.getId(), state.name());
            }
        }

        return container;
    }
}
