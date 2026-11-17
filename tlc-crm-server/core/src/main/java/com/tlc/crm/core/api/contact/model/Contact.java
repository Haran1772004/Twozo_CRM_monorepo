package com.tlc.crm.core.api.contact.model;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.auto.assignment.AutoAssignment;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.core.validator.annotation.ContactValidator;
import com.tlc.crm.core.workflow.internal.annotation.Trigger;
import com.tlc.crm.core.zapier.ZapTrigger;
import com.tlc.crm.entity.dropdown.DefaultOption;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.entity.validator.annotation.FieldsValidator;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import com.tlc.validator.type.URL;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Abishek
 * @version 1.0
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.CONTACT_ASSIGNED, EmailNotificationTemplate.CONTACT_TRANSFERRED})
@Trigger(source = Source.CONTACT)
@Notifiable(types = AppNotificationEventType.CONTACT_ASSIGNED, modelName = CrmModelType.CONTACT)
@Searchable(index = SearchIndex.CONTACT)
@ContactValidator(groups = {Create.class, Update.class}, message = "i18n.contact.validation.failed")
@FieldsValidator(groups = {Create.class, Update.class}, message = "i18n.entity.fields.validation.failed")
@ZapTrigger(source = Source.CONTACT)
@AutoAssignment(source = Source.CONTACT)
public class Contact extends Entity {

    public static final String ENTITY_TYPE = "Contact";
    private final DisplayPictureManager displayPictureManager;

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @AuditEntityName
    @SearchField(name = "name")
    private String fullName;

    @AuditField(aliasName = "firstName", displayName = "i18n.contact.fields.firstName")
    @Field(aliasName = "firstName")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.contact.firstName.length.exceeded")
    private String firstName;

    @AuditField(aliasName = "lastName", displayName = "i18n.contact.fields.lastName")
    @Field(aliasName = "lastName")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.contact.lastName.length.exceeded")
    private String lastName;

    @EmailAlertField(
            fieldType = EmailAlertFieldType.COLLECTION
    )
    @AuditField(aliasName = "emails", displayName = "i18n.contact.fields.emails")
    @SearchField(name = "emails")
    @Field(aliasName = "emails")
    @Size(max = 10, groups = {Create.class, Update.class}, message = "i18n.contact.emails.limit.exceeded")
    private Collection<Email> emails;

    @AuditField(aliasName = "phones", displayName = "i18n.contact.fields.phones")
    @SearchField(name = "phones")
    @Field(aliasName = "phones")
    @Size(max = 10, groups = {Create.class, Update.class}, message = "i18n.contact.phones.limit.exceeded")
    private Collection<Phone> phones;

    @Field(aliasName = "contactSource")
    private Option contactSource;

    @AuditField(aliasName = "lifeCycleStage", displayName = "i18n.contact.fields.stage")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.contact.stage.invalid")
    @Field(aliasName = "lifeCycleStage", autoValidate = false)
    private LifeCycleStage lifeCycleStage;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.contact.status.invalid")
    @Field(aliasName = "lifeCycleStatus", autoValidate = false)
    private LifeCycleStatus lifeCycleStatus;

    @Field(aliasName = "contactLostReason", autoValidate = false)
    private Option lostReason;

    @AuditField(aliasName = "company", displayName = "i18n.contact.fields.company.placeholder")
    @SearchField(name = "company")
    @Field(aliasName = "company", autoValidate = false)
    private Company company;

