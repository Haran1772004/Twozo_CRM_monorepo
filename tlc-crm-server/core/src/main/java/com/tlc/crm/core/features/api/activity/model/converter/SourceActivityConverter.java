package com.tlc.crm.core.features.api.activity.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityGuest;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.field.activity.ActivityHolderFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.activity.model.converter.ActivityConverter;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.l10n.model.country.Country;
import com.tlc.notification.model.email.EmailAddress;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Activity}
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EntityConverter
 */
public class SourceActivityConverter extends EntityConverter<ActivityHolder> {

    private final SourceActivityManager activityManager;
    private final CompanyConverter companyConverter;
    private final ActivityConverter activityConverter;
    private final ContactConverter contactConverter;
    private final DealConverter dealConverter;

    private static class Instance {
        private static final SourceActivityConverter INSTANCE = new SourceActivityConverter();
    }

    /**
     * <p>
     * Gets the {@link SourceActivityConverter} Instance
     * </p>
     *
     * @return {@link SourceActivityConverter}
     */
    public static SourceActivityConverter getInstance() {
        return Instance.INSTANCE;
    }

    private SourceActivityConverter() {
        activityManager = SourceActivityManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        dealConverter = DealConverter.getInstance();
        activityConverter = ActivityConverter.getInstance();
        companyConverter = CompanyConverter.getInstance();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Activity}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return {@link Activity} filled with the data
     */
    @Override
    public ActivityHolder convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {

        if (converterConfig.isPartialData()) {
            return buildActivityByPartialData(converterConfig, jsonObject);
        } else {
            return buildActivity(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Activity}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return {@link Activity} filled with the data
     */
    private ActivityHolder buildActivity(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ActivityHolder activityHolder = new ActivityHolder();

        setJsonToActivity(converterConfig, activityHolder, jsonObject);

        return activityHolder;
    }

    /**
     * <p>
     * Converts the partial json into the Activity
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the partial activity data
     * @return {@link Activity} filled with the activity data
     */
    private ActivityHolder buildActivityByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ActivityHolder activityHolder = activityManager.get(converterConfig.orgId(),
                jsonObject.optLong(ActivityFields.ID.getName(), 0));

        setJsonToActivity(converterConfig, activityHolder, jsonObject);
        return activityHolder;
    }

    /**
     * <p>
     * Sets the json data into the {@link Activity}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activityHolder  An entity that holds the activity attributes
     * @param jsonObject      Which contains the data
     */
    public void setJsonToActivity(final ConverterConfig converterConfig, final ActivityHolder activityHolder, final JsonObject jsonObject) {
        final Activity existingActivity;
        final boolean isPartial = converterConfig.isPartialData();

        if (isPartial) {
            existingActivity = activityHolder.getActivity();
        } else {
            final String idKey = ActivityHolderFields.ID.getName();
            existingActivity = new Activity();

            if (jsonObject.containsKey(idKey)) {
                final long id = jsonObject.getLong(idKey);

                existingActivity.setAssignee(activityManager.get(converterConfig.orgId(), id).getActivity().getAssignee());
            }
        }
        final Activity activity = activityConverter.buildActivity(converterConfig, existingActivity, jsonObject);

        activityHolder.setActivity(activity);
        final String contactIdsKey = ActivityHolderFields.CONTACT_IDS.getName();
        final String dealIdKey = ActivityHolderFields.DEAL_ID.getName();
        final String companyIdKey = ActivityHolderFields.COMPANY_ID.getName();
        final String activityGuestKey = ActivityHolderFields.GUESTS.getName();

        if (jsonObject.containsKey(contactIdsKey)) {
            if (isPartial) {
                activityHolder.setContacts(null);
            }

            if (jsonObject.isKeyPresentWithNullValue(contactIdsKey)) {
                activityHolder.setContacts(null);
            } else {
                final JsonArray contactArray = jsonObject.getJsonArray(contactIdsKey);

                contactArray.forEach(contactId -> {
                    final Contact contact = new Contact();

                    contact.setId((Long) contactId);
                    contact.setOrgId(converterConfig.orgId());
                    activityHolder.addContact(contact);
                });
            }
        }

        if (jsonObject.containsKey(dealIdKey)) {
            if (jsonObject.isKeyPresentWithNullValue(dealIdKey)) {
                activityHolder.setDeal(null);
            } else {
                final Deal deal = new Deal();

                deal.setId(jsonObject.getLong(dealIdKey));
                deal.setOrgId(converterConfig.orgId());
                activityHolder.setDeal(deal);
            }
        }

        if (jsonObject.containsKey(companyIdKey)) {
            if (jsonObject.isKeyPresentWithNullValue(companyIdKey)) {
                activityHolder.setCompany(null);
            } else {
                final Company company = new Company();

                company.setId(jsonObject.getLong(companyIdKey));
                company.setOrgId(converterConfig.orgId());
                activityHolder.setCompany(company);
            }
        }

        if (jsonObject.containsKey(activityGuestKey)) {
            if (jsonObject.isKeyPresentWithNullValue(activityGuestKey)) {
                activityHolder.setActivityGuests(null);
            } else {
                final JsonObject guestContactJson = jsonObject.getJsonObject(activityGuestKey);

                buildActivityGuest(guestContactJson, activityHolder);
            }
        }
    }

    /**
     * <p>
     * Builds the {@link ActivityGuest} json to activity holder details
     * </p>
     *
     * @param activityGuestJson Holds the {@link ActivityGuest}t json details
     * @param activityHolder    Represents the {@link ActivityHolder} details
     */
    private void buildActivityGuest(final JsonObject activityGuestJson, final ActivityHolder activityHolder) {
        final String contactsKey = ActivityHolderFields.CONTACTS.getName();
        final String emailsKey = ActivityHolderFields.EMAILS.getName();

        activityHolder.setActivityGuests(null);

        if (activityGuestJson.containsKey(contactsKey)) {
            final String idKey = ActivityHolderFields.ID.getName();
            final String emailIdKey = ActivityHolderFields.EMAIL_ID.getName();
            final JsonArray contactJsonArray = activityGuestJson.optJsonArray(contactsKey);

            for (int index = 0; index < contactJsonArray.size(); index++) {
                final ActivityGuest activityGuest = new ActivityGuest();
                final JsonObject contactJson = contactJsonArray.getJsonObject(index);
                final Contact contact = new Contact();
                final Email email = new Email();

                contact.setId(contactJson.getLong(idKey));
                email.setId(contactJson.getLong(emailIdKey));
                contact.addEmail(email);
                activityGuest.setContact(contact);
                activityHolder.addActivityGuests(activityGuest);
            }
        }

        if (activityGuestJson.containsKey(emailsKey)) {
            final JsonArray emailJsonArray = activityGuestJson.optJsonArray(emailsKey);

            for (int index = 0; index < emailJsonArray.size(); index++) {
                final JsonObject contactJson = emailJsonArray.getJsonObject(index);
                final ActivityGuest activityGuest = new ActivityGuest();
                final Email email = new Email();
                final String idKey = ContactFields.ID;

                if (contactJson.containsKey(idKey)) {
                    email.setId(contactJson.getLong(idKey));
                }
                email.setValue(contactJson.getString(ContactFields.EMAIL));
                email.setType(Email.Type.WORK);
                email.setOrgId(activityHolder.orgId());
                activityGuest.setEmail(email);
                activityHolder.addActivityGuests(activityGuest);
            }
        }
    }

    /**
     * <p>
     * Converts the {@link Activity} into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activityHolder  An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final ActivityHolder activityHolder) {
        final JsonObject activityJson = activityConverter.convert(converterConfig, activityHolder.getActivity(), Json.object());
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();

        if (!contacts.isEmpty()) {
            final JsonArray contactArray = Json.array();

            contacts.forEach(contact -> contactArray.put(contactConverter.convertContactWithThumbnail(contact)));
            activityJson.put(ActivityHolderFields.CONTACTS.getName(), contactArray);
        }

        if (Objects.nonNull(company)) {
            activityJson.put(ActivityHolderFields.COMPANY.getName(), companyConverter.convertPartialData(company));
        }

        if (Objects.nonNull(deal)) {
            activityJson.put(ActivityHolderFields.DEAL.getName(), dealConverter.convertPartialData(deal));
        }

        if (Objects.nonNull(activityGuests)) {
            activityJson.put(ActivityHolderFields.GUESTS.getName(), convertActivityGuestToJson(activityGuests));
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the {@link Activity} summary into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activityHolder  An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convertSummary(final ConverterConfig converterConfig, final ActivityHolder activityHolder) {
        final JsonObject activityJson = activityConverter.convertSummary(converterConfig, activityHolder.getActivity(), Json.object());
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();
        final JsonArray contactArray = Json.array();
        final String idKey = ContactFields.ID;

        activityHolder.getContacts().forEach(contact -> {
            final JsonObject jsonObject = Json.object();
            final Collection<Email> emails = contact.getEmails();
            final Collection<Phone> phones = contact.getPhones();
            final Company contactCompany = contact.getCompany();
            final DisplayPicture thumbnail = contact.getThumbnail();
            final Long contactId = contact.id();

            jsonObject.put(idKey, contactId);
            jsonObject.put(ContactFields.ORG_ID, contact.orgId());
            jsonObject.put(ContactFields.FULL_NAME, contact.getFullName());
            jsonObject.put(ContactFields.FACEBOOK, contact.getFacebook());
            jsonObject.put(ContactFields.LINKEDIN, contact.getLinkedIn());
            jsonObject.put(ContactFields.TWITTER, contact.getTwitter());

            if (Objects.nonNull(thumbnail)) {
                jsonObject.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(
                        contactId, Contact.ENTITY_TYPE, thumbnail));
            }

            if (Objects.nonNull(emails) && !emails.isEmpty()) {
                final JsonArray emailJsonArray = Json.array();

                for (final Email email : emails) {
                    final JsonObject emailJson = Json.object();

                    emailJson.put(idKey, email.id());
                    emailJson.put(ContactFields.VALUE, email.getValue());
                    emailJsonArray.put(emailJson);
                }
                jsonObject.put(ContactFields.EMAILS, emailJsonArray);
            }

            if (Objects.nonNull(phones) && !phones.isEmpty()) {
                final JsonArray phoneJsonArray = Json.array();

                for (final Phone phone : phones) {
                    final JsonObject phoneJson = Json.object();
                    final Country country = phone.getCountry();

                    if (Objects.nonNull(country)) {
                        phoneJson.put(ContactFields.COUNTRY_CODE, country.getCode());
                    }
                    phoneJson.put(idKey, phone.id());
                    phoneJson.put(ContactFields.NUMBER, phone.getNumber());
                    phoneJsonArray.put(phoneJson);
                }
                jsonObject.put(ContactFields.PHONES, phoneJsonArray);
            }

            if (Objects.nonNull(contactCompany)) {
                jsonObject.put(ContactFields.COMPANY, companyConverter.convertPartialData(contactCompany));
            }
            contactArray.put(jsonObject);
        });

        if (!Json.isEmpty(contactArray)) {
            activityJson.put(ActivityHolderFields.CONTACTS.getName(), contactArray);
        }

        if (Objects.nonNull(company)) {
            activityJson.put(ActivityHolderFields.COMPANY.getName(), companyConverter.convertCompanyWithThumbnail(company));
        }

        if (Objects.nonNull(deal)) {
            activityJson.put(ActivityHolderFields.DEAL.getName(), dealConverter.convertDealToActivityJson(deal));
        }

        if (Objects.nonNull(activityGuests)) {
            activityJson.put(ActivityHolderFields.GUESTS.getName(), convertActivityGuestToJson(activityGuests));
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the collection of contacts into {@link JsonObject}
     * </p>
     *
     * @param activityGuests Represents the {@link ActivityGuest} details
     * @return The {@link JsonObject} details
     */
    public JsonArray convertActivityGuestToJson(final Collection<ActivityGuest> activityGuests) {
        final JsonArray jsonArray = Json.array();
        final Collection<Contact> contacts = activityGuests.stream().map(ActivityGuest::getContact).filter(Objects::nonNull).collect(Collectors.toSet());
        final Collection<Email> emails = activityGuests.stream().map(ActivityGuest::getEmail).filter(Objects::nonNull).collect(Collectors.toSet());

        if (!contacts.isEmpty()) {

            for (final Contact contact : contacts) {
                final Collection<Email> contactEmails = contact.getEmails();

                if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {
                    final JsonObject jsonObject = Json.object();
                    final String name = contact.getFullName();
                    final Email email = contactEmails.stream().findFirst().get();
                    final String value = email.getValue();
                    final Long emailId = email.id();
                    final JsonObject emailJson = Json.object();

                    emailJson.put(ContactFields.ID, emailId);
                    emailJson.put(ContactFields.VALUE, value);
                    jsonObject.put(ContactFields.EMAIL, emailJson);
                    jsonObject.put(ContactFields.NAME, Objects.isNull(name) ? EmailAddress.extractNameFromEmail(value) : name);
                    jsonObject.put(ContactFields.ID, contact.id());
                    jsonArray.put(jsonObject);
                }
            }
        }

        if (!emails.isEmpty()) {

            for (final Email email : emails) {
                final String value = email.getValue();
                final JsonObject jsonObject = Json.object();
                final JsonObject emailJson = Json.object();

                emailJson.put(ContactFields.ID, email.id());
                emailJson.put(ContactFields.VALUE, value);
                jsonObject.put(ContactFields.EMAIL, emailJson);
                jsonObject.put(ContactFields.NAME, EmailAddress.extractNameFromEmail(value));
                jsonArray.put(jsonObject);
            }
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link Activity} into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activityHolder  An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convertActivity(final ConverterConfig converterConfig, final ActivityHolder activityHolder) {
        final JsonObject activityJson = activityConverter.convert(converterConfig, activityHolder.getActivity(), Json.object());
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final Status status = activityHolder.getActivity().getStatus();

        if (!contacts.isEmpty()) {
            final JsonArray contactArray = Json.array();

            contacts.forEach(contact -> contactArray.put(convertContactCardDetailsToJson(contact)));
            activityJson.put(ActivityHolderFields.CONTACTS.getName(), contactArray);
        }

        if (Objects.nonNull(company)) {
            activityJson.put(ActivityHolderFields.COMPANY.getName(), companyConverter.convertCompanyWithThumbnail(company));
        }

        if (Objects.nonNull(deal)) {
            activityJson.put(ActivityHolderFields.DEAL.getName(), dealConverter.convertDealToActivityJson(deal));
        }

        if (Objects.nonNull(status)) {
            activityJson.put(ActivityFields.IS_OVER_DUE.getName(), Status.OVERDUE == status);
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the contact card details into {@link JsonObject}
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return the {@link JsonObject}
     */
    private JsonObject convertContactCardDetailsToJson(final Contact contact) {
        final JsonObject contactJson = Json.object();

        if (Objects.nonNull(contact)) {
            final DisplayPicture thumbnail = contact.getThumbnail();
            final Long id = contact.id();
            final Collection<Phone> contactPhones = contact.getPhones();
            final Collection<Email> contactEmails = contact.getEmails();
            final Company company = contact.getCompany();

            contactJson.put(ContactFields.ID, id);
            contactJson.put(ContactFields.FULL_NAME, contact.getFullName());
            contactJson.put(ContactFields.LINKEDIN, contact.getLinkedIn());
            contactJson.put(ContactFields.FACEBOOK, contact.getFacebook());

            if (Objects.nonNull(company)) {
                contactJson.put(ContactFields.COMPANY, companyConverter.convertPartialData(company));
            }

            if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {
                final JsonArray emailJsonArray = Json.array();

                for (final Email email : contactEmails) {
                    final JsonObject emailJson = Json.object();

                    emailJson.put(ContactFields.ID, email.id());
                    emailJson.put(ContactFields.VALUE, email.getValue());
                    emailJsonArray.put(emailJson);
                }
                contactJson.put(ContactFields.EMAILS, emailJsonArray);
            }

            if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {
                final JsonArray phoneJsonArray = Json.array();

                for (final Phone phone : contactPhones) {
                    final JsonObject phoneJson = Json.object();
                    final Country country = phone.getCountry();

                    if (Objects.nonNull(country)) {
                        phoneJson.put(ContactFields.COUNTRY_CODE, country.getCode());
                    }
                    phoneJson.put(ContactFields.ID, phone.id());
                    phoneJson.put(ContactFields.NUMBER, phone.getNumber());
                    phoneJsonArray.put(phoneJson);
                }
                contactJson.put(ContactFields.PHONES, phoneJsonArray);
            }

            if (Objects.nonNull(thumbnail)) {
                contactJson.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
            }
        }

        return contactJson;
    }

    /**
     * <p>
     * Converts the {@link Activity} into a {@link JsonObject}.
     * </p>
     *
     * @param converterConfig Represents the configuration settings for the conversion process.
     * @param activityHolder  An entity that holds the attributes of the activity, including its contacts, deal, company, and guests.
     * @return {@link JsonObject} A JSON object filled with data extracted from the activity holder, including nested entities.
     */
    public JsonObject convertModelToJson(final ConverterConfig converterConfig, final ActivityHolder activityHolder) {
        final JsonObject activityJson = activityConverter.convertModelToJson(converterConfig, activityHolder.getActivity(), Json.object());
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();

        if (Objects.nonNull(contacts) && !contacts.isEmpty()) {
            final JsonArray contactArray = Json.array();

            contacts.forEach(contact -> contactArray.put(contactConverter.convertContactWithThumbnail(contact)));
            activityJson.put(ActivityHolderFields.CONTACTS.getName(), contactArray);
        }

        if (Objects.nonNull(company)) {
            activityJson.put(ActivityHolderFields.COMPANY.getName(), companyConverter.convertPartialData(company));
        }

        if (Objects.nonNull(deal)) {
            activityJson.put(ActivityHolderFields.DEAL.getName(), dealConverter.convertPartialData(deal));
        }

        if (Objects.nonNull(activityGuests)) {
            activityJson.put(ActivityHolderFields.GUESTS.getName(), convertActivityGuestToJson(activityGuests));
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the {@link ActivityHolder} into a {@link JsonObject}.
     * </p>
     *
     * @param activityHolder An entity that holds the attributes of the activity, including  deal, company
     * @return {@link JsonObject} A JSON object filled with data extracted from the activity holder, including nested entities.
     */
    public JsonObject convertKanbanActivities(final ActivityHolder activityHolder) {
        final Company company = activityHolder.getCompany();
        final Deal deal = activityHolder.getDeal();
        final JsonObject jsonObject = activityConverter.convertKanbanActivities(activityHolder.getActivity());

        if (Objects.nonNull(company)) {
            jsonObject.put(ActivityHolderFields.COMPANY.getName(), companyConverter.convertPartialData(company));
        }

        if (Objects.nonNull(deal)) {
            jsonObject.put(ActivityHolderFields.DEAL.getName(), dealConverter.convertPartialData(deal));
        }

        return jsonObject;
    }
}
