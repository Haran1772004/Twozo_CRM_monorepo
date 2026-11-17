package com.tlc.crm.notifications.api.call.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents the details of a call, including participants, contact details,
 * call status, type, recordings, and notes.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallDetails implements CrmModel, ConversableModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.notification.call.participantUser.invalid")
    private CrmUser participantUser;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notification.call.contactNumber.invalid")
    private Phone contactNumber;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notification.call.serviceId.invalid")
    private String externalId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notification.call.userNumber.invalid")
    private PhoneNumber userNumber;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notification.call.type.invalid")
    private CallType type;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notification.call.status.invalid")
    private CallStatus status;

    private Long callLogId;
    private DateTime loggedTime;
    private CallSummary summary;
    private Contact contact;
    private Deal deal;
    private Collection<CallRecord> callRecordings;
    private CallForwardDetails forwardDetails;
    private Collection<Note> notes;
    private boolean isParentCall;
    private boolean isStoredAsCallLog;
    private CallLog callLog;

    private final Collection<Long> userIds;

    public CallDetails() {
        userIds = new HashSet<>();
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public CrmUser getParticipantUser() {
        return participantUser;
    }

    public void setParticipantUser(final CrmUser participantUser) {
        if (Objects.isNull(participantUser)) {
            return;
        }

        this.participantUser = participantUser;
        userIds.add(participantUser.getIamId());
    }

    public Phone getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(final Phone contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public DateTime getLoggedTime() {
        return loggedTime;
    }

    public void setLoggedTime(final DateTime loggedTime) {
        this.loggedTime = loggedTime;
    }

    public PhoneNumber getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(final PhoneNumber userNumber) {
        this.userNumber = userNumber;
    }

    public CallType getType() {
        return type;
    }

    public void setType(final CallType type) {
        this.type = type;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(final CallStatus status) {
        this.status = status;
    }

    public CallSummary getSummary() {
        return summary;
    }

    public void setSummary(final CallSummary summary) {
        this.summary = summary;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(final Deal deal) {
        this.deal = deal;
    }

    public Collection<CallRecord> getCallRecordings() {
        return callRecordings;
    }

    public void setCallRecordings(final Collection<CallRecord> callRecordings) {
        this.callRecordings = callRecordings;
    }

    public CallForwardDetails getForwardDetails() {
        return forwardDetails;
    }

    public void setForwardDetails(final CallForwardDetails forwardDetails) {
        this.forwardDetails = forwardDetails;
    }

    public Collection<Note> getNotes() {
        return notes;
    }

    public void setNotes(final Collection<Note> notes) {
        this.notes = notes;

        for (final Note note : notes) {
            userIds.add(note.getCreatedBy().getIamId());
        }
    }

    public boolean isParentCall() {
        return isParentCall;
    }

    public void setParentCall(final boolean parentCall) {
        isParentCall = parentCall;
    }

    public Long getCallLogId() {
        return callLogId;
    }

    public void setCallLogId(final Long callLogId) {
        this.callLogId = callLogId;
    }

    public boolean isStoredAsCallLog() {
        return isStoredAsCallLog;
    }

    public void setStoredAsCallLog(final boolean storedAsCallLog) {
        isStoredAsCallLog = storedAsCallLog;
    }

    public boolean isLoggable() {
        return Objects.nonNull(contact) && !isStoredAsCallLog;
    }

    public CallLog getCallLog() {
        return callLog;
    }

    public void setCallLog(final CallLog callLog) {
        this.callLog = callLog;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of user ids
     */
    @Override
    public Collection<Long> userIds() {
        return userIds;
    }
}
