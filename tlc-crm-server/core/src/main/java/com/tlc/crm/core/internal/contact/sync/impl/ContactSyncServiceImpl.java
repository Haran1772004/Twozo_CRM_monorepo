package com.tlc.crm.core.internal.contact.sync.impl;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.generator.FileCompressor;
import com.tlc.attachment.generator.compress.config.ImageCompressConfig;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil.Delimiter;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.attachment.model.FileCategory;
import com.tlc.crm.core.auto.assignment.event.AutoAssignmentEvent;
import com.tlc.crm.core.internal.async.ContactSyncHandler;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;
import com.tlc.crm.notification.app.Event;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.notification.model.contact.ContactConfigurations;
import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.client.service.AttachmentService;

import com.tlc.l10n.model.country.Country;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;
import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.util.TlcModelHandler;

import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.resolver.DateUtil;

import com.tlc.notification.model.contact.Person;
import com.tlc.notification.model.contact.ProfilePic;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.contact.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.crm.core.api.contact.model.Contact.OriginSource.PROVIDER;

/**
 * <p>
 * Provides the services for contact sync
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactSyncServiceImpl implements ContactSyncService, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactSyncServiceImpl.class);
    private static final String IMAGE_TYPE = "image/jpeg";
    private static final int DEFAULT_START = 1;
    private static final long DOB_MIN_LIMIT = 631152000000L;
    private static final long DOB_MAX_LIMIT = 3976214400000L;

    private final ContactSyncManager contactSyncManager;
    private final ContactManager contactManager;
    private final FieldService fieldService;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final UserService userService;
    private final CompanyManager companyManager;
    private final TlcModelHandler tlcModelHandler;
    private final OptionService optionService;
    private final ImageCompressConfig imageCompressConfig;
    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;
    private final ContactService contactService;
    private final IndexDataPublisher indexDataPublisher;

    private ContactSyncServiceImpl() {
        contactSyncManager = ContactSyncManager.getInstance();
        contactManager = ContactManager.getInstance();
        fieldService = FieldService.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        userService = UserService.getInstance();
        companyManager = CompanyManager.getInstance();
        tlcModelHandler = TlcModelHandler.getInstance();
        optionService = OptionService.getInstance();
        imageCompressConfig = new ImageCompressConfig(0.50f, 200, 200);
        dateTimeUtil = DateTimeUtil.getInstance();
        dateUtil = DateUtil.getInstance();
        contactService = ContactService.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
    }

    private static class Instance {
        private static final ContactSyncServiceImpl INSTANCE = new ContactSyncServiceImpl();
    }

    /**
     * <p>
     * Gets instance of {@link ContactSyncServiceImpl}.
     * </p>
     *
     * @return {@link ContactSyncServiceImpl} instance
     */
    public static ContactSyncServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link EmailAddress}
     */
    @Override
    public EmailAddress getSyncedAddress(final long orgId, final long userId) {
        return contactSyncManager.getSyncedAddress(orgId, userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pullContactsFromProvider() {
        loadContacts(new ContactSyncSettingsPageHandler());
    }

    /**
     * <p>
     * Loads the contact sync settings in the given page
     * </p>
     *
     * @param pageHandler Represents the {@link PageHandler}
     */
    private void loadContacts(final PageHandler<ContactSyncSettings> pageHandler) {
        final Page<ContactSyncSettings> currentPage = pageHandler.getCurrentPage();

        if (Objects.nonNull(currentPage)) {
            final Collection<ContactSyncSettings> settings = currentPage.getData();

            if (Objects.nonNull(settings)) {
                final ContactSyncHandler contactSyncHandler = ContactSyncHandler.getInstance();

                for (final ContactSyncSettings contactSyncSettings : settings) {
                    final long orgId = contactSyncSettings.orgId();
                    final long userId = contactSyncSettings.getUserId();
                    final Long syncId = contactSyncSettings.id();

                    secureProcess(orgId, userId, () -> {
                        try {
                            final SyncStatus syncStatus = contactSyncHandler.getSyncStatus(orgId, syncId);

                            if (Objects.nonNull(syncStatus) && SyncStatus.SYNCED == syncStatus) {
                                loadContacts(contactSyncSettings);
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Contact Sync: Sync action failed for Org ID: {}, User ID: {}, Sync ID: {}", orgId, userId, syncId);
                        }
                    });
                }
            }
        }

        if (pageHandler.hasNextPage()) {
            pageHandler.goToNextPage();
            loadContacts(pageHandler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pushContactsToProvider(final ContactSyncSettings contactSyncSettings) {
        pushContacts(contactSyncSettings);
    }

    /**
     * <p>
     * Load the contact sync settings to push the contacts into the provider
     * </p>
     *
     * @param contactSyncSettings Represents the {@link ContactSyncSettings}
     */
    private void pushContacts(final ContactSyncSettings contactSyncSettings) {
        final PageHandler<Contact> contactPageHandler = new SyncedContactPageHandler();

        contactPageHandler.setOrgId(contactSyncSettings.orgId());
        contactPageHandler.setUserId(contactSyncSettings.getUserId());
        loadContactsByPagination(contactSyncSettings, contactPageHandler);
    }

    /**
     * <p>
     * Loads the contacts based on pagination
     * </p>
     *
     * @param contactSyncSettings Represents the {@link ContactSyncSettings}
     * @param pageHandler         Represents the {@link PageHandler}
     */
    private void loadContactsByPagination(final ContactSyncSettings contactSyncSettings, final PageHandler<Contact> pageHandler) {
        final Page<Contact> currentPage = pageHandler.getCurrentPage();

        if (Objects.nonNull(currentPage)) {
            final Collection<Contact> contacts = currentPage.getData();

            if (Objects.nonNull(contacts) && !contacts.isEmpty()) {
                final Long userId = contactSyncSettings.getUserId();
                final Long orgId = contactSyncSettings.orgId();

                secureProcess(orgId, userId, () -> {
                    final EmailAddress address = contactSyncSettings.getAddress();

                    for (final Contact contact : contacts) {
                        final String externalPersonId = contactService.addContact(convertContactToPerson(contact,
                                orgId), address);

                        contact.setOwnerId(userId);
                        contact.setExternalPersonId(externalPersonId);
                        contactSyncManager.loadSyncedContactRowIntoContainer(contact, contactSyncSettings.id(), null);
                    }
                });
            }
        }

        if (pageHandler.hasNextPage()) {
            pageHandler.goToNextPage();
            loadContactsByPagination(contactSyncSettings, pageHandler);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the contact id
     * @return the contact label
     */
    @Override
    public String getExternalPersonId(final long orgId, final Long id) {
        return contactSyncManager.getExternalPersonId(orgId, id);
    }

    /**
     * <p>
     * Converts the contact to person for performing actions in provider
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return the {@link Person}
     */
    @Override
    public Person convertContactToPerson(final Contact contact, final Long orgId) {
        final Person person = new Person();
        final Collection<Phone> phones = contact.getPhones();
        final Collection<Email> emails = contact.getEmails();
        final Address address = contact.getAddress();
        final Company company = contact.getCompany();

        person.setFirstName(contact.getFirstName());
        person.setLastName(contact.getLastName());
        person.setBirthday(dateUtil.getTimeStamp(contact.getDateOfBirth()));
        person.setJobTitle(contact.getDesignation());
        person.setCreatedTime(dateTimeUtil.getTimeStamp(contact.getCreatedTime()));
        person.setModifiedTime(dateTimeUtil.getTimeStamp(contact.getModifiedTime()));

        if (Objects.nonNull(phones)) {
            person.setPhoneNumbers(phones.stream().map(Phone::getNumber).toList());
        }

        if (Objects.nonNull(emails)) {
            person.setEmailAddresses(emails.stream().map(Email::getValue).toList());
        }

        if (Objects.nonNull(address)) {
            person.setAddress(convertAddress(address));
        }

        if (Objects.nonNull(company)) {
            final String name = company.getName();

            person.setCompany(Objects.isNull(name) ? companyManager.partialGet(orgId, company.id()).getName() : name);
        }

        return person;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an owner id
     * @return true, if contact is synced
     */
    @Override
    public boolean isSyncEnabled(final long orgId, final long userId) {
        return contactSyncManager.isSyncEnabled(orgId, userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param ids    Represents the contact ids
     * @return the personal ids with contact id
     */
    @Override
    public Map<Long, String> fetchExternalPersonIds(final long orgId, final long userId, final Collection<Long> ids) {
        return contactSyncManager.fetchExternalPersonIds(orgId, ids);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents a user id
     */
    @Override
    public void stopSync(final long orgId, final long userId) {
        contactSyncManager.stopSync(orgId, userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    @Override
    public void updateContactSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo) {
        contactSyncManager.updateContactSyncStatus(orgId, userId, syncInfo,
                SyncStatus.NEW.equals(syncInfo.syncStatus()) ? null : syncInfo.inProgressConfig());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the contact sync id
     * @return the {@link SyncInfo}
     */
    @Override
    public SyncInfo getContactSyncInfo(final long orgId, final Long syncId) {
        return contactSyncManager.getContactSyncInfo(orgId, syncId);
    }

    /**
     * {@inheritDoc}
     *
     * @param contactSyncSettings Represents the {@link ContactSyncSettings}
     */
    @Override
    public void loadContacts(final ContactSyncSettings contactSyncSettings) {
        final int contactGroupId = contactSyncSettings.getConfig().getGroupTypeId();
        final EmailAddress address = contactSyncSettings.getAddress();
        final Long userId = contactSyncSettings.getUserId();
        final Long orgId = contactSyncSettings.orgId();

        secureProcess(orgId, userId, () -> {
            final Map<String, Collection<Person>> persons = contactService.retrieve(new ContactConfigurations(address, contactGroupId));

            if (Objects.nonNull(persons) && !persons.isEmpty()) {
                final Collection<Person> trashContacts = persons.get(ContactFields.TRASH_CONTACTS);
                final Collection<Person> myContacts = persons.get(ContactFields.MY_CONTACTS);

                if (!myContacts.isEmpty()) {
                    final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
                    final Collection<Contact> syncedContacts = convertToContact(orgId, userId, myContacts, fields);

                    loadContactsIntoContainer(orgId, userId, syncedContacts, fields);
                }

                if (!trashContacts.isEmpty()) {
                    deleteContact(orgId, trashContacts);
                    indexDataPublisher.publishIndexData(orgId, trashContacts, Action.DELETE);
                }
            }
            contactSyncManager.updateLastSyncTime(orgId, userId, address);
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param contactsArray Holds contacts in {@link JsonArray}.
     * @return contacts in a {@link Collection}.
     */
    @Override
    public Collection<Contact> convertJsonToModels(final JsonArray contactsArray) {
        final Collection<Contact> contacts = new HashSet<>();
        final String idKey = ContactFields.ID;
        final String orgIdKey = ContactFields.ORG_ID;
        final String firstNameKey = ContactFields.FIRST_NAME;
        final String lastNameKey = ContactFields.LAST_NAME;
        final String emailsKey = ContactFields.EMAILS;
        final String phonesKey = ContactFields.PHONES;
        final String companyKey = ContactFields.COMPANY;
        final String addressKey = ContactFields.ADDRESS;
        final String birthdayKey = ContactFields.BIRTHDAY;
        final String designationKey = ContactFields.DESIGNATION;
        final String sourceKey = ContactFields.SOURCE;
        final String userIdKey = ContactFields.USER_ID;
        final String originalSizeKey = ContactFields.ORIGINAL_SIZE;
        final String originalExtensionIdKey = ContactFields.ORIGINAL_EXTENSION_ID;
        final String thumbnailSizeKey = ContactFields.THUMBNAIL_SIZE;
        final String displayPictureKey = ContactFields.DISPLAY_PICTURE;

        for (int index = 0; index < contactsArray.size(); index++) {
            final JsonObject contactJson = contactsArray.getJsonObject(index);
            final Contact contact = new Contact();
            final JsonArray emails = contactJson.optJsonArray(emailsKey);
            final JsonArray phones = contactJson.optJsonArray(phonesKey);
            final JsonObject address = contactJson.optJsonObject(addressKey);
            final String company = contactJson.optString(companyKey, null);
            final long orgId = contactJson.getLong(orgIdKey);

            if (Objects.nonNull(emails)) {
                final Collection<Email> contactEmails = new ArrayList<>();

                for (int count = 0; count < emails.size(); count++) {
                    final Email contactEmail = new Email();

                    contactEmail.setValue(emails.getString(count));
                    contactEmails.add(contactEmail);
                }
                contact.setEmails(contactEmails);
            }

            if (Objects.nonNull(phones)) {
                final Collection<Phone> contactPhones = new ArrayList<>();

                for (int count = 0; count < phones.size(); count++) {
                    final Phone contactPhone = new Phone();

                    contactPhone.setNumber(phones.getString(count));
                    contactPhones.add(contactPhone);
                }
                contact.setPhones(contactPhones);
            }

            if (Objects.nonNull(address)) {
                final Address contactAddress = new Address();
                final String countryKey = ContactFields.COUNTRY;

                contactAddress.setAddressLine1(address.optString(ContactFields.ADDRESS_LINE1, contactAddress.getAddressLine1()));
                contactAddress.setAddressLine2(address.optString(ContactFields.ADDRESS_LINE2, contactAddress.getAddressLine2()));
                contactAddress.setState(address.optString(ContactFields.STATE, contactAddress.getState()));
                contactAddress.setCity(address.optString(ContactFields.CITY, contactAddress.getCity()));
                contactAddress.setZipcode(address.optString(ContactFields.ZIPCODE, contactAddress.getZipcode()));
                contact.setAddress(contactAddress);

                if (address.containsKey(countryKey)) {
                    final String countryAsString = address.getString(countryKey);

                    loadCountryIntoAddress(countryAsString, contactAddress);
                }
            }

            if (Objects.nonNull(company)) {
                final Company contactCompany = new Company();

                contactCompany.setName(company);
                contact.setCompany(contactCompany);
            }

            if (contactJson.containsKey(birthdayKey)) {
                contact.setDateOfBirth(new Date(contactJson.getLong(birthdayKey)));
            }

            if (contactJson.containsKey(designationKey)) {
                contact.setDesignation(contactJson.getString(designationKey));
            }

            if (contactJson.containsKey(displayPictureKey)) {
                final JsonObject displayPictureJson = contactJson.getJsonObject(displayPictureKey);
                final SourceDisplayPicture sourceDisplayPicture = new SourceDisplayPicture(orgId, displayPictureJson.getLong(userIdKey));
                final DisplayPicture originalDisplayPicture = new DisplayPicture(ProfilePicture.Quality.ORIGINAL);
                final DisplayPicture thumbnail = new DisplayPicture(ProfilePicture.Quality.THUMBNAIL);

                sourceDisplayPicture.setSource(displayPictureJson.getString(sourceKey));
                originalDisplayPicture.setSize(displayPictureJson.getInt(originalSizeKey));
                originalDisplayPicture.setExtension(ProfilePicture.Extension.get(displayPictureJson.getShort(originalExtensionIdKey)));
                sourceDisplayPicture.setOriginalPicture(originalDisplayPicture);
                thumbnail.setSize(displayPictureJson.getInt(thumbnailSizeKey));
                sourceDisplayPicture.setThumbnail(thumbnail);
                contact.setDisplayPicture(sourceDisplayPicture);
            }
            contact.setFirstName(contactJson.optString(firstNameKey, contact.getFirstName()));
            contact.setLastName(contactJson.optString(lastNameKey, contact.getLastName()));
            contact.setOrgId(orgId);
            contact.setId(contactJson.getLong(idKey));
            contacts.add(contact);
        }
        return contacts;
    }

    /**
     * <p>
     * Converts the contact for syncing from provider
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param persons Represents the {@link Person}
     * @return the collection of {@link Contact}
     */
    private Collection<Contact> convertToContact(final long orgId, final long userId, final Collection<Person> persons,
                                                 final Map<String, FieldInfo> fields) {
        final Collection<Contact> contacts = new ArrayList<>();
        final Long lifecycleStageId = lifeCycleStageManager.getDefaultStageId(orgId);
        final Long lifeCycleStatusId = lifeCycleStatusManager.getDefaultStatusId(orgId, lifecycleStageId);
        final CrmUser user = userService.getUser(orgId, userId);
        final Map<String, Company> companyMap = new HashMap<>();

        for (final Person person : persons) {
            final String personCompany = person.getCompany();
            final com.tlc.notification.model.contact.Address address = person.getAddress();
            final Collection<String> emailAddresses = person.getEmailAddresses();
            final Collection<String> phoneNumbers = person.getPhoneNumbers();
            final ProfilePic profilePicture = person.getProfilePicture();
            final Long modifiedTime = person.getModifiedTime();
            final String externalPersonId = person.getExternalPersonId();
            final boolean personIdExists = contactSyncManager.externalPersonIdExists(orgId, externalPersonId);
            final Contact contact = personIdExists ? contactSyncManager.getContact(orgId, externalPersonId) : new Contact();
            final ValueHolder contactValueHolder = contact.getValueHolder();
            final ValueHolder valueHolder = Objects.isNull(contactValueHolder) ? new ValueHolder() : contactValueHolder;
            final Long dateOfBirth = person.getBirthday();

            if (Objects.nonNull(personCompany)) {
                final Company existingCompany = companyMap.get(personCompany);

                if (Objects.isNull(existingCompany)) {
                    final Collection<Company> companies = companyManager.getCompanies(orgId, List.of(personCompany), Collections.emptyList());

                    if (companies.isEmpty()) {
                        final Company company = new Company();

                        company.setOrgId(orgId);
                        company.setOwnerId(userId);
                        company.setName(personCompany);
                        company.setCreatedBy(user);
                        company.setUpdatedBy(user);
                        company.setSalesOwner(user);
                        company.setValueHolder(new ValueHolder());
                        contact.setCompany(company);
                        companyMap.put(personCompany, company);
                    } else {
                        final Company company = companies.stream().findFirst().orElse(null);

                        contact.setCompany(company);
                        companyMap.put(personCompany, company);
                    }
                } else {
                    contact.setCompany(existingCompany);
                }
            }

            if (Objects.nonNull(address)) {
                contact.setAddress(convertAddress(orgId, address, valueHolder, fields));
            }

            if (Objects.nonNull(profilePicture)) {
                final SourceDisplayPicture displayPicture = new SourceDisplayPicture(orgId, userId);

                displayPicture.setOriginalPicture(constructDisplayPicture(profilePicture, ProfilePicture.Quality.ORIGINAL));
                displayPicture.setThumbnail(constructDisplayPicture(profilePicture, ProfilePicture.Quality.THUMBNAIL));
                displayPicture.setImageAsBytes(profilePicture.getImageAsBytes());
                displayPicture.setSource(Contact.ENTITY_TYPE);

                contact.setDisplayPicture(displayPicture);
            }

            contact.setEmails(emailAddresses.isEmpty() ? Collections.emptyList() :
                    convertEmails(orgId, contact, emailAddresses, valueHolder, fields));
            contact.setPhones(phoneNumbers.isEmpty() ? Collections.emptyList() :
                    convertPhones(orgId, contact, phoneNumbers, valueHolder, fields));

            contact.setOrgId(orgId);
            contact.setFirstName(person.getFirstName());
            contact.setLastName(person.getLastName());
            contact.setModifiedTime(dateTimeUtil.getTemporal(modifiedTime));
            contact.setDateOfBirth(
                    Objects.nonNull(dateOfBirth) && dateOfBirth >= DOB_MIN_LIMIT && dateOfBirth <= DOB_MAX_LIMIT
                            ? dateUtil.getTemporal(dateOfBirth)
                            : null
            );
            contact.setDesignation(person.getJobTitle());
            contact.setExternalPersonId(externalPersonId);
            contact.setOriginSource(Contact.OriginSource.PROVIDER);

            if (!personIdExists) {
                final LifeCycleStage lifeCycleStage = new LifeCycleStage();
                final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

                lifeCycleStage.setId(lifecycleStageId);
                lifeCycleStatus.setId(lifeCycleStatusId);
                contact.setSalesOwner(user);
                contact.setCreatedBy(user);
                contact.setUpdatedBy(user);
                contact.setLifeCycleStage(lifeCycleStage);
                contact.setLifeCycleStatus(lifeCycleStatus);
                contact.setSalesOwner(user);
                contact.setOwnerId(userId);
                setDefaultSubscriptions(orgId, contact, valueHolder);
                contact.setValueHolder(valueHolder);
            }
            DefaultFieldValuesLoader.getInstance().load(contact);
            contacts.add(contact);
        }

        return contacts;
    }

    /**
     * <p>
     * Sets the default subscription for given contact
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param contact     Represents the {@link Contact}
     * @param valueHolder Represents the {@link ValueHolder}
     */
    private void setDefaultSubscriptions(final long orgId, final Contact contact, final ValueHolder valueHolder) {
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String subscriptionTypesKey = ContactFields.SUBSCRIPTION_TYPES;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, List.of(subscriptionStatusKey, subscriptionTypesKey));
        final FieldInfo subscriptionTypesField = fields.get(subscriptionTypesKey);
        final FieldInfo subscriptionStatusField = fields.get(subscriptionStatusKey);
        final Collection<Option> options = optionService.get(orgId, subscriptionTypesField);

        contact.setSubscriptionStatus(optionService.getOption(orgId, subscriptionStatusField, Contact.SubscriptionStatus.SUBSCRIBED.getName()));
        contact.setSubscriptionTypes(options);
        final Long subscriptionTypesFieldId = subscriptionTypesField.id();

        valueHolder.put(subscriptionStatusField.id(), contact.getSubscriptionStatus().id());
        valueHolder.put(subscriptionTypesFieldId, options);
    }

    /**
     * <p>
     * Constructs the display picture for specific quality
     * </p>
     *
     * @param profilePic     Represents the {@link ProfilePic}
     * @param pictureQuality Represents the {@link ProfilePicture.Quality}
     * @return the {@link DisplayPicture}
     */
    private DisplayPicture constructDisplayPicture(final ProfilePic profilePic, final ProfilePicture.Quality pictureQuality) {
        final DisplayPicture displayPicture = new DisplayPicture(pictureQuality);

        displayPicture.setExtension(profilePic.getExtension());
        displayPicture.setSize(profilePic.getSize());

        return displayPicture;
    }

    /**
     * <p>
     * Converts the address to required format
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param addressFormat Represents the address format
     * @param valueHolder   Represents the {@link ValueHolder}
     * @param fields        Represents the fields
     * @return the {@link Address}
     */
    private Address convertAddress(final long orgId, final com.tlc.notification.model.contact.Address addressFormat,
                                   final ValueHolder valueHolder, final Map<String, FieldInfo> fields) {
        final Address address = new Address();
        final String countryAsString = addressFormat.getCountry();

        address.setOrgId(orgId);
        address.setAddressLine1(addressFormat.getAddressLine1());
        address.setAddressLine2(addressFormat.getAddressLine2());
        address.setState(addressFormat.getState());
        address.setCity(addressFormat.getCity());
        address.setZipcode(addressFormat.getZipcode());

        if (Objects.nonNull(countryAsString)) {
            loadCountryIntoAddress(countryAsString, address);
        }

        valueHolder.put(fields.get(ContactFields.ADDRESS).id(), address);
        return address;
    }

    /**
     * <p>
     * Loads the {@link Country} into the {@link Address}.
     * </p>
     *
     * @param countryAsString Represents the country name.
     * @param address         Holds the details of the address.
     */
    private void loadCountryIntoAddress(final String countryAsString, final Address address) {
        final Optional<Country> country = Country.getCountryByName(countryAsString);

        if (country.isPresent()) {
            address.setCountry(country.get());
        } else {
            final String addressLine2 = address.getAddressLine2();

            address.setAddressLine2(Objects.isNull(addressLine2) ? countryAsString : String.format("%s, %s", addressLine2, countryAsString));
        }
    }

    /**
     * <p>
     * Converts the emails to required format
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param emailAddresses Represents the email address
     * @param valueHolder    Represents the {@link ValueHolder}
     * @param fields         Represents the fields
     * @return the collection of email
     */
    private Collection<Email> convertEmails(final long orgId, final Contact contact, final Collection<String> emailAddresses,
                                            final ValueHolder valueHolder, final Map<String, FieldInfo> fields) {
        final Collection<Email> existingEmails = contact.getEmails();
        final Collection<Email> contactEmails = Objects.isNull(existingEmails) ? new ArrayList<>() :
                new ArrayList<>(existingEmails);
        final Collection<String> existingEmailAddresses = contactEmails.stream().map(Email::getValue).toList();
        Email primaryEmail = contact.getPrimaryEmail();

        for (final String emailAddress : emailAddresses) {

            if (!existingEmailAddresses.contains(emailAddress)) {
                final Email email = new Email();

                email.setOrgId(orgId);
                email.setValue(emailAddress);
                email.setType(Email.Type.WORK);

                if (Objects.isNull(primaryEmail)) {
                    primaryEmail = email;
                    contact.setPrimaryEmail(email);
                }
                contactEmails.add(email);
            }
        }
        valueHolder.put(fields.get(ContactFields.EMAILS).id(), contactEmails);

        return contactEmails;
    }

    /**
     * <p>
     * Converts the phones to required format
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param phones      Represents the phones
     * @param valueHolder Represents the {@link ValueHolder}
     * @param fields      Represents the fields
     * @return the collection of phones
     */
    private Collection<Phone> convertPhones(final long orgId, final Contact contact, final Collection<String> phones,
                                            final ValueHolder valueHolder, final Map<String, FieldInfo> fields) {
        final Collection<Phone> phoneNumbers = contact.getPhones();
        final Collection<Phone> contactPhoneNumbers = Objects.isNull(phoneNumbers) ? new ArrayList<>() :
                new ArrayList<>(phoneNumbers);
        final Collection<String> existingPhoneNumbers = contactPhoneNumbers.stream().map(Phone::getNumber).toList();
        Phone primaryPhone = contact.getPrimaryPhone();

        for (final String phone : phones) {

            if (!existingPhoneNumbers.contains(phone)) {
                final Phone phoneNumber = new Phone();

                phoneNumber.setOrgId(orgId);
                phoneNumber.setNumber(phone);
                phoneNumber.setType(Phone.Type.WORK);

                if  (Objects.isNull(primaryPhone)) {
                    primaryPhone = phoneNumber;
                    contact.setPrimaryPhone(phoneNumber);
                }
                contactPhoneNumbers.add(phoneNumber);
            }
        }
        valueHolder.put(fields.get(ContactFields.PHONES).id(), contactPhoneNumbers);

        return contactPhoneNumbers;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param contacts Represents the contacts to be sync
     */
    private void loadContactsIntoContainer(final long orgId, final long userId, final Collection<Contact> contacts,
                                           final Map<String, FieldInfo> fields) {
        final Collection<String> externalPersonIds = contactSyncManager.getExternalPersonIds(orgId, userId);
        final Collection<Contact> existingContacts = new ArrayList<>();
        final Collection<Contact> createActionContacts = new ArrayList<>();
        final Collection<Contact> updateActionContacts = new ArrayList<>();

        contacts.removeIf(contact -> processContact(contact, externalPersonIds, existingContacts, fields));

        if (!existingContacts.isEmpty()) {
            existingContacts.forEach(existingContact -> uploadDisplayPicture(orgId, existingContact));
            contactManager.update(orgId, existingContacts);
            updateActionContacts.addAll(existingContacts);
        }

        if (!contacts.isEmpty()) {
            for (final Contact contact : contacts) {

                if (contactManager.isNameExists(contact)) {
                    final Long existingContactId = contactManager.fetchExistingContactId(contact);

                    if (Objects.nonNull(existingContactId)) {
                        contact.setId(existingContactId);
                        setUpdatedData(contact, fields);
                        uploadDisplayPicture(orgId, contact);
                        contactManager.update(contact);
                        updateActionContacts.add(contact);
                    } else {
                        createContact(orgId, contact, createActionContacts);
                    }
                } else {
                    createContact(orgId, contact, createActionContacts);
                }
            }
        }
        indexDataPublisher.publishIndexData(orgId, createActionContacts, Action.CREATE);
        indexDataPublisher.publishIndexData(orgId, updateActionContacts, Action.UPDATE);

        final EventDataHandler eventDataHandler = EventDataHandler.getInstance();

        for (final Collection<Contact> models : JavaUtil.splitCollection(createActionContacts, NumericalConstant.TWENTY)) {
            final EventData eventData = new SecureEventData(orgId, userId);

            eventData.setAction(Action.CREATE);
            eventData.setValues(models);

            try {
                eventDataHandler.handleEvent(AutoAssignmentEvent.ASSIGN_OWNER, eventData, Event.Type.POST);
                LOGGER.debug("Contact sync: Auto assignment event triggered for synced contact. Org ID: {}, User ID: {}", orgId, userId);
            } catch (Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Contact sync: Auto assignment failed for Org ID: {}, User ID: {}", orgId, userId);
            }
        }
    }

    /**
     * <p>
     * Creates contact and adds created contacts to a collection
     * </p>
     *
     * @param orgId                Represents an Organization ID
     * @param contact              Represents a {@link Contact}
     * @param createActionContacts Represents the {@link Collection} to add the created contacts
     */
    private void createContact(final long orgId, final Contact contact, final Collection<Contact> createActionContacts) {
        uploadDisplayPicture(orgId, contact);
        contactManager.create(orgId, List.of(contact));
        createActionContacts.add(contact);
    }


    /**
     * <p>
     * Uploads the display picture of the contact
     * </p>
     *
     * @param orgId   Refers the organisation id.
     * @param contact Represents the {@link Contact}
     */
    private void uploadDisplayPicture(final long orgId, final Contact contact) {
        final SourceDisplayPicture sourceDisplayPicture = contact.getDisplayPicture();

        if (Objects.nonNull(sourceDisplayPicture)) {

            try {
                final DisplayPicture thumbnailInfo = (DisplayPicture) sourceDisplayPicture.getThumbnail();
                final String extension = thumbnailInfo.extension().getName();
                final Collection<Attachment> attachments = new HashSet<>();
                final File originalFile = new File(new ByteArrayInputStream(sourceDisplayPicture.getImageAsBytes()),
                        String.join(Delimiter.DOT.getDelimiter(), DisplayPictureFields.ORIGINAL, extension), extension,
                        ((Number) sourceDisplayPicture.getOriginalPicture().size()).longValue(), IMAGE_TYPE);

                attachments.add(convertToAttachment(originalFile));
                final File compressedFile = FileCompressor.getImageCompressor().compress(originalFile, imageCompressConfig);
                final File thumbnail = new File(new ByteArrayInputStream(compressedFile.inputStream().readAllBytes()),
                        String.join(Delimiter.DOT.getDelimiter(), DisplayPictureFields.THUMBNAIL, extension), extension, compressedFile.size(),
                        compressedFile.mediaType());

                attachments.add(convertToAttachment(thumbnail));
                thumbnailInfo.setSize(Math.toIntExact(thumbnail.size()));
                final Collection<Attachment> uploadedAttachment = AttachmentService.getInstance().upload(orgId, attachments, FileCategory.SOURCE_PROFILE.getName());

                for (final Attachment attachment : uploadedAttachment) {
                    final Long attachmentId = attachment.id();

                    if (attachment.getFileName().contains(ProfilePictureFields.ORIGINAL)) {
                        sourceDisplayPicture.setOriginalPictureId(attachmentId);
                    } else {
                        sourceDisplayPicture.setThumbnailId(attachmentId);
                    }
                }
            } catch (IOException exception) {
                LoggerUtil.logError(LOGGER, exception, "Contact sync: Contact sync action failed. Reason: {}, OrgId: {}", exception.getMessage(), orgId);
            }
        }
    }

    /**
     * Converts a given {@code File} into an {@code Attachment}.
     *
     * @param file The file to be converted into an attachment.
     * @return The {@code Attachment} instance created from the file.
     */
    private Attachment convertToAttachment(final File file) {
        final Attachment attachment = new ObjectAttachment();

        attachment.setFileName(file.name());
        attachment.setFileSize(file.size());
        attachment.setExtension(file.extension());
        attachment.setMediaType(file.mediaType());
        attachment.setInputStream(file.inputStream());

        return attachment;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param contacts Represents the contacts to be deleted
     */
    private void deleteContact(final long orgId, final Collection<Person> contacts) {
        final Collection<Contact> syncedContacts = contacts.stream()
                .map(person -> {
                    final String externalPersonId = person.getExternalPersonId();

                    if (contactSyncManager.externalPersonIdExists(orgId, externalPersonId)) {
                        final Contact syncedContact = new Contact();

                        syncedContact.setOriginSource(PROVIDER);
                        syncedContact.setExternalPersonId(externalPersonId);
                        return syncedContact;
                    }
                    return new Contact();
                }).toList();

        deleteTrashContacts(orgId, syncedContacts);
    }

    /**
     * <p>
     * Processes the contact synced from the provider
     * </p>
     *
     * @param contact          Represents the {@link Contact}
     * @param resources        Represents the resources
     * @param existingContacts Represents the existing contacts
     * @return true, if contact already exists
     */
    private boolean processContact(final Contact contact, final Collection<String> resources, final Collection<Contact> existingContacts,
                                   final Map<String, FieldInfo> fields) {
        final String externalPersonId = contact.getExternalPersonId();
        final Long orgId = contact.orgId();

        if (resources.contains(externalPersonId)) {
            final Contact existingContact = contactSyncManager.getContact(orgId, externalPersonId);
            final Collection<String> updatedValueFields = tlcModelHandler.fetchUpdatedValueFields(existingContact, contact, false);

            if (updatedValueFields.size() > 0) {
                contact.setId(existingContact.id());
                setUpdatedData(contact, fields);
                existingContacts.add(contact);
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * Sets the new data for updating the contact
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @param fields  Represents the fields
     */
    private void setUpdatedData(final Contact contact, final Map<String, FieldInfo> fields) {
        final ValueHolder valueHolder = contact.getValueHolder();
        final Collection<Phone> phones = contact.getPhones();
        final Collection<Email> emails = contact.getEmails();
        final Long id = contact.id();
        final Long orgId = contact.orgId();
        final Address address = contact.getAddress();

        if (phones.isEmpty()) {
            final Collection<Phone> existingPhones = contactManager.getExistingPhones(id, orgId, fields.get(ContactFields.PHONES).id());

            if (Objects.nonNull(existingPhones)) {
                contact.setPhones(existingPhones);
                contact.setPrimaryPhone(existingPhones.stream().findFirst().orElse(null));
                valueHolder.put(fields.get(ContactFields.PHONES).id(), existingPhones);
            }
        }

        if (emails.isEmpty()) {
            final Collection<Email> existingEmails = contactManager.getExistingEmails(id, orgId, fields.get(ContactFields.EMAILS).id());

            if (Objects.nonNull(existingEmails)) {
                contact.setEmails(existingEmails);
                contact.setPrimaryEmail(existingEmails.stream().findFirst().orElse(null));
                valueHolder.put(fields.get(ContactFields.EMAILS).id(), existingEmails);
            }
        }

        if (Objects.nonNull(address)) {
            address.setId(contactManager.getExistingAddressId(id, orgId, fields.get(ContactFields.ADDRESS).id()));
            valueHolder.put(fields.get(ContactFields.ADDRESS).id(), address);
        }
    }

    /**
     * <p>
     * Deletes the trash contacts
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param contacts Represents the contacts to be deleted
     */
    private void deleteTrashContacts(final long orgId, final Collection<Contact> contacts) {
        final Collection<Contact> deletedContactList = contacts.stream().filter(deletedContact -> Objects.nonNull(deletedContact.getExternalPersonId()))
                .peek(deletedContact -> deletedContact.setId(contactSyncManager.getContactId(orgId, deletedContact.getExternalPersonId()))).toList();

        if (!deletedContactList.isEmpty()) {
            contactManager.delete(orgId, contactManager.get(orgId, deletedContactList.stream().map(Contact::id).toList()));
        }
    }

    /**
     * <p>
     * Converts the address for performing actions in provider
     * </p>
     *
     * @param address Represents the address
     * @return {@link Address}
     */
    private com.tlc.notification.model.contact.Address convertAddress(final Address address) {
        final com.tlc.notification.model.contact.Address personAddress = new com.tlc.notification.model.contact.Address();

        personAddress.setAddressLine1(address.getAddressLine1());
        personAddress.setAddressLine2(address.getAddressLine2());
        personAddress.setCountry(address.getCountryAsString());
        personAddress.setState(address.getState());
        personAddress.setZipcode(address.getZipcode());
        personAddress.setCity(address.getCity());

        return personAddress;
    }

    /**
     * <p>
     * Represent the page list for the synced contact fetch using pagination
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class SyncedContactPageHandler extends PageHandler<Contact> {

        private final ContactManager contactManager;
        private final ContactSyncManager contactSyncManager;

        private SyncedContactPageHandler() {
            this.contactManager = ContactManager.getInstance();
            this.contactSyncManager = ContactSyncManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @return the {@link Page<Contact>}
         */
        @Override
        protected Page<Contact> fetchPageData() {
            final long orgId = getOrgId();
            final Page<Contact> contactPage = new Page<>(DEFAULT_START);
            final Collection<Long> contactsIds = contactSyncManager.fetchContactIds(orgId, getUserId(), DEFAULT_START);

            contactPage.setData(contactsIds.isEmpty() ? null : contactManager.get(orgId, contactsIds));

            return contactPage;
        }

        /**
         * {@inheritDoc}
         *
         * @return the total count
         */
        @Override
        protected long totalRecordsCount() {
            return contactSyncManager.fetchContactCount(getOrgId(), getUserId());
        }
    }

    /**
     * <p>
     * Represent the page list for the contact sync settings
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactSyncSettingsPageHandler extends PageHandler<ContactSyncSettings> {

        private final ContactSyncManager contactSyncManager;

        private ContactSyncSettingsPageHandler() {
            this.contactSyncManager = ContactSyncManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @return the {@link Page<ContactSyncSettings>}
         */
        @Override
        protected Page<ContactSyncSettings> fetchPageData() {
            return contactSyncManager.getCurrentSettingsPage(getRecordStartingIndex(), getRecordsPerPage());
        }

        /**
         * {@inheritDoc}
         *
         * @return the total count
         */
        @Override
        protected long totalRecordsCount() {
            return contactSyncManager.getTotalCountForSyncSettings();
        }
    }
}