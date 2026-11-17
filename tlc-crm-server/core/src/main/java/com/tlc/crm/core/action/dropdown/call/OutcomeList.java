package com.tlc.crm.core.action.dropdown.call;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.core.field.call.CallLogFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.i18n.I18nResolver;

import java.util.List;

/**
 * Dropdown action class that provides a list of available {@link Outcome} options
 * for call logs in a dropdown format.
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "outcomeList")
public class OutcomeList extends AbstractDropdownAction {

    private final CallLogManager callLogManager;

    public OutcomeList() {
        callLogManager = CallLogManager.getInstance();
    }

    /**
     * Fetches a paginated and filtered list of {@link Outcome} options to populate a dropdown.
     * Adds the first result as the default option if the dropdown start index is 1.
     *
     * @param dropdownRequest The request object containing filtering criteria and pagination limits.
     * @return An {@link OptionContainer} holding a list of outcomes formatted as JSON options.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalOutcomesCount = callLogManager.getTotalOutcomesCount(dropdownCriteria);
        final int start = dropdownLimit.start();
        final OptionContainer optionContainer = new OptionContainer(totalOutcomesCount);

        if (totalOutcomesCount > 0 && start <= totalOutcomesCount) {
            final List<Outcome> outcomes = List.copyOf(callLogManager.getOutcomes(dropdownCriteria, dropdownLimit));
            final String idKey = CallLogFields.ID.getName();
            final String nameKey = CallLogFields.NAME.getName();
            final I18nResolver i18nResolver = dropdownRequest.i18nResolver();
            final int size = outcomes.size();
            final boolean isStartFromOne = 1 == start;

            for (int index = 0; index < size; index++) {
                final Outcome outcome = outcomes.get(index);
                final JsonObject jsonObject = Json.object();

                if (isStartFromOne && index == 0) {
                    jsonObject.put(CallLogFields.IS_DEFAULT.getName(), true);
                }
                jsonObject.put(idKey, outcome.getId());
                jsonObject.put(nameKey, i18nResolver.get(outcome.getName()));

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }
}