package com.tlc.crm.core.features.action.merge.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.action.merge.EntityMergeManagement;
import com.tlc.crm.core.features.api.contact.manager.ContactMergeManager;
import com.tlc.crm.core.field.EntityMergeFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.internal.SecureFieldService;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.i18n.I18nResolver;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Handles contact merge actions by reusing duplicate merge validations.
 * </p>
 *
 * @author EswariNivethaVU.
 * @version 1.0.
 */
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS)
@WebAction(path = "/contact/merge/mgmt")
public class ContactMergeManagement extends EntityMergeManagement<Contact> {

    private final SecureFieldService secureFieldService;
    private final ContactManager contactManager;
    private final ContactMergeManager contactMergeManager;
    private final ContactConverter contactConverter;

    public ContactMergeManagement() {
        secureFieldService = SecureFieldService.getInstance();
        contactManager = ContactManager.getInstance();
        contactMergeManager = ContactMergeManager.getInstance();
        contactConverter = ContactConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request.
     * @param response the response to be sent.
     */
    @Override
    protected void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(ContactFields.DATA);
        final long orgId = request.orgId();
        final long userId = request.userId();
        final String type = requestJson.getString(ContactFields.TYPE);
        final I18nResolver i18nResolver = request.i18nResolver();

        switch (type) {
            case EntityMergeFields.PREVIEW -> getContactPreview(orgId, data, response, i18nResolver);
            case EntityMergeFields.MERGE -> mergeContacts(orgId, userId, data, response);
            case EntityMergeFields.GET_CONTACT_FIELDS -> response.put(EntityMergeFields.FIELDS, getContactFields(orgId, response));
            case EntityMergeFields.GET_CONTACT -> get(orgId, data, response, i18nResolver);
        }
    }

    @Override
    protected EntityConfigManager<Contact> getEntityManager() {
        return contactManager;
    }

    /**
     * <p>
     * Gets the contact based on the contact id.
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param data         Holds the {@link JsonObject} details.
     * @param i18nResolver Represents the {@link I18nResolver}
     * @param crmResponse  Represents the {@link CrmResponse} details.
     */
    private void get(final Long orgId, final JsonObject data, final CrmResponse crmResponse, final I18nResolver i18nResolver) {
        final Long id = data.getLong(ContactFields.ID);

        if (!contactManager.exists(orgId, id)) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
        final Contact contact = contactMergeManager.get(orgId, id);

        crmResponse.put(ContactFields.PREVIEW_RESULT, contactConverter.convertPrimaryContactToJson(contact, i18nResolver));
    }

    /**
     * <p>
     * Gets the contact fields list based on the organization id.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param crmResponse Represents the {@link CrmResponse} details.
     * @return The {@link JsonArray} details.
     */
    private JsonArray getContactFields(final Long orgId, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final Collection<FieldInfo> fieldsBySource = secureFieldService.getFieldsBySource(orgId, Contact.ENTITY_TYPE);

        for (final FieldInfo fieldInfo : fieldsBySource) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(DealFields.KEY, fieldInfo.isPersistent() ? fieldInfo.idAsString() : fieldInfo.aliasName());
            jsonObject.put(DealFields.NAME, crmResponse.resolveI18nPlaceholder(fieldInfo.i18nName()));
            jsonObject.put(DealFields.STORE_TYPE, fieldInfo.storeType());
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Gets the contact preview details.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param data        Represents the {@link JsonObject}.
     * @param crmResponse Represents the {@link CrmResponse} data.
     */
    private void getContactPreview(final Long orgId, final JsonObject data, final CrmResponse crmResponse, final I18nResolver i18nResolver) {
        final Long primaryId = data.getLong(ContactFields.PRIMARY_CONTACT_ID);
        final Long secondaryId = data.getLong(ContactFields.SECONDARY_CONTACT_ID);

        if (validatePrimaryAndSecondaryEntities(orgId, primaryId, secondaryId,
                ContactErrorCodes.SAME_PRIMARY_AND_SECONDARY_ID_RESTRICTED,
                ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found")) {
            final Contact contact = contactMergeManager.getContactPreviewDetails(orgId, primaryId, secondaryId);

            crmResponse.put(ContactFields.PREVIEW_RESULT, contactConverter.convertPrimaryContactToJson(contact, i18nResolver));
        }
    }

    /**
     * <p>
     * Merges the primary and secondary contact.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param userId      Represents the user id.
     * @param data        Represents the {@link JsonObject}.
     * @param crmResponse Response of the Merged contacts.
     */
    private void mergeContacts(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Long primaryContactId = data.getLong(ContactFields.PRIMARY_CONTACT_ID);
        final Long secondaryContactId = data.getLong(ContactFields.SECONDARY_CONTACT_ID);
        final Collection<Long> duplicateIds = List.of(secondaryContactId);
        final Collection<Long> contactIds = buildAllIds(primaryContactId, duplicateIds);

        validatePrimaryAndDuplicateEntities(orgId, primaryContactId, duplicateIds,
                ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        validateContactEmailAndPhoneLimits(contactManager.getEmails(orgId, contactIds), contactManager.getPhones(orgId, contactIds));
        final Map<String, Object> mergedData = contactMergeManager.mergeContacts(orgId, userId, primaryContactId, duplicateIds, contactIds);
        final Contact retainedContact = mergedData.containsKey(ContactFields.CONTACT) ?
                (Contact) mergedData.get(ContactFields.CONTACT) : contactMergeManager.get(orgId, primaryContactId);

        crmResponse.put(ContactFields.MERGED_STATUS, Map.of(ContactFields.MERGED_RECORDS, mergedData.get(ContactFields.MERGED_RECORDS)));
        publishMergeIndexData(orgId, retainedContact, duplicateIds);
    }

    /**
     * <p>
     * Validates the number of contact emails and phone numbers against the maximum allowed limits.
     * </p>
     *
     * @param contactEmails Which contains the collection of {@link Email} associated with the contact.
     * @param contactPhones Which contains the collection of {@link Phone} associated with the contact.
     */
    private void validateContactEmailAndPhoneLimits(final Collection<Email> contactEmails, final Collection<Phone> contactPhones) {
        final String phoneField = ContactFields.PHONES;
        final String emailField = ContactFields.EMAILS;
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField, phoneField);
        final boolean isEmailLimitExceeded = contactEmails.size() > fields.get(emailField).presetConfig().maxLimit();
        final boolean isPhoneLimitExceeded = contactPhones.size() > fields.get(phoneField).presetConfig().maxLimit();

        if (isEmailLimitExceeded && isPhoneLimitExceeded) {
            throw ErrorCode.get(ContactErrorCodes.EMAILS_AND_PHONES_LIMIT_EXCEEDED, "i18n.contact.duplicate.email.phone.maximum.limit.reached");
        } else if (isEmailLimitExceeded) {
            throw ErrorCode.get(ContactErrorCodes.EMAILS_LIMIT_EXCEEDED, "i18n.contact.duplicate.email.maximum.limit.reached");
        } else if (isPhoneLimitExceeded) {
            throw ErrorCode.get(ContactErrorCodes.PHONES_LIMIT_EXCEEDED, "i18n.contact.duplicate.phone.maximum.limit.reached");
        }
    }
}


