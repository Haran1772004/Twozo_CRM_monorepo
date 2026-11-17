package com.tlc.notification.internal.contact;

import com.microsoft.graph.models.Contact;
import com.microsoft.graph.models.PhysicalAddress;
import com.microsoft.graph.models.ProfilePhoto;
import com.microsoft.graph.requests.ContactDeltaCollectionRequest;
import com.microsoft.graph.requests.ContactRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.ProfilePhotoRequestBuilder;
import com.microsoft.graph.requests.UserRequestBuilder;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.contact.Address;
import com.tlc.notification.model.contact.ContactConfigurations;
import com.tlc.notification.model.contact.Person;
import com.tlc.notification.model.contact.ProfilePic;
import com.tlc.notification.model.sync.ContactGroup;
import com.tlc.notification.service.contact.ContactService;
import com.tlc.notification.service.contact.ContactTokenService;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.service.token.ContactSyncTokenService;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import org.apache.commons.lang3.time.DateUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Provides the contact sync services for the OUTLOOK provider
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class OutlookContactService implements ContactService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookContactService.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final int DEFAULT_SIZE = 48;

    private final ContactTokenService contactTokenService;
    private final ContactSyncTokenService contactSyncTokenService;

    private OutlookContactService() {
        contactTokenService = ContactTokenService.getInstance();
        contactSyncTokenService = ContactSyncTokenService.getInstance();
    }

    private static class Instance {
        private static final OutlookContactService INSTANCE = new OutlookContactService();
    }

    /**
     * <p>
     * Gets the {@link ContactService} Instance
     * </p>
     *
     * @return {@link ContactService}
     */
    public static OutlookContactService getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * {@inheritDoc}
     *
     * @param contactConfigurations Represents the {@link ContactConfigurations}
     * @return the collection of {@link Person}
     */
    @Override
    public Map<String, Collection<Person>> retrieve(final ContactConfigurations contactConfigurations) {
        final ContactGroup contactGroup = ContactGroup.get(ContactProvider.OUTLOOK, contactConfigurations.folderId());

        if (Objects.nonNull(contactGroup)) {
            final com.tlc.notification.model.email.EmailAddress emailAddress = contactConfigurations.emailAddress();
            final String emailId = emailAddress.getEmailId();
            final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(new EmailAddress(emailId), TokenRequest.TokenType.ACCESS);
            final String accessToken = contactSyncTokenService.getAccessToken(accessTokenReadRequest);
            final String contactListUrl = getRequestUrl(emailAddress);
            final Map<String, JsonArray> valueAsArray = getTokenWithData(contactListUrl, accessToken);
            final Map<String, Collection<Person>> personList = new HashMap<>();

            for (final Map.Entry<String, JsonArray> entry : valueAsArray.entrySet()) {
                final JsonArray jsonArray = entry.getValue();
                final Collection<Person> modifiedPerson = new ArrayList<>();
                final Collection<Person> deletedPerson = new ArrayList<>();

                for (int index = 0; index < jsonArray.size(); index++) {
                    final JsonObject jsonObject = jsonArray.getJsonObject(index);

                    if (jsonObject.containsKey(NotificationKeys.REMOVED.getKey())) {
                        final Person person = new Person();

                        person.setExternalPersonId(jsonObject.getString(NotificationKeys.ID.getKey()));
                        deletedPerson.add(person);
                    } else {
                        setPersonFields(jsonObject, modifiedPerson, emailAddress);
                    }
                }
                contactTokenService.putSyncToken(StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, String.valueOf(emailAddress.orgId()), emailId), extractToken(entry.getKey()));

                personList.put(NotificationKeys.MY_CONTACTS.getKey(), modifiedPerson);
                personList.put(NotificationKeys.TRASH_CONTACTS.getKey(), deletedPerson);
            }

            return personList;
        }
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * @param person Represents the {@link Person}
     * @return the unique id of contact created
     */
    @Override
    public String addContact(final Person person, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            final Contact contact = buildRequest(emailAddress).contacts().buildRequest().post(buildContact(person));

            return contact.id;
        } catch (Exception e) {
            LOGGER.debug("Contact add action : failed. Trace: {}", e.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_ADD_CONTACT);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param person       Represents the {@link Person}
     * @param emailAddress Represents the {@link com.tlc.notification.model.email.EmailAddress}
     * @return the unique id of contact updated
     */
    @Override
    public String updateContact(final Person person, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            if (contactExists(emailAddress, person.getExternalPersonId())) {
                final Contact contact = buildRequest(emailAddress).contacts(person.getExternalPersonId()).buildRequest().patch(buildContact(person));

                return Objects.requireNonNull(contact).id;
            }
            return null;
        } catch (Exception e) {
            LOGGER.debug("Contact update action : failed. Trace: {}", e.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_UPDATE_CONTACT);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param personId     Represents the person id
     * @param emailAddress Represents the {@link com.tlc.notification.model.email.EmailAddress}
     * @return true, if contact deleted
     */
    @Override
    public void deleteContact(final String personId, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            if (contactExists(emailAddress, personId)) {
                final ContactRequestBuilder contacts = buildRequest(emailAddress).contacts(personId);

                contacts.buildRequest().delete();
            }
        } catch (Exception e) {
            LOGGER.debug("Contact delete action : failed. Trace: {}", e.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_DELETE_CONTACT);
        }
    }

    /**
     * <p>
     * Checks whether the contact is present or not
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param contactId    Represents the contact id
     * @return true, if exists
     */
    private boolean contactExists(final com.tlc.notification.model.email.EmailAddress emailAddress, final String contactId) {
        try {
            final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(new EmailAddress(emailAddress.getEmailId()), TokenRequest.TokenType.ACCESS);
            final String accessToken = contactSyncTokenService.getAccessToken(accessTokenReadRequest);
            final UserRequestBuilder userRequestBuilder = GraphServiceClient.builder().authenticationProvider((token) -> CompletableFuture.completedFuture(accessToken))
                    .buildClient().me();
            final Contact contact = userRequestBuilder.contacts(contactId).buildRequest().get();

            return Objects.nonNull(contact);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>
     * Sets the person fields in the list given
     * </p>
     *
     * @param json       Represents the {@link JsonObject}
     * @param personList Represents the person list
     */
    private void setPersonFields(final JsonObject json, final Collection<Person> personList, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final String displayName = (String) json.opt(NotificationKeys.GIVEN_NAME.getKey());
        final Collection<String> emails = buildEmails(json);
        final Collection<String> phones = buildPhones(json);

        if (Objects.nonNull(displayName)) {
            final Person person = new Person();

            person.setEmailAddresses(!emails.isEmpty() && emails.size() < LIMIT ? emails : Collections.emptyList());
            person.setPhoneNumbers(!phones.isEmpty() && phones.size() < LIMIT ? phones : Collections.emptyList());

            if (!person.getEmailAddresses().isEmpty() || !person.getPhoneNumbers().isEmpty()) {
                final String jobTitle = (String) json.opt(NotificationKeys.JOB_TITLE.getKey());
                final String companyName = (String) json.opt(NotificationKeys.COMPANY_NAME.getKey());
                final String personId = json.getString(NotificationKeys.ID.getKey());

                person.setFirstName(displayName);
                person.setLastName((String) json.opt(NotificationKeys.SURNAME.getKey()));
                person.setCompany(Objects.isNull(companyName) ? null : companyName.isEmpty() ? null : companyName);
                person.setJobTitle(Objects.isNull(jobTitle) ? null : jobTitle.isEmpty() ? null : jobTitle);
                person.setBirthday(getTime((String) json.opt(NotificationKeys.BIRTHDAY.getKey())));
                person.setAddress(buildAddress(json));
                person.setExternalPersonId(personId);
                person.setCreatedTime(getTime((String) json.opt(NotificationKeys.CREATED_DATE_TIME.getKey())));
                person.setModifiedTime(getTime((String) json.opt(NotificationKeys.LAST_MODIFIED_DATE_TIME.getKey())));

                try {
                    final ProfilePhotoRequestBuilder photo = buildRequest(emailAddress).contacts(personId).photo();
                    final ProfilePhoto profilePhoto = photo.buildRequest().get();

                    if (Objects.nonNull(profilePhoto)) {
                        final InputStream inputStream = photo.content().buildRequest().get();

                        if (Objects.nonNull(inputStream)) {
                            final ProfilePic profilePic = new ProfilePic();
                            final Integer height = profilePhoto.height;
                            final Integer width = profilePhoto.width;

                            profilePic.setImageAsBytes(inputStream.readAllBytes());
                            profilePic.setSize((Objects.isNull(height) ? DEFAULT_SIZE : height) * (Objects.isNull(width) ? DEFAULT_SIZE : width) * 3);
                            profilePic.setExtension(getExtension(String.valueOf(profilePhoto.additionalDataManager().get(NotificationKeys.MEDIA_CONTENT_TYPE.getKey()))));
                            person.setProfilePicture(profilePic);
                        }
                    }
                } catch (Exception e) {
                    final String message = e.getMessage();

                    if (message.contains("ErrorItemNotFound")) {
                        LOGGER.warn("No profile picture found for Person-Id: {}", personId);
                    } else {
                        LOGGER.error("Failed to retrieve DP for Person-Id: {}, Error: {}", personId, message);
                    }
                }

                personList.add(person);
            }
        }
    }

    /**
     * <p>
     * Fetches the extension based on the content type
     * </p>
     *
     * @param contentType Represents the content type
     * @return the {@link ProfilePicture.Extension}
     */
    private ProfilePicture.Extension getExtension(final String contentType) {

        if (NotificationKeys.JPG.getKey().contains(contentType)) {
            return ProfilePicture.Extension.JPG;
        } else if (NotificationKeys.PNG.getKey().contains(contentType)) {
            return ProfilePicture.Extension.PNG;
        } else {
            return ProfilePicture.Extension.JPEG;
        }
    }

    /**
     * <p>
     * Gets the request url to process the contacts
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return the request url
     */
    private String getRequestUrl(final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final ContactDeltaCollectionRequest contactDeltaCollectionRequest = buildRequest(emailAddress).contacts().delta().buildRequest();
        final String deltaToken = contactTokenService.getSyncToken(StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, String.valueOf(emailAddress.orgId()), emailAddress.getEmailId()));

        return Objects.isNull(deltaToken) ? contactDeltaCollectionRequest.getRequestUrl().toString() :
                contactDeltaCollectionRequest.deltaToken(deltaToken).getRequestUrl().toString();
    }

    /**
     * <p>
     * Builds the user request for performing functions in contacts
     * </p>
     *
     * @param emailAddress Represents the {@link com.microsoft.graph.models.EmailAddress}
     * @return {@link UserRequestBuilder}
     */
    private UserRequestBuilder buildRequest(final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getContactTokenReadRequest(new EmailAddress(emailAddress.getEmailId()), TokenRequest.TokenType.ACCESS);
        return GraphServiceClient.builder().authenticationProvider((token) -> CompletableFuture.completedFuture(
                contactSyncTokenService.getAccessToken(accessTokenReadRequest))).buildClient().me();
    }

    /**
     * <p>
     * Builds the contact for performing create and update action in provider
     * </p>
     *
     * @param person Represents the {@link Person}
     * @return the {@link Contact}
     */
    private Contact buildContact(final Person person) {
        final Contact contact = new Contact();
        final String firstName = person.getFirstName();
        final String lastName = person.getLastName();
        final Address address = person.getAddress();
        final Collection<String> phoneNumbers = person.getPhoneNumbers();
        final Collection<String> emailAddresses = person.getEmailAddresses();
        final Long birthday = person.getBirthday();

        contact.displayName = firstName;
        contact.givenName = firstName;
        contact.jobTitle = person.getJobTitle();
        contact.companyName = person.getCompany();

        if (Objects.nonNull(lastName)) {
            contact.surname = lastName;
        }

        if (Objects.nonNull(address)) {
            contact.homeAddress = convertAddress(address);
        }

        if (Objects.nonNull(phoneNumbers)) {
            contact.businessPhones = new ArrayList<>(phoneNumbers);
        }

        if (Objects.nonNull(emailAddresses)) {
            contact.emailAddresses = convertEmails(emailAddresses);
        }

        if (Objects.nonNull(birthday)) {
            contact.birthday = Instant.ofEpochMilli(birthday).atOffset(ZoneOffset.UTC);
        }

        return contact;
    }

    /**
     * <p>
     * Converts the emails for creating or updating
     * </p>
     *
     * @param emails Represents the emails
     * @return list of {@link com.microsoft.graph.models.EmailAddress}
     */
    private List<com.microsoft.graph.models.EmailAddress> convertEmails(final Collection<String> emails) {
        final List<com.microsoft.graph.models.EmailAddress> addresses = new ArrayList<>();

        for (final String email : emails) {
            final com.microsoft.graph.models.EmailAddress emailAddress = new com.microsoft.graph.models.EmailAddress();

            emailAddress.address = email;
            addresses.add(emailAddress);
        }

        return addresses;
    }

    /**
     * <p>
     * Converts the address for creating or updating
     * </p>
     *
     * @param address Represents the {@link Address}
     * @return the {@link PhysicalAddress}
     */
    private PhysicalAddress convertAddress(final Address address) {
        final PhysicalAddress physicalAddress = new PhysicalAddress();

        physicalAddress.city = address.getCity();
        physicalAddress.countryOrRegion = address.getCountry();
        physicalAddress.postalCode = address.getZipcode();
        physicalAddress.state = address.getState();
        physicalAddress.street = address.getAddressLine1();

        return physicalAddress;
    }

    /**
     * <p>
     * Gets the utc time in long
     * </p>
     *
     * @param dateTimeFormat Represents the date time format
     * @return the utc time
     */
    private Long getTime(final String dateTimeFormat) {
        try {
            return Objects.isNull(dateTimeFormat) ? null : DateUtils.parseDate(dateTimeFormat, DATE_FORMAT).getTime();
        } catch (ParseException e) {
            throw ErrorCode.get(NotificationErrorCodes.DATE_EXCEPTION);
        }
    }

    /**
     * <p>
     * Builds the address synced from provider
     * </p>
     *
     * @param json Represents the {@link JsonObject}
     * @return the {@link Address}
     */
    private Address buildAddress(final JsonObject json) {
        final JsonObject homeAddress = json.optJsonObject(NotificationKeys.HOME_ADDRESS.getKey());

        if (homeAddress.size() > 0) {
            final Address address = new Address();

            address.setAddressLine1(homeAddress.optString(NotificationKeys.STREET.getKey(), address.getAddressLine1()));
            address.setCity(homeAddress.optString(NotificationKeys.CITY.getKey(), address.getCity()));
            address.setState(homeAddress.optString(NotificationKeys.STATE.getKey(), address.getState()));
            address.setCountry(homeAddress.optString(NotificationKeys.COUNTY_OR_REGION.getKey(), address.getCountry()));
            address.setZipcode(homeAddress.optString(NotificationKeys.POSTAL_CODE.getKey(), address.getZipcode()));

            return address;
        }
        return null;
    }

    /**
     * <p>
     * Build the emails of the given contact
     * </p>
     *
     * @param json Represents the {@link JsonObject}
     * @return the emails in the contacts
     */
    private Collection<String> buildEmails(final JsonObject json) {
        final JsonArray jsonArray = json.optJsonArray(NotificationKeys.EMAIL_ADDRESSES.getKey());
        final Collection<String> emails = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);

            emails.add(jsonObject.getString(NotificationKeys.ADDRESS.getKey()));
        }
        return emails;
    }

    /**
     * <p>
     * Build the emails of the given contact
     * </p>
     *
     * @param json Represents the {@link JsonObject}
     * @return the phones in the contacts
     */
    private Collection<String> buildPhones(final JsonObject json) {
        final Collection<String> phones = new ArrayList<>();
        final String mobilePhone = json.optString(NotificationKeys.MOBILE_PHONE.getKey(), null);

        if (Objects.nonNull(mobilePhone) && !mobilePhone.isEmpty()) {
            phones.add(mobilePhone);
        }
        phones.addAll(fetchContactPhones(json.optJsonArray(NotificationKeys.BUSINESS_PHONES.getKey())));
        phones.addAll(fetchContactPhones(json.optJsonArray(NotificationKeys.HOME_PHONES.getKey())));

        return phones;
    }

    /**
     * <p>
     * Fetches the given contact phones in required format
     * </p>
     *
     * @param jsonArray Represents the {@link JsonArray}
     */
    private Collection<String> fetchContactPhones(final JsonArray jsonArray) {
        final Collection<String> contactInfo = new HashSet<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            contactInfo.add(jsonArray.getString(index));
        }
        return contactInfo;
    }

    /**
     * <p>
     * Gets the output for required request
     * </p>
     *
     * @param uri         Represents the uri
     * @param accessToken Represents the access token
     * @return {@link JsonArray}
     */
    private Map<String, JsonArray> getTokenWithData(final String uri, final String accessToken) {
        try {
            final Map<String, String> headers = new HashMap<>();
            final Map<String, JsonArray> message = new HashMap<>();

            headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
            headers.put(NotificationKeys.ACCEPT.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
            headers.put(NotificationKeys.CONTENT_TYPE.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(uri), Collections.emptyMap(), HttpMethod.GET, headers));
            final String nextLink = response.optString(NotificationKeys.NEXT_LINK.getKey(), null);

            message.put(Objects.isNull(nextLink) ? response.getString(NotificationKeys.DELTA_LINK.getKey()) : nextLink,
                    response.optJsonArray(NotificationKeys.VALUE.getKey()));
            return message;
        } catch (Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL);
        }
    }

    /**
     * <p>
     * Extracts the token from the given url
     * </p>
     *
     * @param url Represents the url
     * @return the delta token
     */
    private String extractToken(final String url) {
        try {
            final Map<String, String> queryParams = new HashMap<>();
            final String query = new URL(url).getQuery();

            if (Objects.nonNull(query)) {
                final String[] pairs = query.split("&");

                for (final String pair : pairs) {
                    final String[] keyValue = pair.split("=");

                    if (2 == keyValue.length) {
                        queryParams.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            final String deltaToken = queryParams.get(NotificationKeys.DELTA_TOKEN.getKey());

            return Objects.isNull(deltaToken) ? queryParams.get(NotificationKeys.SKIP_TOKEN.getKey()) : deltaToken;
        } catch (Exception e) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL);
        }
    }
}