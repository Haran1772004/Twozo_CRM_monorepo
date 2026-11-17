package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Manages the contact list dropdown action with basic data
 *
 * @author Maharaja S
 * @version 1.0
 */
@StaticDropdown(name = "contactOverview")
public class ContactOverviewList extends AbstractDropdownAction {

    private final ContactManager contactManager;

    public ContactOverviewList() {
        contactManager = ContactManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = contactManager.getTotalCount(orgId, dropdownCriteria, true);
        final Collection<Contact> contacts = (totalRecords > 0 && dropdownLimit.start() <= totalRecords)
                ? contactManager.partialGet(orgId, dropdownLimit, dropdownCriteria, true)
                : Collections.emptyList();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        for (final Contact contact : contacts) {
            final JsonObject contactJson = Json.object();
            final Long id = contact.id();

            contactJson.put(ContactFields.VALUE, id);
            contactJson.put(ContactFields.NAME, contact.getFullName());
            final Email primaryEmail = contact.getPrimaryEmail();
            final Phone primaryPhone = contact.getPrimaryPhone();

            if (Objects.nonNull(primaryEmail)) {
                contactJson.put(ContactFields.EMAIL, primaryEmail.getValue());
            }

            if (Objects.nonNull(primaryPhone)) {
                contactJson.put(ContactFields.PHONE, primaryPhone.getNumber());
            }

            optionContainer.addOption(contactJson);
        }
        return optionContainer;
    }
}
