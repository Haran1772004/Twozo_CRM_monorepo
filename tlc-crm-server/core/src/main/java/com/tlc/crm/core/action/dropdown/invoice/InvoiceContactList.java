package com.tlc.crm.core.action.dropdown.invoice;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Same as {@code contactList}; only the {@code company} key is a JSON object with id, name and address.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Dropdown(name = "invoiceContactList", fieldAlias = "invoiceContact")
public class InvoiceContactList extends AbstractDropdownAction {

    private final ContactManager contactManager;
    private final ContactConverter contactConverter;
    private final FieldService fieldService;

    public InvoiceContactList() {
        contactManager = ContactManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        fieldService = FieldService.getInstance();
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
        final JsonObject data = dropdownRequest.data();
        final JsonObject criteria = data.optJsonObject(FieldKeys.CRITERIA.getName());
        final String field = Objects.isNull(criteria) ? null : criteria.optString(FieldKeys.FIELD.getName(), null);
        final boolean includeEmails = dropdownRequest.data().optBoolean(ContactFields.INCLUDE_EMAILS, false);
        final boolean includePhones = data.optBoolean(ContactFields.INCLUDE_PHONES, false);
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int start = dropdownLimit.start();
        final boolean isFieldNull = Objects.isNull(field);
        final long totalRecords;
        final Collection<Contact> contacts;

        if (isFieldNull) {
            totalRecords = contactManager.getTotalCount(orgId, includeEmails, includePhones, dropdownCriteria);
            contacts = (totalRecords > 0 && start <= totalRecords)
                    ? contactManager.getContactDropdownList(orgId, dropdownLimit, includeEmails, includePhones, true, dropdownCriteria)
                    : Collections.emptyList();
        } else {
            final FieldInfo fieldInfo = fieldService.getField(orgId, Long.parseLong(field));

            totalRecords = contactManager.getContactDropdownCount(orgId, fieldInfo, dropdownCriteria);
            contacts = (totalRecords > 0 && start <= totalRecords)
                    ? contactManager.getContactDropdownList(orgId, fieldInfo, dropdownLimit, true, dropdownCriteria)
                    : Collections.emptyList();
        }
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        for (final Contact contact : contacts) {
            final JsonObject contactJson = Json.object();
            final Long id = contact.id();

            contactJson.put(ContactFields.VALUE, id);
            contactJson.put(ContactFields.NAME, contact.getFullName());

            if (isFieldNull && includeEmails) {
                final Collection<Email> emails = contact.getEmails();
                final Email firstEmail = Objects.nonNull(emails) ? emails.stream().findFirst().orElse(null) : null;

                if (Objects.nonNull(firstEmail)) {
                    final JsonObject emailJson = Json.object()
                            .put(ContactFields.ID, firstEmail.id())
                            .put(ContactFields.VALUE, firstEmail.getValue());
                    final Email primaryEmail = contact.getPrimaryEmail();

                    if (Objects.nonNull(primaryEmail) && Objects.equals(primaryEmail.id(), firstEmail.id())) {
                        emailJson.put(ContactFields.IS_PRIMARY, true);
                    }
                    contactJson.put(ContactFields.EMAIL, emailJson);
                }

                if (includePhones) {
                    final Phone primaryPhone = contact.getPrimaryPhone();

                    if (Objects.nonNull(primaryPhone)) {
                        contactJson.put(ContactFields.PHONE, primaryPhone.getNumber());
                    }
                }
            } else {
                final Email email;

                if (Objects.nonNull(field)) {
                    final Collection<Email> emails = contact.getEmails();

                    email = Objects.nonNull(emails) ? emails.stream().findFirst().orElse(null) : null;
                } else {
                    email = contact.getPrimaryEmail();
                }

                if (Objects.isNull(email)) {
                    final Phone phone;

                    if (Objects.nonNull(field)) {
                        final Collection<Phone> phones = contact.getPhones();

                        phone = Objects.nonNull(phones) ? phones.stream().findFirst().orElse(null) : null;
                    } else {
                        phone = contact.getPrimaryPhone();
                    }

                    if (Objects.nonNull(phone)) {
                        contactJson.put(ContactFields.PHONE, phone.getNumber());
                    }
                } else {
                    contactJson.put(ContactFields.EMAIL, email.getValue());
                }
            }
            final Company company = contact.getCompany();

            if (Objects.nonNull(company)) {
                contactJson.put(ContactFields.COMPANY, CompanyConverter.getInstance().convertCompanyWithAddress(false, company));
            }
            final DisplayPicture thumbnail = contact.getThumbnail();

            if (Objects.nonNull(thumbnail)) {
                contactJson.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
            }
            optionContainer.addOption(contactJson);
        }
        return optionContainer;
    }
}
