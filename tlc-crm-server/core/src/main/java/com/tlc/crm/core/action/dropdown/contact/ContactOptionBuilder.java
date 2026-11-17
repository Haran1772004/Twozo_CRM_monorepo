package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;

import java.util.Objects;

/**
 * Builds the common dropdown option for a contact.
 * Includes name, email/phone (email preferred), and thumbnail.
 *
 */
public final class ContactOptionBuilder {

    private ContactOptionBuilder() {
    }

    /**
     * <p>
     * Builds the dropdown option JSON for the given contact.
     * </p>
     *
     * @param contact          The contact to represent as a dropdown option.
     * @param contactConverter Used to convert the contact's thumbnail to its JSON representation.
     * @return {@link JsonObject} holding the option's {@code name}, {@code value} and, when
     * available, {@code email}/{@code phone} and {@code thumbnail}.
     */
    public static JsonObject build(final Contact contact, final ContactConverter contactConverter) {
        final JsonObject option = Json.object();

        option.put(OptionKeys.VALUE, contact.id());
        option.put(OptionKeys.NAME, contact.getFullName());

        final Email primaryEmail = contact.getPrimaryEmail();

        if (Objects.isNull(primaryEmail)) {
            final Phone primaryPhone = contact.getPrimaryPhone();

            if (Objects.nonNull(primaryPhone)) {
                option.put(ContactFields.PHONE, primaryPhone.getNumber());
            }
        } else {
            option.put(ContactFields.EMAIL, primaryEmail.getValue());
        }
        final Company company = contact.getCompany();

        if (Objects.nonNull(company)) {
            option.put(ContactFields.COMPANY, company.getName());
        }
        final DisplayPicture thumbnail = contact.getThumbnail();

        if (Objects.nonNull(thumbnail)) {
            option.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
        }

        return option;
    }
}