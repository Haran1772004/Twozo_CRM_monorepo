package com.tlc.crm.notifications.action.dropdown.call;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.State;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.fields.NotificationFields;

import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.service.notification.VoiceCallService;

import java.util.Collection;

/**
 * Provides a list of available states for a dropdown selection.
 * <p>
 * This class fetches available states from the {@link VoiceCallService}
 * based on the given {@link NumberCriteria} and populates an {@link OptionContainer}.
 * </p>
 * <p>
 * The dropdown list includes a default option **"All States"** to allow selecting all states.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "availableStateList")
public class AvailableStateList extends AbstractDropdownAction {

    private final PhoneNumberManager phoneNumberManager;

    public AvailableStateList() {
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Fetches available states based on the request parameters.
     * <p>
     * This method retrieves available states from the voice call service and
     * populates an {@link OptionContainer} with state names.
     * </p>
     *
     * @param dropdownRequest The request containing dropdown parameters.
     * @return An {@link OptionContainer} containing state options.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final JsonObject data = dropdownRequest.data();
        final long countryId = data.getLong(NotificationFields.COUNTRY_ID.getName());
        final long totalStatesCount = phoneNumberManager.getTotalStatesCount(countryId, criteria);
        final OptionContainer optionContainer = new OptionContainer(totalStatesCount + 1);//Added extra one count for the default state(ALL_STATES).
        final int start = dropdownLimit.start();

        if (totalStatesCount > 0 && start <= totalStatesCount) {
            final Collection<State> states = phoneNumberManager.getStates(countryId, criteria, dropdownLimit);
            final String idKey = NotificationFields.ID.getName();
            final String nameKey = NotificationFields.NAME.getName();
            final String countryIdKey = NotificationFields.COUNTRY_ID.getName();

            if (1 == start) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(NotificationFields.IS_DEFAULT.getName(), true);
                jsonObject.put(nameKey, NotificationAttributes.ALL_STATES);
                optionContainer.addOption(jsonObject);
            }

            for (final State state : states) {
                final JsonObject stateJson = Json.object();

                stateJson.put(idKey, state.getId());
                stateJson.put(nameKey, state.getName());
                stateJson.put(countryIdKey, state.getCountry().getId());
                optionContainer.addOption(stateJson);
            }
        }
        return optionContainer;
    }
}