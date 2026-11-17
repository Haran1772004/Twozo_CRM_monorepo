package com.tlc.crm.core.features.action.call.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.action.call.model.FavouriteContact;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.models.type.Phone;

import java.util.Objects;

/**
 * Converter for transforming {@link FavouriteContact} entries between their {@link JsonObject} and {@link FavouriteContact}.
 *
 * @author kishore J
 * @version 1.0
 */
public final class FavouriteContactConverter extends CrmModelConverter<FavouriteContact, JsonObject> {

    private final ContactConverter contactConverter;

    private FavouriteContactConverter() {
        contactConverter = ContactConverter.getInstance();
    }

    private static class Instance {
        private static final FavouriteContactConverter INSTANCE = new FavouriteContactConverter();
    }

    /**
     * Gets the singleton instance of {@link FavouriteContactConverter}.
     *
     * @return The singleton {@link FavouriteContactConverter} instance.
     */
    public static FavouriteContactConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the CRM-specific container holding the serialized data and user context
     * @return The {@link FavouriteContact}
     */
    @Override
    public FavouriteContact convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject jsonObject = converterData.dataToBeConverted();
        final FavouriteContact favouriteContact = new FavouriteContact();
        final Contact contact = new Contact();

        if (jsonObject.containsKey(ContactFields.ID)) {
            favouriteContact.setId(jsonObject.getLong(ContactFields.ID));
        }

        if (jsonObject.containsKey(ContactFields.CONTACT_ID)) {
            contact.setId(jsonObject.getLong(ContactFields.CONTACT_ID));
        }
        favouriteContact.setUserId(converterData.ownerId());
        favouriteContact.setContact(contact);
        favouriteContact.setOrgId(converterData.orgId());

        return favouriteContact;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the CRM-specific container holding the domain model and user context
     * @return The {@link JsonObject} representation of the {@link FavouriteContact}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<FavouriteContact> converterData) {
        final FavouriteContact favouriteContact = converterData.dataToBeConverted();
        final JsonObject jsonObject = Json.object();
        final Contact contact = favouriteContact.getContact();

        jsonObject.put(ContactFields.VALUE, favouriteContact.id());

        if (Objects.nonNull(contact)) {
            final Phone primaryPhone = contact.getPrimaryPhone();
            final DisplayPicture thumbnail = contact.getThumbnail();

            jsonObject.put(ContactFields.NAME, contact.getFullName());

            if (Objects.nonNull(primaryPhone)) {
                jsonObject.put(ContactFields.PHONE, primaryPhone.getNumber());
            }

            if (Objects.nonNull(thumbnail)) {
                jsonObject.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
            }
        }

        return jsonObject;
    }
}
