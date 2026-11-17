package com.tlc.crm.core.features.api.activity.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.map.util.MapUrlProvider;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.validator.annotation.ActivityHolderValidator;
import com.tlc.crm.core.workflow.internal.annotation.Trigger;
import com.tlc.crm.core.zapier.ZapTrigger;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.CheckInOutRecord;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;

import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.notification.model.alert.EmailNotifiableModel;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A Model that represents the {@link ActivityHolder} which holds the activity details
 *
 * @author Kowsalya SP
 * @version 1.0
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.ACTIVITY_ASSIGNED, EmailNotificationTemplate.ACTIVITY_CREATED,
        EmailNotificationTemplate.ACTIVITY_MODIFIED, EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION, EmailNotificationTemplate.ACTIVITY_COMPLETED})
@Trigger(source = Source.ACTIVITY)
@Notifiable(types = {AppNotificationEventType.NEW_ACTIVITY, AppNotificationEventType.ACTIVITY_REMINDER, AppNotificationEventType.COLLABORATOR_ADDED, AppNotificationEventType.ACTIVITY_PRIVATE_NOTE_MENTIONED}, modelName = CrmModelType.ACTIVITY, needHandling = true)
@Searchable(index = SearchIndex.ACTIVITY)
@ActivityHolderValidator(groups = {Group.Create.class}, message = "i18n.activity.validation.failed")
@ZapTrigger(source = Source.ACTIVITY)
public class ActivityHolder implements TlcModel, CrmModel, EmailNotifiableModel {