    @SearchField(name = "ownerId")
    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.CONTACT_ASSIGNED, EmailNotificationTemplate.CONTACT_TRANSFERRED},
            placeholderTemplates = {EmailNotificationTemplate.CONTACT_ASSIGNED, EmailNotificationTemplate.CONTACT_TRANSFERRED},
            requirePreviousValue = true
    )
    private Long ownerId;

    private SourceDisplayPicture displayPicture;

    @SearchField(name = "primaryEmail")
    private Email primaryEmail;

    @SearchField(name = "primaryPhone")
    private Phone primaryPhone;

    private boolean notifiable;

    public Email getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(final Email primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public Phone getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(final Phone primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    private Long activityCount;

    private Long dealCount;

    private boolean isCreatedViaCall;

    @Field(aliasName = "designation")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.contact.designation.length.exceeded")
    private String designation;

    @Field(aliasName = "department")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.contact.department.length.exceeded")
    private String department;

    @URL(groups = {Create.class, Update.class})
    @SearchField(name = "facebook")
    @Field(aliasName = "facebook")
    private String facebook;

    @URL(groups = {Create.class, Update.class})
    @Field(aliasName = "twitter")
    private String twitter;

    @URL(groups = {Create.class, Update.class})
    @SearchField(name = "linkedin")
    @Field(aliasName = "linkedIn")
    private String linkedIn;

    @Field(aliasName = "dateOfBirth", autoValidate = false)
    private Date dateOfBirth;

    @SearchField(name = "address")
    @Field(aliasName = "address")
    private Address address;

    @Field(aliasName = "subscriptionTypes", autoValidate = false)
    private Collection<Option> subscriptionTypes;

    @Field(aliasName = "subscriptionStatus")
    private Option subscriptionStatus;

    @Field(aliasName = "timeZone")
    private Option timeZone;

    @Valid
    private transient SummaryInfo summaryInfo;

    @Field(aliasName = "unsubscribeReason", autoValidate = false)
    private Option unsubscribeReason;

    @Field(aliasName = "otherUnsubscribeReason", autoValidate = false)
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.contact.other.unsubscribeReason.length.exceeded")
    private String otherUnsubscribeReason;

    @SearchField(name = "territory")
    private Territory territory;
    private String externalPersonId;
    private OriginSource originSource;
    private DisplayPicture thumbnail;
    private DisplayPicture originalPicture;
    private String recentNote;
    private Collection<MarketingList> marketingLists;

    public Contact() {
        super(ENTITY_TYPE);
        displayPictureManager = DisplayPictureManager.getInstance();
        originSource = OriginSource.TWOZO;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    private void updateFullName() {
        fullName = (Objects.isNull(firstName) || firstName.isBlank()) ? lastName
            : (Objects.isNull(lastName) || lastName.isBlank()) ? firstName : firstName + " " + lastName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setSalesOwner(final CrmUser salesOwner) {
        this.ownerId = salesOwner.id();
        super.setSalesOwner(salesOwner);
    }

    public void setTerritory(final Territory territory) {
        this.territory = territory;
    }

    public Territory getTerritory() {
        return territory;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public Collection<Email> getEmails() {
        return emails;
    }

    public void setEmails(final Collection<Email> emails) {
        this.emails = emails;
    }

    public void addEmail(final Email email) {
        this.emails = this.emails == null ? new ArrayList<>() : this.emails;

        if (!this.emails.contains(email)) {
            this.emails.add(email);
        }
    }

    public Collection<Phone> getPhones() {
        return phones;
    }

    public void addPhone(final Phone phone) {
        this.phones = this.phones == null ? new ArrayList<>() : this.phones;

        if (!this.phones.contains(phone)) {
            this.phones.add(phone);
        }
    }

    public void setPhones(final Collection<Phone> phones) {
        this.phones = phones;
    }

    public Option getContactSource() {
        return contactSource;
    }

    public void setContactSource(final Option contactSource) {
        this.contactSource = contactSource;
    }

    public LifeCycleStage getLifeCycleStage() {
        return lifeCycleStage;
    }

    public void setLifeCycleStage(final LifeCycleStage lifeCycleStage) {
        this.lifeCycleStage = lifeCycleStage;
    }

    public LifeCycleStatus getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(final LifeCycleStatus lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    public Option getLostReason() {
        return lostReason;
    }

    public void setLostReason(final Option lostReason) {
        this.lostReason = lostReason;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(final Company company) {
        this.company = company;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(final String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(final String department) {
        this.department = department;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(final String facebook) {
        this.facebook = facebook;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(final String twitter) {
        this.twitter = twitter;
    }

    public String getLinkedIn() {
        return linkedIn;
    }

    public void setLinkedIn(final String linkedIn) {
        this.linkedIn = linkedIn;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public Collection<Option> getSubscriptionTypes() {
        return subscriptionTypes;
    }

    public void setSubscriptionTypes(final Collection<Option> subscriptionTypes) {
        this.subscriptionTypes = subscriptionTypes;
    }

    public void addSubscriptionType(final Option subscriptionType) {
        this.subscriptionTypes = this.subscriptionTypes == null ? new ArrayList<>() : this.subscriptionTypes;

        if (!this.subscriptionTypes.contains(subscriptionType)) {
            this.subscriptionTypes.add(subscriptionType);
        }
    }

    public Option getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(final Option subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Long getActivityCount() {
        return activityCount;
    }

    public Long getDealCount() {
        return dealCount;
    }

    public void setDealCount(final Long dealCount) {
        this.dealCount = dealCount;
    }

    public void setActivityCount(final Long activityCount) {
        this.activityCount = activityCount;
    }

    public Option getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(final Option timeZone) {
        this.timeZone = timeZone;
    }

    public SummaryInfo getSummaryInfo() {
        return summaryInfo;
    }

    public void setSummaryInfo(final SummaryInfo summaryInfo) {
        this.summaryInfo = summaryInfo;
    }

    public String getExternalPersonId() {
        return externalPersonId;
    }

    public void setExternalPersonId(final String externalPersonId) {
        this.externalPersonId = externalPersonId;
    }

    public String getRecentNote() {
        return recentNote;
    }

    public void setRecentNote(final String recentNote) {
        this.recentNote = recentNote;
    }

    public DisplayPicture getThumbnail() {
        final Long orgId = orgId();

        if (Objects.isNull(thumbnail) && Objects.nonNull(orgId) && Objects.nonNull(id)) {
            final Optional<DisplayPicture> displayPicture = displayPictureManager.getDisplayPicture(orgId, id,
                    Source.CONTACT, ProfilePicture.Quality.THUMBNAIL);

            this.thumbnail = displayPicture.orElse(null);
        }

        return thumbnail;
    }

    public void setOriginalPicture(final DisplayPicture originalPicture) {
        this.originalPicture = originalPicture;
    }

    public DisplayPicture getOriginalPicture() {
        return originalPicture;
    }

    public SourceDisplayPicture getDisplayPicture() {
        return displayPicture;
    }

    public void setDisplayPicture(final SourceDisplayPicture profilePicture) {
        this.displayPicture = profilePicture;
    }

    @Override
    public boolean notifiable() {
        return notifiable;
    }

    @Override
    public void setNotifiable(final boolean notifiable) {
        this.notifiable = notifiable;
    }

    /**
     * Gets the full name of contact from the given firstName and lastName.
     *
     * @param firstName Represents the first name of contact
     * @param lastName  Represents the last name of contact
     * @return the full name
     */
    public static String getFullName(final String firstName, final String lastName) {
        return (Objects.isNull(firstName) || firstName.isBlank()) ? lastName
                : ((Objects.isNull(lastName) || lastName.isBlank()) ? firstName : String.format("%s %s",firstName, lastName));
    }

    public OriginSource getOriginSource() {
        return originSource;
    }

    public void setOriginSource(final OriginSource originSource) {
        this.originSource = originSource;
    }

    public Collection<MarketingList> getMarketingLists() {
        return marketingLists;
    }

    public void setMarketingLists(final Collection<MarketingList> marketingLists) {
        this.marketingLists = marketingLists;
    }

    /**
     * <p>
     * Checks whether the contact has emails or not
     * </p>
     *
     * @return true if it has emails, false otherwise
     */
    public boolean hasEmails() {
        return Objects.nonNull(emails) && !emails.isEmpty();
    }

    /**
     * <p>
     * Checks whether the contact has phones or not
     * </p>
     *
     * @return true if it has phones, false otherwise
     */
    public boolean hasPhones() {
        return Objects.nonNull(phones) && !phones.isEmpty();
    }

    public boolean isCreatedViaCall() {
        return isCreatedViaCall;
    }

    public void setCreatedViaCall(final boolean createdViaCall) {
        isCreatedViaCall = createdViaCall;
    }

    /**
     * <p>
     * Represents the subscription status.
     * </p>
     */
    public enum SubscriptionStatus implements DefaultOption {

        SUBSCRIBED("i18n.contact.subscriptionStatus.option.subscribed", true),
        UNSUBSCRIBED("i18n.contact.subscriptionStatus.option.unsubscribed"),
        NOT_SUBSCRIBED("i18n.contact.subscriptionStatus.option.notSubscribed"),
        REPORTED_AS_SPAM("i18n.contact.subscriptionStatus.option.reportedASpam"),
        BOUNCED("i18n.contact.subscriptionStatus.option.bounced");

        private final String name;
        private final boolean isDefault;

        SubscriptionStatus(final String name) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = false;
        }

        SubscriptionStatus(final String name, final boolean isDefault) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = isDefault;
        }

        /**
         * {@inheritDoc}
         *
         * @return The option name
         */
        @Override
        public String getName() {
            return this.name;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is editable, otherwise it will return false
         */
        @Override
        public boolean isEditable() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is default, otherwise it will return false
         */
        public boolean isDefault() {
            return isDefault;
        }

        /**
         * <p>
         * Retrieves a collection of default options represented by the {@link SubscriptionStatus} enum values.
         * </p>
         *
         * @return The collection of {@link DefaultOption}
         */
        public static Collection<DefaultOption> getOptions() {
            return Arrays.asList(SubscriptionStatus.values());
        }
    }

    /**
     * <p>
     * Represents the subscription type.
     * </p>
     */
    public enum SubscriptionTypes implements DefaultOption {

        NEWS_LETTER("i18n.contact.subscriptionType.option.newsletter", true),
        PROMOTIONAL("i18n.contact.subscriptionType.option.promotional", true),
        PRODUCT_UPDATES("i18n.contact.subscriptionType.option.productUpdates", true),
        CONFERENCE_AND_EVENTS("i18n.contact.subscriptionType.option.conferenceAndEvents", true),
        NON_MARKETING_EMAILS_FROM_OUR_COMPANY("i18n.contact.subscriptionType.option.nonMarketingEmailsFromOurCompany", true),
        ;

        private final String name;
        private final boolean isDefault;

        SubscriptionTypes(final String name) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = false;
        }

        SubscriptionTypes(final String name, final boolean isDefault) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = isDefault;
        }

        /**
         * {@inheritDoc}
         *
         * @return The option name
         */
        @Override
        public String getName() {
            return this.name;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is editable, otherwise it will return false
         */
        @Override
        public boolean isEditable() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is default, otherwise it will return false
         */
        public boolean isDefault() {
            return isDefault;
        }

        /**
         * <p>
         * Retrieves a collection of default options represented by the {@link SubscriptionTypes} enum values.
         * </p>
         *
         * @return The collection of {@link DefaultOption}
         */
        public static Collection<DefaultOption> getOptions() {
            return Arrays.asList(SubscriptionTypes.values());
        }
    }

    /**
     * <p>
     * An enum contains the various the {@link SmsSubscriptionStatus} data
     * </p>
     */
    public enum SmsSubscriptionStatus {

        UNSUBSCRIBED("Unsubscribed", 1),
        SUBSCRIBED("Subscribed", 2),
        NOT_SUBSCRIBED("Not subscribed", 3);

        private final String name;
        private final int id;

        SmsSubscriptionStatus(final String name, final int id) {
            this.name = name;
            this.id = id;
        }

        /**
         * <p>
         * Gets the name of the sms subscription status
         * </p>
         *
         * @return the name of sms subscription status
         */
        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Gets the sms subscription status id
         * </p>
         *
         * @return The sms subscription status id
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the {@link SmsSubscriptionStatus}
         * </p>
         *
         * @param id Represents the sms subscription status id
         * @return {@link SmsSubscriptionStatus}
         */
        public SmsSubscriptionStatus valueOf(final int id) {

            return switch (id) {
                case 1 -> UNSUBSCRIBED;
                case 2 -> SUBSCRIBED;
                case 3 -> NOT_SUBSCRIBED;
                default -> throw ErrorCode.get(ContactErrorCodes.INVALID_SMS_SUBSCRIPTION_STATUS);
            };
        }
    }

    /**
     * An enum contains the various the {@link WhatsAppSubscriptionStatus} data
     */
    public enum WhatsAppSubscriptionStatus {

        UNSUBSCRIBED("Unsubscribed", 1),
        SUBSCRIBED("Subscribed", 2),
        NOT_SUBSCRIBED("Not subscribed", 3);

        private final String name;
        private final int id;

        WhatsAppSubscriptionStatus(final String name, final int id) {
            this.name = name;
            this.id = id;
        }

        /**
         * <p>
         * Gets the name of the whatsApp subscription status
         * </p>
         *
         * @return The name of whatsApp subscription status
         */
        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Gets the whatsApp subscription status id
         * </p>
         *
         * @return The whatsApp subscription status id
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the {@link WhatsAppSubscriptionStatus}
         * </p>
         *
         * @param id Represents the whatsApp subscription status id
         * @return The {@link WhatsAppSubscriptionStatus}
         */
        public WhatsAppSubscriptionStatus valueOf(final int id) {

            return switch (id) {
                case 1 -> UNSUBSCRIBED;
                case 2 -> SUBSCRIBED;
                case 3 -> NOT_SUBSCRIBED;
                default -> throw ErrorCode.get(ContactErrorCodes.INVALID_WHATSAPP_SUBSCRIPTION_STATUS);
            };
        }
    }

    public String getOtherUnsubscribeReason() {
        return otherUnsubscribeReason;
    }

    public void setOtherUnsubscribeReason(final String otherUnsubscribeReason) {
        this.otherUnsubscribeReason = otherUnsubscribeReason;
    }

    public Option getUnsubscribeReason() {
        return unsubscribeReason;
    }

    public void setUnsubscribeReason(final Option unsubscribeReason) {
        this.unsubscribeReason = unsubscribeReason;
    }

    /**
     * <p>
     * An enum contains the various {@link UnsubscribeReason} options
     * </p>
     */
    public enum UnsubscribeReason implements DefaultOption {

        I_NO_LONGER_WANT_TO_RECEIVE_EMAILS_FROM_YOU("i18n.contact.unsubscribeReason.option.noLongerWantToReceiveEmailsFromYou"),
        I_RECEIVE_TOO_MANY_EMAILS_FROM_YOU("i18n.contact.unsubscribeReason.option.receiveTooManyEmailsFromYou"),
        THE_EMAILS_ARE_INAPPROPRIATE("i18n.contact.unsubscribeReason.option.theEmailsAreInappropriate"),
        THE_EMAILS_ARE_SPAM("i18n.contact.unsubscribeReason.option.theEmailsAreSpam"),
        OTHER_UNSUBSCRIBE_REASON("i18n.contact.unsubscribeReason.option.others"),
        ;

        private static final Collection<DefaultOption> UNSUBSCRIBE_REASONS = Arrays.asList(UnsubscribeReason.values());
        private final String name;

        UnsubscribeReason(final String name) {
            this.name = Objects.requireNonNull(name);
        }

        /**
         * {@inheritDoc}
         *
         * @return The option name
         */
        @Override
        public String getName() {
            return this.name;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is editable, otherwise it will return false
         */
        @Override
        public boolean isEditable() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is default, otherwise it will return false
         */
        public boolean isDefault() {
            return false;
        }

        /**
         * <p>
         * Retrieves a collection of default options represented by the {@link UnsubscribeReason} enum values.
         * </p>
         *
         * @return The collection of {@link DefaultOption}
         */
        public static Collection<DefaultOption> getOptions() {
            return UNSUBSCRIBE_REASONS;
        }
    }

    /**
     * <p>
     * Represents the origin source.
     * </p>
     */
    public enum OriginSource {

        TWOZO(1),
        PROVIDER(2);

        private final int id;

        OriginSource(final int id) {
            this.id = id;
        }

        /**
         * <p>
         * Gets the if of the source type
         * </p>
         *
         * @return the id of source type
         */
        public int getId() {
            return this.id;
        }

        /**
         * Gets the {@link OriginSource} by id
         *
         * @param sourceTypeId represents the id of the source type.
         * @return {@link OriginSource}
         */
        public static OriginSource valueOf(final int sourceTypeId) {

            return switch (sourceTypeId) {
                case 1 -> TWOZO;
                case 2 -> PROVIDER;
                default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE_TYPE_ID);
            };
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The name of the contact
     */
    @Override
    public String getName() {
        return fullName;
    }
}
