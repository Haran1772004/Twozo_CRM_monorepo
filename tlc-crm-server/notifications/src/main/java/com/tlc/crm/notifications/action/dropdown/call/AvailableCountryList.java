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
import com.tlc.crm.notifications.api.call.model.Country;
import com.tlc.crm.notifications.fields.NotificationFields;

import com.tlc.notification.service.notification.VoiceCallService;

import java.util.Collection;

/**
 * Provides a list of available countries for a dropdown selection.
 * <p>
 * This class fetches available countries from the {@link VoiceCallService}
 * and populates an {@link OptionContainer} with country codes and names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "availableCountryList")
public class AvailableCountryList extends AbstractDropdownAction {

    private final PhoneNumberManager phoneNumberManager;

    public AvailableCountryList() {
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Fetches available countries and populates the dropdown options.
     *
     * @param dropdownRequest The request containing dropdown parameters.
     * @return An {@link OptionContainer} containing country options.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalCountriesCount = phoneNumberManager.getTotalCountriesCount(criteria);
        final OptionContainer optionContainer = new OptionContainer(totalCountriesCount);

        if (totalCountriesCount > 0 && dropdownLimit.start() <= totalCountriesCount) {
            final Collection<Country> countries = phoneNumberManager.getCountries(criteria, dropdownLimit);
            final String idKey = NotificationFields.ID.getName();
            final String codeKey = NotificationFields.CODE.getName();
            final String nameKey = NotificationFields.NAME.getName();

            for (final Country country : countries) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(idKey, country.getId());
                jsonObject.put(codeKey, country.getCode());
                jsonObject.put(nameKey, country.getName());

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }
}
