package com.tlc.crm.core.api.deal.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.ParticipantFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;

import java.util.Collection;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link Participant}
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ParticipantConverter extends EntityConverter<Participant> {

    private final ParticipantManager participantManager;
    private final CompanyConverter companyConverter;
    private final ContactConverter contactConverter;

    private static class Instance {
        private static final ParticipantConverter INSTANCE = new ParticipantConverter();
    }

    private ParticipantConverter() {
        participantManager = ParticipantManager.getInstance();
        companyConverter = CompanyConverter.getInstance();
        contactConverter = ContactConverter.getInstance();
    }

    public static ParticipantConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the Json data into {@link Participant}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the participant data
     * @return the {@link Participant} filled with data
     */
    @Override
    public Participant convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildParticipantByPartialData(converterConfig, jsonObject);
        } else {
            return buildParticipant(converterConfig, jsonObject);
        }
    }

    /**
     * Converts the {@link JsonObject} data into the {@link Participant}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the data
     * @return the {@link Participant} filled with the partial data
     */
    private Participant buildParticipantByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Participant participant = participantManager.get(converterConfig.orgId(),
                jsonObject.getLong("id"));

        setJsonToParticipant(converterConfig, participant, jsonObject);
        return participant;
    }

    /**
     * Converts the Json data into {@link Participant}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the participant data
     * @return the {@link Participant} filled with data
     */
    private Participant buildParticipant(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Participant participant = new Participant();
        final String idKey = ParticipantFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            participant.setId(jsonObject.getLong(idKey));
        }

        setJsonToParticipant(converterConfig, participant, jsonObject);
        return participant;
    }

    /**
     * Sets the Json data into the {@link Participant}
     *
     * @param converterConfig Converter Configuration
     * @param participant     which needs the data
     * @param jsonObject      which contains the participant data
     */
    private void setJsonToParticipant(final ConverterConfig converterConfig, final Participant participant,
                                      final JsonObject jsonObject) {
        final String dealIdKey = ParticipantFields.DEAL_ID.getName();
        final String contactIdKey = ParticipantFields.CONTACT_ID.getName();
        final Long orgId = converterConfig.orgId();

        participant.setOrgId(orgId);

        if (jsonObject.containsKey(contactIdKey)) {
            participant.setContactId(jsonObject.getLong(contactIdKey));
        }

        if (jsonObject.containsKey(dealIdKey)) {
            participant.setDealId(jsonObject.getLong(dealIdKey));
        }
    }

    /**
     * Converts the {@link Participant} into the {@link JsonObject}
     *
     * @param participant contains participant attributes
     * @return the {@link JsonObject} filled with participant details
     */
    @Override
    public JsonObject convert(final Participant participant) {
        final JsonObject participantJson = Json.object();

        participantJson.put(ParticipantFields.ID.getName(), participant.id());
        participantJson.put(ParticipantFields.ORG_ID.getName(), participant.orgId());
        participantJson.put(ParticipantFields.DEAL_ID.getName(), participant.getDealId());
        participantJson.put(ParticipantFields.CONTACT_ID.getName(), participant.getContactId());

        return participantJson;
    }

    /**
     * <p>
     * Converts the {@link Participant} into the {@link JsonObject}
     * </p>
     *
     * @param participant contains participant attributes
     * @return the {@link JsonObject} filled with participant details
     */
    public JsonObject convertDealParticipant(final Participant participant) {
        final JsonObject participantJson = Json.object();
        final Contact contact = participant.getContact();
        final Collection<Email> contactEmails = contact.getEmails();
        final Collection<Phone> contactPhones = contact.getPhones();
        final Company company = contact.getCompany();
        final DisplayPicture thumbnail = contact.getThumbnail();
        final Long contactId = contact.id();

        participantJson.put(ContactFields.CONTACT_ID, contactId);
        participantJson.put(ContactFields.FULL_NAME, contact.getFullName());
        participantJson.put(ContactFields.FACEBOOK, contact.getFacebook());
        participantJson.put(ContactFields.LINKEDIN, contact.getLinkedIn());
        participantJson.put(ContactFields.TWITTER, contact.getTwitter());
        participantJson.put(ContactFields.ID, participant.id());
        participantJson.put(ContactFields.DESIGNATION, contact.getDesignation());

        if (Objects.nonNull(thumbnail)) {
            participantJson.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, thumbnail));
        }

        if (Objects.nonNull(company)) {
            participantJson.put(ContactFields.COMPANY, companyConverter.convertPartialData(company));
        }

        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {
            final JsonArray emails = Json.array();
            final String idKey = FieldSubTypeKeys.ID.getName();
            final String valueKey = FieldSubTypeKeys.VALUE.getName();

            for (final Email email : contactEmails) {
                final JsonObject emailJson = Json.object();

                emailJson.put(idKey, email.id());
                emailJson.put(valueKey, email.getValue());
                emails.put(emailJson);
            }
            participantJson.put(ContactFields.EMAILS, emails);
        }

        if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {
            final JsonArray phones = Json.array();
            final String idKey = FieldSubTypeKeys.ID.getName();
            final String numberKey = FieldSubTypeKeys.NUMBER.getName();

            for (final Phone phone : contactPhones) {
                final JsonObject phoneJson = Json.object();

                phoneJson.put(idKey, phone.id());
                phoneJson.put(numberKey, phone.getNumber());
                phones.put(phoneJson);
            }
            participantJson.put(ContactFields.PHONES, phones);
        }

        return participantJson;
    }
}
