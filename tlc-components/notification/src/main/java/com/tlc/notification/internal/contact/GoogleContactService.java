package com.tlc.notification.internal.contact;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Birthday;
import com.google.api.services.people.v1.model.Date;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Membership;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.api.services.people.v1.model.Photo;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.profilePicture.ProfilePicture;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.notification.fields.SyncKeys;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.TokenType;
import com.tlc.notification.model.contact.Address;
import com.tlc.notification.model.contact.ContactConfigurations;
import com.tlc.notification.model.contact.ProfilePic;
import com.tlc.notification.model.sync.ContactGroup;
import com.tlc.notification.model.sync.GmailContactGroup;
import com.tlc.notification.service.contact.ContactService;
import com.tlc.notification.service.contact.ContactTokenService;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.service.token.ContactSyncTokenService;
import com.tlc.web.HttpConnectionHelper;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/**
 * <p>
 * Provides the contact sync services for the GMAIL provider
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class GoogleContactService implements ContactService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleContactService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String RETRIEVE_FIELDS = "metadata,names,emailAddresses,phoneNumbers,addresses,biographies,organizations,birthdays,memberships,photos";
    private static final String SOURCE = "people/me";
    private static final String UPDATE_FIELDS = "names,emailAddresses,phoneNumbers,addresses,organizations,birthdays";
    private static final int ZERO = 0;

    private final ContactTokenService contactTokenService;
    private final ContactSyncTokenService contactSyncTokenService;
    private String clientId;
    private String clientSecret;
    private String applicationName;

    private GoogleContactService() {
        contactTokenService = ContactTokenService.getInstance();
        contactSyncTokenService = ContactSyncTokenService.getInstance();
    }

    private static class Instance {
        private static final GoogleContactService INSTANCE = new GoogleContactService();
    }

    /**
     * <p>
     * Gets the {@link ContactService} Instance
     * </p>
     *
     * @return {@link ContactService}
     */
    public static GoogleContactService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param emailAppConfigs Represents the {@link Configurations}
     */
    public void setAppProperties(final Configurations emailAppConfigs) {
        final Configurations configurations = emailAppConfigs.getNestedConfigs(com.tlc.notification.fields.NotificationKeys.GMAIL.getKey());

        this.clientId = configurations.getConfig(SyncKeys.APP_CLIENT_ID);
        this.clientSecret = configurations.getConfig(SyncKeys.APP_CLIENT_SECRET);
        this.applicationName = configurations.getConfig(SyncKeys.NAME);
        LOGGER.info("Gmail provider sync properties has been registered successfully");
    }

    /**
     * {@inheritDoc}
     *
     * @param contactConfigurations Represents the {@link ContactConfigurations}
     * @return the collection of {@link com.tlc.notification.model.contact.Person}
     */
    @Override
    public Map<String, Collection<com.tlc.notification.model.contact.Person>> retrieve(final ContactConfigurations contactConfigurations) {
        final ContactGroup contactGroup = ContactGroup.get(ContactProvider.GOOGLE, contactConfigurations.folderId());

        if (Objects.nonNull(contactGroup)) {
            final com.tlc.notification.model.email.EmailAddress syncedAddress = contactConfigurations.emailAddress();

            try {
                final PeopleService.People.Connections.List list = getPersonService(syncedAddress).people().connections().list(SOURCE)
                        .setPersonFields(RETRIEVE_FIELDS).setRequestSyncToken(true);
                final String value = StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, String.valueOf(syncedAddress.orgId()), syncedAddress.getEmailId());
                final String syncToken = contactTokenService.getSyncToken(value);
                final String pageToken = contactTokenService.getPageToken(value);
                final ListConnectionsResponse response = Objects.isNull(pageToken) ? Objects.isNull(syncToken) ? list.execute()
                        : list.setSyncToken(syncToken).execute() : list.setPageToken(pageToken).execute();
                final List<Person> persons = response.getConnections();
                final Map<String, Collection<com.tlc.notification.model.contact.Person>> personList = new HashMap<>();

                if (Objects.nonNull(persons)) {
                    final Collection<com.tlc.notification.model.contact.Person> deletedPersons = new ArrayList<>();
                    final Collection<com.tlc.notification.model.contact.Person> modifiedPersons = new ArrayList<>();
                    final String starredKey = NotificationKeys.STARRED.getKey();
                    final String myContactsKey = NotificationKeys.MY_CONTACTS.getKey();
                    final String trashContactsKey = NotificationKeys.TRASH_CONTACTS.getKey();

                    for (final Person person : persons) {
                        final com.tlc.notification.model.contact.Person contactPerson = new com.tlc.notification.model.contact.Person();
                        final List<Name> names = person.getNames();
                        final Boolean isDeleted = person.getMetadata().getDeleted();
                        final String resourceName = person.getResourceName();

                        contactPerson.setExternalPersonId(resourceName);
                        contactPerson.setDeleted(Objects.nonNull(isDeleted) && isDeleted);

                        final String membershipName = GmailContactGroup.ALL_CONTACTS == contactGroup ? myContactsKey : starredKey;
                        final List<Membership> memberships = Objects.nonNull(person.getMemberships()) ? person.getMemberships() : Collections.emptyList();
                        final List<Membership> filteredMemberships = memberships.stream().filter(membership -> membershipName
                                .equals(membership.getContactGroupMembership().getContactGroupId())).toList();

                        if (!filteredMemberships.isEmpty()) {
                            final List<com.google.api.services.people.v1.model.EmailAddress> emailAddresses = person.getEmailAddresses();
                            final List<PhoneNumber> phoneNumbers = person.getPhoneNumbers();

                            contactPerson.setEmailAddresses(Objects.nonNull(emailAddresses) && emailAddresses.size() < LIMIT ?
                                    emailAddresses.stream().map(com.google.api.services.people.v1.model.EmailAddress::getValue).toList() : Collections.emptyList());
                            contactPerson.setPhoneNumbers(Objects.nonNull(phoneNumbers) && phoneNumbers.size() < LIMIT ?
                                    phoneNumbers.stream().map(PhoneNumber::getValue).toList() : Collections.emptyList());

                            if (!contactPerson.getPhoneNumbers().isEmpty() || !contactPerson.getEmailAddresses().isEmpty()) {
                                final List<Organization> organizations = person.getOrganizations();
                                final List<Photo> photos = person.getPhotos();

                                if (Objects.nonNull(organizations)) {
                                    final Organization organization = organizations.get(ZERO);

                                    contactPerson.setJobTitle(organization.getTitle());
                                    contactPerson.setCompany(organization.getName());
                                }

                                if (Objects.nonNull(photos) && !photos.isEmpty()) {
                                    final Photo photo = person.getPhotos().get(ZERO);

                                    if (Objects.isNull(photo.getDefault())) {

                                        try (final HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper()) {
                                            final ProfilePic profilePic = new ProfilePic();
                                            final URL url = new URL(photo.getUrl());
                                            final BufferedImage image = ImageIO.read(url);
                                            final int bytesPerPixel = image.getColorModel().getPixelSize() / 8;
                                            final File file = httpConnectionHelper.fetchFile(new HttpRequest(url, Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));
                                            final String contentType = file.mediaType();

                                            profilePic.setImageAsBytes(file.bytes());
                                            profilePic.setSize(image.getHeight() * image.getWidth() * bytesPerPixel);
                                            profilePic.setExtension(ProfilePicture.Extension.get(contentType.substring(contentType.lastIndexOf('/') + 1).toUpperCase()));
                                            contactPerson.setProfilePicture(profilePic);
                                        } catch (Exception exception) {
                                            LoggerUtil.logError(LOGGER, exception, "Profile pic fetch issue for Person-Id: {}, Error: {}", resourceName, exception.getMessage());
                                        }
                                    }
                                }

                                if (Objects.nonNull(names) && !names.isEmpty()) {
                                    final Name name = names.get(ZERO);

                                    contactPerson.setFirstName(name.getGivenName());
                                    contactPerson.setLastName(name.getFamilyName());
                                } else {
                                    if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {
                                        contactPerson.setFirstName(emailAddresses.get(ZERO).getValue().split("@")[ZERO]);
                                    } else if (Objects.nonNull(organizations) && !organizations.isEmpty()) {
                                        contactPerson.setFirstName(person.getOrganizations().get(ZERO).getName());
                                    } else if (Objects.nonNull(phoneNumbers) && !phoneNumbers.isEmpty()) {
                                        contactPerson.setFirstName(phoneNumbers.get(ZERO).getValue());
                                    }
                                }

                                contactPerson.setBirthday(getBirthday(person.getBirthdays()));
                                contactPerson.setAddress(buildAddress(person.getAddresses()));

                                modifiedPersons.add(contactPerson);
                            }
                        }

                        if (contactPerson.isDeleted()) {
                            deletedPersons.add(contactPerson);
                        }
                    }
                    personList.put(myContactsKey, modifiedPersons);
                    personList.put(trashContactsKey, deletedPersons);
                }
                final String nextSyncToken = response.getNextSyncToken();
                final String nextPageToken = response.getNextPageToken();

                if (Objects.nonNull(nextSyncToken) && Objects.isNull(nextPageToken)) {
                    contactTokenService.putSyncToken(value, nextSyncToken);
                }

                if (Objects.isNull(nextPageToken)) {
                    contactTokenService.clearToken(value, TokenType.PAGE_TOKEN);
                } else {
                    contactTokenService.putPageToken(value, nextPageToken);
                }

                return personList;
            } catch (Exception e) {
                LOGGER.debug("Contact create action : failed. Trace: {}", e.getMessage());
                throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_GET_CONTACTS);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * @param contactPerson Represents the {@link com.tlc.notification.model.contact.Person}
     * @param emailAddress  Represents the {@link com.google.api.services.people.v1.model.EmailAddress}
     * @return the contact label
     */
    @Override
    public String addContact(final com.tlc.notification.model.contact.Person contactPerson, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            final Person person = getPersonService(emailAddress).people().createContact(buildContact(contactPerson)).execute();

            return person.getResourceName();
        } catch (IOException exception) {
            LOGGER.debug("Contact add action : failed. Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_ADD_CONTACT);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param person       Represents the {@link com.tlc.notification.model.contact.Person}
     * @param emailAddress Represents the {@link com.google.api.services.people.v1.model.EmailAddress}
     * @return the contact label
     */
    @Override
    public String updateContact(final com.tlc.notification.model.contact.Person person, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            final String personId = person.getExternalPersonId();
            final PeopleService.People peopleService = getPersonService(emailAddress).people();

            if (peopleExists(peopleService, personId)) {
                final Person existingContact = peopleService.get(personId).setPersonFields(NotificationKeys.METADATA.getKey()).execute();
                final Person updatedContact = buildContact(person);

                updatedContact.setEtag(existingContact.getEtag());

                return peopleService.updateContact(personId, updatedContact)
                        .setUpdatePersonFields(UPDATE_FIELDS).execute().getResourceName();
            }
            return null;
        } catch (IOException exception) {
            LOGGER.debug("Contact update action : failed. Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_UPDATE_CONTACT);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param personId     Represents the person id
     * @param emailAddress Represents the {@link com.google.api.services.people.v1.model.EmailAddress}
     * @return true, if contact is deleted
     */
    @Override
    public void deleteContact(final String personId, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            final PeopleService.People people = getPersonService(emailAddress).people();

            if (peopleExists(people, personId)) {
                people.deleteContact(personId).execute();
            }
        } catch (IOException exception) {
            LOGGER.debug("Contact delete action : failed. Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_DELETE_CONTACT);
        }
    }

    /**
     * <p>
     * Checks whether the people exists or not
     * </p>
     *
     * @param people   Represents the {@link com.tlc.notification.model.contact.Person}
     * @param personId Represents the person id
     * @return true, if exists
     */
    private boolean peopleExists(final PeopleService.People people, final String personId) {
        try {
            people.get(personId).setPersonFields(UPDATE_FIELDS).execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>
     * Builds the person for adding/updating the contact
     * </p>
     *
     * @param contactPerson Represents the {@link com.tlc.notification.model.contact.Person}
     * @return the {@link Person}
     */
    private Person buildContact(final com.tlc.notification.model.contact.Person contactPerson) {
        final Person person = new Person();
        final Name name = new Name();
        final Long birthday = contactPerson.getBirthday();
        final Address address = contactPerson.getAddress();
        final String company = contactPerson.getCompany();
        final Collection<String> phones = contactPerson.getPhoneNumbers();
        final Collection<String> emails = contactPerson.getEmailAddresses();
        final String jobTitle = contactPerson.getJobTitle();

        name.setGivenName(contactPerson.getFirstName());
        name.setFamilyName(contactPerson.getLastName());
        person.setNames(List.of(name));

        if (Objects.nonNull(birthday)) {
            final Date date = new Date();
            final Calendar calendar = Calendar.getInstance();

            calendar.setTime(new java.util.Date(birthday));
            date.setYear(calendar.get(Calendar.YEAR));
            date.setMonth(calendar.get(Calendar.MONTH) + 1);
            date.setDay(calendar.get(Calendar.DAY_OF_MONTH));

            person.setBirthdays(List.of(new Birthday().setDate(date)));
        }

        if (Objects.nonNull(address)) {
            final com.google.api.services.people.v1.model.Address peopleAddress = new com.google.api.services.people.v1.model.Address();

            peopleAddress.setStreetAddress(address.getAddressLine1());
            peopleAddress.setExtendedAddress(address.getAddressLine2());
            peopleAddress.setCity(address.getCity());
            peopleAddress.setCountry(address.getCountry());
            peopleAddress.setPostalCode(address.getZipcode());
            peopleAddress.setRegion(address.getState());

            person.setAddresses(List.of(peopleAddress));
        }

        if (Objects.nonNull(company)) {
            final Organization organization = new Organization();

            organization.setName(company);
            person.setOrganizations(List.of(organization));
        }

        if (Objects.nonNull(jobTitle)) {
            List<Organization> organizations = person.getOrganizations();

            if (Objects.isNull(organizations)) {
                organizations = new ArrayList<>();

                person.setOrganizations(organizations);
            }
            final Organization organization = organizations.isEmpty() ? new Organization() : organizations.get(0);

            organization.setTitle(jobTitle);
            person.setOrganizations(List.of(organization));
        }

        if (Objects.nonNull(phones)) {
            final List<PhoneNumber> phoneNumbers = phones.stream()
                    .map(phone -> new PhoneNumber().setValue(phone)).toList();

            person.setPhoneNumbers(phoneNumbers);
        }

        if (Objects.nonNull(emails)) {
            final List<com.google.api.services.people.v1.model.EmailAddress> emailAddresses = emails.stream()
                    .map(email -> new com.google.api.services.people.v1.model.EmailAddress().setValue(email)).toList();

            person.setEmailAddresses(emailAddresses);
        }

        return person;
    }

    /**
     * <p>
     * Gets the persons service for performing various functions like create, update or delete
     * </p>
     *
     * @param emailAddress Represents the {@link com.google.api.services.people.v1.model.EmailAddress}
     * @return the {@link PeopleService}
     */
    private PeopleService getPersonService(final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            return new PeopleService.Builder(httpTransport, JSON_FACTORY,
                    getCredentials(httpTransport, emailAddress)).setApplicationName(applicationName).build();
        } catch (Exception e) {
            throw ErrorCode.get(NotificationErrorCodes.BAD_REQUEST_ERROR);
        }
    }

    /**
     * <p>
     * Gets the credentials for starting the operations like sync, delete, update in provider
     * </p>
     *
     * @param httpTransport Represents the {@link NetHttpTransport}
     * @param emailAddress  Represents the {@link com.google.api.services.people.v1.model.EmailAddress}
     * @return the {@link Credential}
     */
    private Credential getCredentials(final NetHttpTransport httpTransport, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport).setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret).build();
        final EmailAddress address = new EmailAddress(emailAddress.getEmailId());
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(address, TokenRequest.TokenType.ACCESS);
        final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(address, TokenRequest.TokenType.REFRESH);

        credential.setAccessToken(contactSyncTokenService.getAccessToken(accessTokenReadRequest));
        credential.setRefreshToken(contactSyncTokenService.getRefreshToken(refreshTokenReadRequest));

        return credential;
    }

    /**
     * <p>
     * Builds the address from the data synced from provider
     * </p>
     *
     * @param addresses Represents the addresses
     * @return the {@link Address}
     */
    private Address buildAddress(final List<com.google.api.services.people.v1.model.Address> addresses) {
        if (Objects.nonNull(addresses)) {
            final com.google.api.services.people.v1.model.Address address = addresses.get(0);
            final Address personAddress = new Address();

            personAddress.setAddressLine1(address.getStreetAddress());
            personAddress.setAddressLine2(address.getExtendedAddress());
            personAddress.setCountry(address.getCountry());
            personAddress.setState(address.getRegion());
            personAddress.setCity(address.getCity());
            personAddress.setZipcode(address.getPostalCode());

            return personAddress;
        }
        return null;
    }

    /**
     * <p>
     * Gets the birthday timestamp
     * </p>
     *
     * @param birthdays Represents the list of {@link Birthday}
     * @return the birthday time
     */
    private Long getBirthday(final List<Birthday> birthdays) {
        if (Objects.nonNull(birthdays)) {
            final Date date = birthdays.get(0).getDate();
            final Integer month = date.getMonth();
            final Integer day = date.getDay();
            final Integer year = date.getYear();

            if (Objects.nonNull(month) && Objects.nonNull(day) && Objects.nonNull(year)) {
                final Calendar calendar = Calendar.getInstance();

                calendar.set(Calendar.MONTH, month - 1);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.YEAR, year);
                calendar.setTimeZone(TimeZone.getTimeZone(NotificationKeys.UTC.getKey()));

                return calendar.getTimeInMillis();
            }
        }

        return null;
    }
}