    @SearchField(name = "activity")
    @Valid
    @EmailAlertField(
            fieldType = EmailAlertFieldType.TLC_MODEL
    )
    private Activity activity;
    @SearchField(name = "contact")
    private List<Contact> contacts;
    @SearchField(name = "deal")
    private Deal deal;
    @SearchField(name = "company")
    private Company company;
    private Collection<Long> userIds;
    @Valid
    @EmailAlertField(
            name = "activityGuests",
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION},
            recipientTemplates = {EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION},
            requirePreviousValue = true,
            fieldType = EmailAlertFieldType.COLLECTION
    )
    private Collection<ActivityGuest> activityGuests;

    public List<Contact> getContacts() {
        return Objects.isNull(contacts) ? new LinkedList<>() : contacts;
    }

    public void setContacts(final List<Contact> contacts) {
        this.contacts = Objects.nonNull(contacts) ? new LinkedList<>(contacts) : null;
    }

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(final Deal deal) {
        this.deal = deal;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(final Company company) {
        this.company = company;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(final Activity activity) {
        this.activity = activity;
    }

    public void addContact(final Contact contact) {
        this.contacts = this.contacts == null ? new LinkedList<>() : this.contacts;

        this.contacts.add(contact);
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    public Collection<ActivityGuest> getActivityGuests() {
        return activityGuests;
    }

    public void setActivityGuests(final Collection<ActivityGuest> activityGuests) {
        this.activityGuests = activityGuests;
    }

    public void addActivityGuests(final ActivityGuest activityGuest) {
        this.activityGuests = this.activityGuests == null ? new LinkedList<>() : this.activityGuests;

        this.activityGuests.add(activityGuest);
    }

    /**
     * <p>
     * Retrieves the checked-in time of the activity. If the activity is null, it returns null. Otherwise, it returns the checked-in time from the activity.
     * </p>
     *
     * @return The checked-in time of the activity, or null if the activity is null.
     */
    public DateTime getCheckedInTime() {
        return Objects.isNull(activity) ? null : activity.getCheckedInTime();
    }

    /**
     * <p>
     * Retrieves the checked-out time of the activity. If the activity is null, it returns null. If the checked-out time is available, it returns that value.
     * Otherwise, it attempts to calculate the checked-out time using the checked-in time and total time spent on the activity.
     * </p>
     *
     * @return The checked-out time of the activity, or null if it cannot be determined.
     */
    public DateTime getCheckedOutTime() {
        return Objects.isNull(activity) ? null : activity.getCheckedOutTime();
    }

    /**
     * <p>
     * Retrieves the checked-in location of the activity. If the activity is null, it returns null. If the checked-in location is available, it returns that value.
     * Otherwise, it attempts to extract the location text from the check-in record's data JSON using the getLocationText method.
     * </p>
     *
     * @return The checked-in location of the activity, or null if it cannot be determined.
     */
    public String getCheckedInLocation() {
        if (Objects.isNull(activity)) {
            return null;
        }

        final String checkedInLocation = activity.getCheckedInLocation();

        return Objects.nonNull(checkedInLocation) ? checkedInLocation :
                getLocationString(activity.getCheckInRecord());
    }

    /**
     * <p>
     * Retrieves the checked-out location of the activity. If the activity is null, it returns null. If the checked-out location is available, it returns that value.
     * Otherwise, it attempts to extract the location text from the check-out record's data JSON using the getLocationText method.
     * </p>
     *
     * @return The checked-out location of the activity, or null if it cannot be determined.
     */
    public String getCheckedOutLocation() {
        if (Objects.isNull(activity)) {
            return null;
        }

        final String checkedOutLocation = activity.getCheckedOutLocation();

        return Objects.nonNull(checkedOutLocation) ? checkedOutLocation :
                getLocationString(activity.getCheckOutRecord());
    }

    /**
     * <p>
     * Retrieves the URL for the checked-in location of the activity, if available. It extracts the latitude and longitude
     * from the check-in record's data JSON and constructs a map URL using the MapUrlProvider. If the activity or check-in record
     * is null, or if the necessary location data is not present, it returns null.
     * </p>
     *
     * @return The URL for the checked-in location, or null if it cannot be determined.
     */
    public String getCheckedInLocationUrl() {
        return getLocationUrl(Objects.isNull(activity) ? null : activity.getCheckInRecord());
    }

    /**
     * <p>
     * Retrieves the URL for the checked-out location of the activity, if available. It extracts the latitude and longitude
     * from the check-out record's data JSON and constructs a map URL using the MapUrlProvider. If the activity or check-out record
     * is null, or if the necessary location data is not present, it returns null.
     * </p>
     *
     * @return The URL for the checked-out location, or null if it cannot be determined.
     */
    public String getCheckedOutLocationUrl() {
        return getLocationUrl(Objects.isNull(activity) ? null : activity.getCheckOutRecord());
    }

    /**
     * <p>
     * Retrieves the total time spent on the activity in a formatted string.
     * It returns a human-readable string representing the total time spent, such as "1 day 2 hours 30 minutes".
     * </p>
     *
     * @return A formatted string representing the total time spent on the activity, or null if it cannot be determined.
     */
    public String getFormattedTotalTimeSpent() {
        if (Objects.isNull(activity)) {
            return null;
        }
        long totalTimeInMillis = activity.getTotalTimeSpent();

        if (totalTimeInMillis <= NumericalConstant.ZERO) {
            final DateTime checkedInTime = activity.getCheckedInTime();
            final DateTime checkedOutTime = activity.getCheckedOutTime();

            if (Objects.nonNull(checkedInTime) && Objects.nonNull(checkedOutTime)) {
                totalTimeInMillis = checkedOutTime.getTimeStamp() - checkedInTime.getTimeStamp();
            }
        }

        return TemporalServiceProvider.get().toHumanReadableFormat(totalTimeInMillis);
    }

    /**
     * <p>
     * Retrieves the location URL from the given check-in/out record.
     * </p>
     *
     * @param checkInOutRecord The check-in/out record containing the location data
     * @return The location URL if available, or null if not found
     */
    private String getLocationUrl(final CheckInOutRecord checkInOutRecord) {
        if (Objects.isNull(checkInOutRecord)) {
            return null;
        }
        final JsonObject dataJson = checkInOutRecord.getDataJson();
        final String latitudeKey = ActivityFields.LATITUDE.getName();
        final String longitudeKey = ActivityFields.LONGITUDE.getName();

        if (Objects.isNull(dataJson) || !dataJson.containsKey(latitudeKey)
                || !dataJson.containsKey(longitudeKey)) {
            return null;
        }

        return MapUrlProvider.fetchUrl(
                dataJson.getString(latitudeKey), dataJson.getString(longitudeKey)
        );
    }

    /**
     * <p>
     * Retrieves the location text from the given CheckInOutRecord's dataJson, if available.
     * </p>
     *
     * @param checkInOutRecord The CheckInOutRecord containing the dataJson with potential location information.
     * @return The location string if present in the dataJson; otherwise, null.
     */
    private String getLocationString(final CheckInOutRecord checkInOutRecord) {
        if (Objects.isNull(checkInOutRecord)) {
            return null;
        }
        final JsonObject dataJson = checkInOutRecord.getDataJson();
        final String locationKey = ActivityFields.LOCATION.getName();

        if (Objects.isNull(dataJson) || !dataJson.containsKey(locationKey)) {
            return null;
        }

        return dataJson.getString(locationKey);
    }

    @Override
    public Long orgId() {
        return activity.orgId();
    }

    @Override
    public Long id() {
        return activity.id();
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public Collection<Long> userIds() {
        return userIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDateTimeField() {
        return true;
    }
}