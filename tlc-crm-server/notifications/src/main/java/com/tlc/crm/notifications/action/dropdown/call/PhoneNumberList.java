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
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.notification.model.telephone.TelephoneProvider;

import java.util.Collection;
import java.util.Objects;

/**
 * This class implements a dropdown action for fetching phone numbers.
 * It extends {@link AbstractDropdownAction} and retrieves phone number data from the PhoneNumberManager.
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "phoneNumberList")
public class PhoneNumberList extends AbstractDropdownAction {

    private final PhoneNumberManager phoneNumberManager;

    public PhoneNumberList() {
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Fetches phone number data based on the provided dropdown request.
     *
     * @param dropdownRequest The request containing criteria, pagination limit, and organization ID.
     * @return OptionContainer containing formatted phone number data.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Long orgId = dropdownRequest.getOrgId();
        final Long providerId = dropdownRequest.data().getNullableValue(NotificationFields.PROVIDER_ID.getName());
        final TelephoneProvider telephoneProvider = Objects.isNull(providerId) ? TelephoneProvider.getDefaultProvider()
                : TelephoneProvider.get(providerId.shortValue());
        final long totalNumbersCount = phoneNumberManager.getTotalNumbersCount(orgId, criteria, telephoneProvider);
        final OptionContainer optionContainer = new OptionContainer(totalNumbersCount);

        if (totalNumbersCount > 0 && dropdownLimit.start() <= totalNumbersCount) {
            final Collection<PhoneNumber> phoneNumbers = phoneNumberManager.getNumberDropdownList(orgId, criteria, dropdownLimit, telephoneProvider);
            final String idKey = NotificationFields.ID.getName();
            final String numberKey = NotificationFields.NUMBER.getName();
            final String displayNameKey = NotificationFields.DISPLAY_NAME.getName();

            for (final PhoneNumber phoneNumber : phoneNumbers) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(idKey, phoneNumber.id());
                jsonObject.put(numberKey, phoneNumber.getNumber());
                jsonObject.put(displayNameKey, phoneNumber.getDisplayName());

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }
}
