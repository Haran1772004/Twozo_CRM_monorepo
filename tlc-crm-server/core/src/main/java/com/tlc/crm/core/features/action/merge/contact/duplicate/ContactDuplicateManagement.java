package com.tlc.crm.core.features.action.merge.contact.duplicate;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.action.merge.EntityMergeManagement;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.contact.manager.ContactMergeManager;
import com.tlc.crm.core.field.EntityMergeFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages the duplicate and merge contact related web actions
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS)
@WebAction(path = "/contact/duplicate/mgmt")
public class ContactDuplicateManagement extends EntityMergeManagement<Contact> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ContactConverter contactConverter;
    private final ContactManager contactManager;
    private final SourceActivityManager sourceActivityManager;
    private final ContactMergeManager contactDuplicateManager;
    private final EmailConverter emailConverter;
    private final PhoneConverter phoneConverter;
    private final FieldService fieldService;

    public ContactDuplicateManagement() {
        contactManager = ContactManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        contactDuplicateManager = ContactMergeManager.getInstance();
        emailConverter = EmailConverter.getInstance();
        fieldService = FieldService.getSecureInstance();
        phoneConverter = PhoneConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    protected void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(ContactFields.DATA);
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final String type = requestJson.getString(ContactFields.TYPE);

        switch (type) {
            case EntityMergeFields.REVIEW -> reviewContacts(orgId, data, response);
            case EntityMergeFields.PREVIEW -> response.put(ContactFields.PREVIEW_RESULT, previewContacts(data, orgId));
            case EntityMergeFields.MERGE -> mergeContacts(data, orgId, userId, response);
            case EntityMergeFields.LIST -> processContactList(data, orgId, response);
            case EntityMergeFields.CHECK_DUPLICATES -> getContactDuplicates(orgId, data, request, response);
            case EntityMergeFields.MERGE_DUPLICATES -> mergeDuplicates(orgId, data, request, response);
            case EntityMergeFields.DISMISS -> dismissContactFields(orgId, data, response);
        }
    }

    @Override
    protected EntityConfigManager<Contact> getEntityManager() {
        return contactManager;
    }

    /**
     * <p>
     * Review contacts which contains the both primary and duplicate contacts records
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Contains the JSON object
     * @param response Response to be sent
     */
    private void reviewContacts(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray duplicateContactArray = Json.array();
        final Contact primaryContact = contactDuplicateManager.getPrimaryContact(orgId, data.getLong(ContactFields.CONTACT_ID));
        final long totalRecords = contactDuplicateManager.getDuplicateContactCount(orgId, primaryContact);
        final int begin = data.optInt(ContactFields.START, DEFAULT_START_VALUE);
        final int count = data.optInt(ContactFields.LIMIT, DEFAULT_LIMIT);
        final int start = begin > 0 ? begin : DEFAULT_START_VALUE;
        final int limit = count > 0 ? count : DEFAULT_LIMIT;
        final boolean hasDuplicates = totalRecords > 1;

        if (hasDuplicates) {
            final Collection<Contact> contacts = contactDuplicateManager.getDuplicateContacts(orgId, primaryContact, start, limit);

            for (final Contact contact : contacts) {
                duplicateContactArray.put(contactConverter.convertDuplicateContactToJson(contact, primaryContact));
            }
        }
        response.put(ContactFields.TOTAL_RECORDS, hasDuplicates ? totalRecords : totalRecords - 1);
        response.put(ContactFields.HAS_MORE, hasDuplicates && (start + limit) <= totalRecords);
        response.put(ContactFields.DUPLICATES, duplicateContactArray);
    }

    /**
     * <p>
     * Gets the preview contact records which contains the preview result before merging
     * </p>
     *
     * @param orgId Represents the organization id
     * @param data  Contains the JSON object
     * @return {@link JsonArray} of preview contacts
     */
    private JsonArray previewContacts(final JsonObject data, final Long orgId) {
        final JsonArray contacts = Json.array();
        final Long primaryContactId = data.getLong(ContactFields.PRIMARY_CONTACT_ID);
        final JsonArray duplicateContactIdsArray = data.optJsonArray(ContactFields.DUPLICATE_CONTACT_IDS);
        final Collection<Long> duplicateContactIds = buildDuplicateEntityIds(duplicateContactIdsArray);
        final Collection<Long> contactIds = buildAllIds(primaryContactId, duplicateContactIds);

        validateExists(contactManager.exists(orgId, contactIds), ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        final Contact previewContact = contactDuplicateManager.getPreviewContact(orgId, primaryContactId,
                duplicateContactIds, contactIds);

        previewContact.setActivityCount(sourceActivityManager.getContactActivityCount(orgId, contactIds));
        contacts.put(contactConverter.convertDuplicateContactToJson(previewContact));

        return contacts;
    }

    /**
     * <p>
     * Merge the selected duplicate contacts with primary contact
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Contains the JSON object
     * @param crmResponse Response to be sent
     */
    private void mergeContacts(final JsonObject data, final long orgId, final long userId, final CrmResponse crmResponse) {
        final Long primaryContactId = data.getLong(ContactFields.PRIMARY_CONTACT_ID);
        final JsonArray duplicateContactIdsArray = data.optJsonArray(ContactFields.DUPLICATE_CONTACT_IDS);
        final Collection<Long> duplicateContactIds = buildDuplicateEntityIds(duplicateContactIdsArray);
        final Collection<Long> contactIds = buildAllIds(primaryContactId, duplicateContactIds);

        validateExists(contactManager.exists(orgId, contactIds), ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        validateContactEmailAndPhoneLimits(contactManager.getEmails(orgId, contactIds), contactManager.getPhones(orgId, contactIds));

        final Map<String, Object> mergedData = contactDuplicateManager.mergeContacts(orgId, userId, primaryContactId,
                duplicateContactIds, contactIds);
        final Contact retainedContact = mergedData.containsKey(ContactFields.CONTACT) ?
                (Contact) mergedData.get(ContactFields.CONTACT) : contactDuplicateManager.get(orgId, primaryContactId);

        crmResponse.put(ContactFields.MERGED_STATUS, Map.of(ContactFields.MERGED_RECORDS, mergedData.get(ContactFields.MERGED_RECORDS)));
        publishMergeIndexData(orgId, retainedContact, duplicateContactIds);
    }

    /**
     * <p>
     * Processes the primary and secondary duplicate contact list by using orgId
     * </p>
     *
     * @param orgId organizationId
     */
    private void processContactList(final JsonObject data, final long orgId, final CrmResponse response) {
        final JsonArray contactsArray = Json.array();
        final int begin = data.optInt(ContactFields.START, DEFAULT_START_VALUE);
        final int count = data.optInt(ContactFields.LIMIT, DEFAULT_LIMIT);
        final int start = begin > 0 ? begin : DEFAULT_START_VALUE;
        final int limit = count > 0 ? count : DEFAULT_LIMIT;
        final int duplicateEmailCount = contactDuplicateManager.getDuplicateContactsCountByEmail(orgId);
        final int totalRecordsCount = duplicateEmailCount + contactDuplicateManager.getDuplicateContactsCountByPhone(orgId);
        final Counter emailCount = new Counter();

        if (duplicateEmailCount > 0) {
            final Collection<List<Contact>> emailContactList = contactDuplicateManager.getContactsByDuplicateEmail(orgId, limit, start);

            for (final List<Contact> contacts : emailContactList) {

                if (contacts.size() > 1) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(ContactFields.PRIMARY, contactConverter.convertContactEmailToJson(contacts.get(0)));
                    jsonObject.put(ContactFields.SECONDARY, contactConverter.convertContactEmailToJson(contacts.get(1)));
                    contactsArray.put(jsonObject);
                    emailCount.getAndIncrement();
                }
            }
        }
        final int phoneLimit = limit - emailCount.getCount();
        final int phoneStart = start - duplicateEmailCount;

        if (phoneLimit > 0) {
            final Collection<List<Contact>> phoneContactList = contactDuplicateManager.getContactsByDuplicateMobile(orgId, Math.max(phoneStart, 1), phoneLimit);

            for (final List<Contact> contacts : phoneContactList) {

                if (contacts.size() > 1) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(ContactFields.PRIMARY, contactConverter.convertContactPhoneToJson(contacts.get(0)));
                    jsonObject.put(ContactFields.SECONDARY, contactConverter.convertContactPhoneToJson(contacts.get(1)));
                    contactsArray.put(jsonObject);
                }
            }
        }

        response.put(ContactFields.TOTAL_RECORDS, totalRecordsCount);
        response.put(ContactFields.HAS_MORE, (start + limit) <= totalRecordsCount);
        response.put(ContactFields.CONTACTS, contactsArray);
    }

    /**
     * <p>
     * Gets the duplicate contact list by using email and phone
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param request  Refers the {@link CrmRequest}
     * @param data     Represents the JSON object
     * @param response Refers the {@link CrmResponse}
     */
    private void getContactDuplicates(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
        final String emailKey = fields.get(ContactFields.EMAILS).idAsString();
        final String phoneKey = fields.get(ContactFields.PHONES).idAsString();
        final int limit = Math.min(data.optInt(ContactFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final int start = Math.max(data.optInt(ContactFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, data);
        final Contact contact = new Contact();
        final JsonArray emailArray = data.optJsonArray(emailKey);

        if (Objects.nonNull(emailArray)) {

            for (int index = 0; index < emailArray.size(); index++) {
                contact.addEmail(emailConverter.convert(converterConfig, emailArray.optJsonObject(index)));
            }
        }
        final JsonArray phoneArray = data.optJsonArray(phoneKey);

        if (Objects.nonNull(phoneArray)) {

            for (int index = 0; index < phoneArray.size(); index++) {
                contact.addPhone(phoneConverter.convert(converterConfig, phoneArray.optJsonObject(index)));
            }
        }
        final Collection<Email> emails = contact.getEmails();
        final Collection<Phone> phones = contact.getPhones();

        if ((Objects.isNull(emails) || emails.isEmpty()) && (Objects.isNull(phones) || phones.isEmpty())) {
            throw ErrorCode.get(ContactErrorCodes.EMAIL_OR_PHONE_IS_REQUIRED, "i18n.contact.email.or.phone.is.required");
        }
        final long totalRecords = contactDuplicateManager.getDuplicateContactCount(orgId, contact);
        final Collection<Contact> duplicateContacts = NumericalConstant.ZERO < totalRecords && start <= totalRecords ?
                contactDuplicateManager.fetchDuplicateContacts(orgId, contact, start, limit) : Collections.emptyList();
        final JsonArray duplicateContactArray = Json.array();

        for (final Contact duplicateContact : duplicateContacts) {
            duplicateContactArray.put(contactConverter.convertDuplicateContactToJson(duplicateContact, contact));
        }
        response.put(ContactFields.TOTAL_RECORDS, totalRecords);
        response.put(ContactFields.HAS_MORE, (start + limit) <= totalRecords);
        response.put(ContactFields.DUPLICATES, duplicateContactArray);
    }

    /**
     * <p>
     * Merge the given contact payload with primary contact
     * </P>
     *
     * @param orgId       Represents the organization id
     * @param data        Represents the JSON object
     * @param request     Request to be sent
     * @param crmResponse Response to be sent
     */
    private void mergeDuplicates(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse crmResponse) {
        final Long primaryContactId = data.getLong(ContactFields.PRIMARY_CONTACT_ID);

        if (contactManager.exists(orgId, primaryContactId)) {
            final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, data);
            final Contact contact = contactConverter.convert(converterConfig, data.getJsonObject(ContactFields.CONTACT));
            final Collection<Email> emails = contact.getEmails();
            final Collection<Phone> phones = contact.getPhones();
            final Collection<Email> contactEmails = Objects.isNull(emails) || emails.isEmpty() ? Collections.emptyList() : new HashSet<>(emails);
            final Collection<Phone> contactPhones = Objects.isNull(phones) || phones.isEmpty() ? Collections.emptyList() : new HashSet<>(phones);

            if (!contactEmails.isEmpty()) {
                final Collection<Email> primaryContactEmails = contactManager.getEmails(orgId, List.of(primaryContactId));
                final Collection<String> emailValues = contactEmails.stream().map(Email::getValue).collect(Collectors.toSet());

                for (final Email email : primaryContactEmails) {

                    if (!emailValues.contains(email.getValue())) {
                        contactEmails.add(email);
                    }
                }
            }

            if (!contactPhones.isEmpty()) {
                final Collection<Phone> primaryContactPhones = contactManager.getPhones(orgId, List.of(primaryContactId));
                final Collection<String> phonesValues = contactPhones.stream().map(Phone::getNumber).collect(Collectors.toSet());

                for (final Phone phone : primaryContactPhones) {

                    if (!phonesValues.contains(phone.getNumber())) {
                        contactPhones.add(phone);
                    }
                }
            }
            validateContactEmailAndPhoneLimits(contactEmails, contactPhones);
            final Map<String, Object> mergedData = contactDuplicateManager.mergeContactDuplicates(orgId, primaryContactId, contact);
            final Contact retainedContact = Objects.nonNull(mergedData.get(ContactFields.CONTACT))
                    ? (Contact) mergedData.get(ContactFields.CONTACT) : contactDuplicateManager.get(orgId, primaryContactId);

            crmResponse.put(ContactFields.MERGED_STATUS, Map.of(ContactFields.MERGED_RECORDS, mergedData.get(ContactFields.MERGED_RECORDS)));
            publishIndexData(orgId, retainedContact, Action.UPDATE);
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Dismisses contact fields based on the given contact id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void dismissContactFields(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        try {
            final long contactId = data.getLong(ContactFields.ID);
            final String fieldName = data.getString(ContactFields.FIELD_NAME);
            final String value = data.getString(ContactFields.VALUE);

            if (!contactManager.exists(orgId, contactId)) {
                throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
            }
            final Contact contact = contactManager.get(orgId, contactId);
            final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);

            switch (fieldName) {
                case "emails" -> {
                    final Collection<Email> emails = contact.getEmails();
                    final FieldInfo fieldInfo = contactFields.get(fieldName);
                    final Long fieldId = fieldInfo.id();
                    final Long sourceId = fieldInfo.sourceId();
                    final Collection<String> emailAddresses = new HashSet<>();

                    for (final Email email : emails) {
                        final String emailAddress = email.getValue();

                        if (value.equals(emailAddress)) {
                            emailAddresses.add(emailAddress);
                        }
                    }
                    contactDuplicateManager.updateDismissedValues(orgId, fieldId, sourceId, emailAddresses);
                    crmResponse.put(ContactFields.MESSAGE,
                            crmResponse.resolveI18nString("i18n.contact.fields.email.dismiss.success"));
                }
                case "phones" -> {
                    final Collection<Phone> phones = contact.getPhones();
                    final FieldInfo fieldInfo = contactFields.get(fieldName);
                    final Long fieldId = fieldInfo.id();
                    final Long sourceId = fieldInfo.sourceId();
                    final Collection<String> phoneNumbers = new HashSet<>();

                    for (final Phone phone : phones) {
                        final String phoneNumber = phone.getNumber();

                        if (value.equals(phoneNumber)) {
                            phoneNumbers.add(phoneNumber);
                        }
                    }
                    contactDuplicateManager.updateDismissedValues(orgId, fieldId, sourceId, phoneNumbers);
                    crmResponse.put(ContactFields.MESSAGE,
                            crmResponse.resolveI18nString("i18n.contact.fields.phone.dismiss.success"));
                }
                default ->
                        throw ErrorCode.get(ContactErrorCodes.CONTACT_FIELDS_NOT_FOUND, "i18n.contact.fields.not.found");
            }
        } catch (final Exception exception) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_DISMISSAL_FAILED, "i18n.contact.fields.dismiss.failed", exception);
        }
    }

    /**
     * <p>
     * Validates the number of contact emails and phone numbers against the maximum allowed limits.
     * </p>
     *
     * @param contactEmails Which contains the collection of {@link Email} associated with the contact
     * @param contactPhones Which contains the collection of {@link Phone} associated with the contact
     */
    private void validateContactEmailAndPhoneLimits(final Collection<Email> contactEmails, final Collection<Phone> contactPhones) {
        final String phoneField = ContactFields.PHONES;
        final String emailField = ContactFields.EMAILS;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailField, phoneField);
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