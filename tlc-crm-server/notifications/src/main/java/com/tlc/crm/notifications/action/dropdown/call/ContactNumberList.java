package com.tlc.crm.notifications.action.dropdown.call;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.notifications.fields.NotificationFields;

import java.util.Collection;

/**
 * This class implements a dropdown action to fetch and display contact phone numbers.
 * It extends {@link AbstractDropdownAction} and retrieves contacts with their phone numbers.
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "contactNumberList")
public class ContactNumberList extends AbstractDropdownAction {

    private final ContactManager contactManager;

    public ContactNumberList() {
        contactManager = ContactManager.getInstance();
    }

    /**
     * Fetches contact numbers based on the provided dropdown request.
     *
     * @param dropdownRequest The request containing criteria, limit, and organization ID.
     * @return OptionContainer containing contact numbers in a structured JSON format.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Long orgId = dropdownRequest.getOrgId();
        final long totalNumbersCount = contactManager.getTotalContactNumbersCount(orgId, criteria);
        final OptionContainer optionContainer = new OptionContainer(totalNumbersCount);

        if (totalNumbersCount > 0 && dropdownLimit.start() <= totalNumbersCount) {
            final Collection<Contact> contacts = contactManager.getContactNumbers(orgId, criteria, dropdownLimit);
            final String idKey = NotificationFields.ID.getName();
            final String numberKey = NotificationFields.NUMBER.getName();
            final String displayNameKey = NotificationFields.DISPLAY_NAME.getName();

            for (final Contact contact : contacts) {
                final JsonObject jsonObject = Json.object();
                final JsonObject phoneJson = Json.object();
                final Phone phone = contact.getPrimaryPhone();

                phoneJson.put(idKey, phone.id());
                phoneJson.put(numberKey, phone.getNumberWithCode());
                jsonObject.put(idKey, contact.id());
                jsonObject.put(displayNameKey, contact.getFullName());
                jsonObject.put(numberKey, phoneJson);//TODO Add profile pic

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }
}
