package com.tlc.crm.core.features.api.activity.model.converter.calendar;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityGuest;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.notification.model.contact.Address;
import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.model.email.EmailAddress;

import java.time.Instant;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * Facilitates the conversion of {@link CalendarEvent} to {@link ActivityHolder} representations and vice versa.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class CalendarEventMapper {

    private static final String DELIMITER = "-----------------------------------------------";

    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final ContactManager contactManager;

    private CalendarEventMapper() {
        this.companyManager = CompanyManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final CalendarEventMapper INSTANCE = new CalendarEventMapper();
    }

    /**
     * <p>
     * Gets the {@link CalendarEventMapper} Instance
     * </p>
     *
     * @return {@link CalendarEventMapper}
     */
    public static CalendarEventMapper getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the collection of {@link CalendarEvent} into {@link ActivityHolder}.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param calendarEvents Represents the collection of {@link CalendarEvent}
     * @return collection of {@link Activity}
     */
    public Collection<ActivityHolder> convertEventsToActivityHolders(final long orgId, final long userId, final Long calendarEventId,
                                                                     final Collection<CalendarEvent> calendarEvents) {
        final Collection<ActivityHolder> activityHolders = new HashSet<>();

        for (final CalendarEvent calendarEvent : calendarEvents) {
            final ActivityHolder activityHolder = new ActivityHolder();
            final Activity activity = new Activity();
            final ActivityType activityType = new ActivityType();
            final DateTime startTime = calendarEvent.getStartTime();
            final DateTime endTime = calendarEvent.getEndTime();
            final DateTime currentTime = new DateTime(Instant.now().toEpochMilli());
            final Address address = calendarEvent.getAddress();
            final Collection<EmailAddress> emailAddresses = calendarEvent.getEmailAddresses();
            final String description = calendarEvent.getDescription();

            activityType.setId(calendarEventId);
            activityType.setOrgId(orgId);
            activity.setTitle(calendarEvent.getSummary());

            if (Objects.nonNull(description)) {
                final String[] split = description.split(DELIMITER, 2);

                activity.setDescription(split.length > 1 ? split[1].trim() : description);
            }
            activity.setStartTime(startTime);
            activity.setEndTime(endTime);
            activity.setEventId(calendarEvent.getEventId());
            activity.setReferenceId(calendarEvent.getReferenceId());
            activity.setActiveEvent(calendarEvent.isDeleteEvent());
            activity.setDuration(Objects.isNull(endTime) && Objects.isNull(startTime) ? 0 : (int) TimeUnit.MILLISECONDS.toMinutes(endTime.getTimeStamp() - startTime.getTimeStamp()));
            activity.setUpdatedTime(currentTime);
            activity.setCreatedTime(currentTime);

            if (Objects.nonNull(endTime)) {
                activity.setStatus(currentTime.getTimeStamp() < endTime.getTimeStamp() ? Status.UPCOMING : Status.OVERDUE);
            }
            activity.setDone(false);
            activity.setType(activityType);
            activity.setModifiedById(userId);
            activity.setCreatorId(userId);
            activity.setAssigneeId(userId);
            activity.setPriority(Priority.MEDIUM);
            activity.setAvailability(Availability.FREE);
            activity.setReminder(Reminder.NO_REMINDER);

            if (Objects.nonNull(address)) {
                activity.setLocation(address.getFullAddress());
            }

            if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {

                for (final EmailAddress emailAddress : emailAddresses) {
                    final ActivityGuest activityGuest = new ActivityGuest();
                    final Email email = new Email();

                    email.setValue(emailAddress.getEmailId());
                    email.setType(Email.Type.WORK);
                    email.setOrgId(orgId);
                    activityGuest.setEmail(email);
                    activityHolder.addActivityGuests(activityGuest);
                }
            }
            activityHolder.setActivity(activity);
            activityHolders.add(activityHolder);
        }

        return activityHolders;
    }

    /**
     * <p>
     * Converts the {@link ActivityHolder} to {@link CalendarEvent}
     * </p>
     *
     * @param activityHolder     Represents the entity of {@link ActivityHolder}
     * @param isLinkedWithSource A flag that represents whether the provider needs the source details along with the description
     * @return {@link CalendarEvent}
     */
    public CalendarEvent convertActivityHolderToEvent(final ActivityHolder activityHolder, final boolean isLinkedWithSource) {
        final CalendarEvent calendarEvent = new CalendarEvent();
        final Activity activity = activityHolder.getActivity();
        final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();
        final Address address = new Address();
        final String activityDescription = activity.getDescription();

        calendarEvent.setSummary(activity.getTitle());
        calendarEvent.setStartTime(activity.getStartTime());
        calendarEvent.setEndTime(activity.getEndTime());

        address.setFullAddress(activity.getLocation());
        calendarEvent.setAddress(address);

        if (Objects.nonNull(activityGuests) && !activityGuests.isEmpty()) {
            calendarEvent.setEmailAddresses(
                    activityGuests.stream()
                            .map(ActivityGuest::getEmail)
                            .filter(Objects::nonNull)
                            .map(email -> new EmailAddress(email.getValue()))
                            .collect(Collectors.toUnmodifiableSet())
            );
        }

        if (isLinkedWithSource) {
            final Long orgId = activityHolder.orgId();
            final StringBuilder description = new StringBuilder();
            final Deal activityDeal = activityHolder.getDeal();
            final Company activityCompany = activityHolder.getCompany();
            final Company company = Objects.isNull(activityCompany) ? null : companyManager.get(orgId, activityCompany.id());
            final String companyName = Objects.isNull(company) ? null : company.getName();
            final Deal deal = Objects.isNull(activityDeal) ? null : dealManager.get(orgId, activityDeal.id());
            final String dealTitle = Objects.isNull(deal) ? null : deal.getTitle();
            final Collection<Contact> activityContacts = activityHolder.getContacts();
            final Collection<Long> contactIds = activityContacts.isEmpty() ? null :
                    activityContacts.stream().map(Contact::id).collect(Collectors.toSet());
            final Collection<Contact> contacts = Objects.isNull(contactIds) ? null : contactManager.get(orgId, contactIds);

            if (Objects.nonNull(contacts)) {
                description.append("Contact : ").append("\n").append(contacts.stream().filter(Objects::nonNull).map(contact -> {
                    final StringBuilder builder = new StringBuilder(contact.getFullName());
                    final Collection<Phone> phones = contact.getPhones();

                    if (Objects.nonNull(phones) && !phones.isEmpty()) {
                        builder.append(", ").append("phoneNumber : ").append(phones.stream().map(Phone::getNumber)
                                .filter(Objects::nonNull).collect(Collectors.joining(", ")));
                    }

                    return builder.toString();
                }).collect(Collectors.joining("\n")));
            }

            if (Objects.nonNull(companyName)) {
                description.append("\n").append("Company : ").append(companyName);
            }

            if (Objects.nonNull(dealTitle)) {
                description.append("\n").append("Deal : ").append(dealTitle);
            }

            if (!description.isEmpty() && Objects.nonNull(activityDescription)) {
                description.append("\n").append(DELIMITER).append("\n");
                description.append(activityDescription);
            }

            calendarEvent.setDescription(description.toString());
        } else {
            calendarEvent.setDescription(activityDescription);
        }

        return calendarEvent;
    }
}
