package com.tlc.notification.model.calendar;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.contact.Address;

import com.tlc.notification.model.email.EmailAddress;
import com.tlc.validator.TlcModel;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;

/**
 * <p>
 * A model that represents the calendar events.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class CalendarEvent implements TlcModel {

    @NotNull(message = "i18n.notification.calendar.event.invalid.id")
    private Long id;

    @NotNull(message = "i18n.notification.calendar.event.invalid.orgId")
    private Long orgId;

    private String summary;
    private String description;

    @NotNull(message = "i18n.notification.calendar.event.invalid.eventId")
    private String eventId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.calendar.event.invalid.startTime")
    private DateTime startTime;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.calendar.event.invalid.endTime")
    private DateTime endTime;
    private DateTime lastModifiedDateTime;
    private String referenceId;
    private Collection<EmailAddress> emailAddresses;
    private Address address;
    private boolean isDeleteEvent;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final DateTime endTime) {
        this.endTime = endTime;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    public Collection<EmailAddress> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(final Collection<EmailAddress> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public DateTime getLastModifiedDateTime() {
        return lastModifiedDateTime;
    }

    public void setLastModifiedDateTime(final DateTime lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public boolean isDeleteEvent() {
        return isDeleteEvent;
    }

    public void setDeleteEvent(final boolean isDeleteEvent) {
        this.isDeleteEvent = isDeleteEvent;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}