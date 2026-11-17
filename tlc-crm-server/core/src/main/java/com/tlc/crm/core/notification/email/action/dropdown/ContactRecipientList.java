package com.tlc.crm.core.notification.email.action.dropdown;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import java.util.Collection;
import java.util.Objects;

/**
 * Manages {@link Contact} related dropdown actions.
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@StaticDropdown(name = "contactRecipientsList")
public class ContactRecipientList extends AbstractDropdownAction {

    private final ContactManager contactManager;
    private final ContactConverter contactConverter;

    public ContactRecipientList() {
        this.contactManager = ContactManager.getInstance();
        this.contactConverter = ContactConverter.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link OptionContainer} for the requested dropdown values.
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = contactManager.getTotalCount(orgId, true, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Contact> contacts = contactManager.getContactDropdownList(orgId, dropdownLimit, true, dropdownCriteria);

            final String idKey = CoreEmailFields.ID.getName();
            final String emailKey = CoreEmailFields.EMAIL.getName();
            final String nameKey = CoreEmailFields.NAME.getName();

            for (final Contact contact : contacts) {
                final JsonObject jsonObject = Json.object();
                final DisplayPicture thumbnail = contact.getThumbnail();

                contact.getEmails().stream().findFirst().ifPresent(email -> {
                    jsonObject.put(idKey, email.id());
                    jsonObject.put(emailKey, email.getValue());
                });

                if (Objects.nonNull(thumbnail)) {
                    jsonObject.put(CoreEmailFields.THUMBNAIL.getName(), contactConverter.convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
                }
                jsonObject.put(nameKey, contact.getFullName());

                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}
