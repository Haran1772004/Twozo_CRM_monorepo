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
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.fields.NotificationFields;

import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.service.notification.VoiceCallService;

import java.util.Collection;

/**
 * Provides a list of available localities for a dropdown selection.
 * <p>
 * This class fetches localities from the {@link VoiceCallService} based on the
 * specified {@link NumberCriteria} and populates an {@link OptionContainer}.
 * </p>
 * <p>
 * A default option "All Localities" is included in the dropdown list.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "availableLocalityList")
public class AvailableLocalityList extends AbstractDropdownAction {

    private final PhoneNumberManager phoneNumberManager;

    public AvailableLocalityList() {
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Fetches available localities based on the request parameters.
     * <p>
     * The method retrieves available localities from the voice call service and
     * populates an {@link OptionContainer} with locality names.
     * </p>
     *
     * @param dropdownRequest The request containing dropdown parameters.
     * @return An {@link OptionContainer} containing locality options.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final JsonObject data = dropdownRequest.data();
        final long countryId = data.getLong(NotificationFields.COUNTRY_ID.getName());
        final Long stateId = (Long) data.opt(NotificationFields.STATE_ID.getName());
        final long totalLocalitiesCount = phoneNumberManager.getTotalLocalitiesCount(countryId, stateId, criteria);
        final OptionContainer optionContainer = new OptionContainer(totalLocalitiesCount + 1);//Added extra one count for the default locality(ALL_LOCALITIES).
        final int start = dropdownLimit.start();

        if (totalLocalitiesCount > 0 && start <= totalLocalitiesCount) {
            final Collection<Locality> localities = phoneNumberManager.getLocalities(countryId, stateId, criteria, dropdownLimit);
            final String idKey = NotificationFields.ID.getName();
            final String nameKey = NotificationFields.NAME.getName();
            final String stateNameKey = NotificationFields.STATE_NAME.getName();
            final String countryNameKey = NotificationFields.COUNTRY_NAME.getName();
            final String countryCodeKey = NotificationFields.COUNTRY_CODE.getName();

            if (1 == start) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(NotificationFields.IS_DEFAULT.getName(), true);
                jsonObject.put(nameKey, NotificationAttributes.ALL_LOCALITIES);
                optionContainer.addOption(jsonObject);
            }

            for (final Locality locality : localities) {
                final JsonObject localityJson = Json.object();

                localityJson.put(idKey, locality.getId());
                localityJson.put(nameKey, locality.getName());
                localityJson.put(stateNameKey, locality.getState().getName());
                localityJson.put(countryNameKey, locality.getCountry().getName());
                localityJson.put(countryCodeKey, locality.getCountry().getCode());
                optionContainer.addOption(localityJson);
            }
        }
        return optionContainer;
    }
}
