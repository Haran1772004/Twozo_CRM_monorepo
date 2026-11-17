package com.tlc.crm.entity.features.api.activity.model;

import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.validator.annotation.ActivityValidator;
import com.tlc.crm.entity.models.app.CrmAppNotifiableModel;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Model which represents the Activity
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see TlcModel
 */
@ActivityValidator(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.validation.failed")
public class Activity implements TlcModel, CrmAppNotifiableModel {

    private static final Map<String, String> URLS = new HashMap<>(NumericalConstant.ONE);

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.activity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.org.invalid.id")
    private Long orgId;

    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.ACTIVITY_CREATED, EmailNotificationTemplate.ACTIVITY_MODIFIED,
                    EmailNotificationTemplate.ACTIVITY_COMPLETED},
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_CREATED, EmailNotificationTemplate.ACTIVITY_MODIFIED,
                    EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION, EmailNotificationTemplate.ACTIVITY_ASSIGNED,
                    EmailNotificationTemplate.ACTIVITY_COMPLETED},
            requirePreviousValue = true,
            requirePreviousValueFor = {EmailNotificationTemplate.ACTIVITY_CREATED, EmailNotificationTemplate.ACTIVITY_MODIFIED,
            EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION}
    )
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.invalid.creator.id")
    private Long creatorId;

    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.ACTIVITY_ASSIGNED, EmailNotificationTemplate.ACTIVITY_MODIFIED},
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_ASSIGNED},
            requirePreviousValue = true
    )
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.invalid.assignee.id")
    private Long assigneeId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.invalid.duration")
    private Integer duration;

    @SearchField(name = "activityType", readMethodName = "getType")
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.type.invalid")
    private ActivityType activityType;

    @EmailAlertField(
            name = "title",
            fieldType = EmailAlertFieldType.NUMBER,
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_MODIFIED},
            requirePreviousValue = true
    )
    @AuditEntityName
    @AuditField(aliasName = "title", displayName = "i18n.activity.fields.title")
    @SearchField(name = "name")
    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.title.invalid")
    private String title;

    @EmailAlertField(
            name = "startTime",
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_MODIFIED},
            requirePreviousValue = true,
            fieldType = EmailAlertFieldType.DATE_TIME
    )
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.startTime.invalid")
    @SearchField(name = "startTime")
    private DateTime startTime;
    @EmailAlertField(
            name = "endTime",
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_MODIFIED},
            requirePreviousValue = true,
            fieldType = EmailAlertFieldType.DATE_TIME
    )
    @SearchField(name = "endTime")
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.endTime.invalid")
    private DateTime endTime;

    @EmailAlertField(
            name = "description",
            fieldType = EmailAlertFieldType.NUMBER,
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_MODIFIED},
            requirePreviousValue = true
    )
    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.invalid.description.size")
    private String description;

    @SearchField(name = "status")
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.status.invalid")
    private Status status;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.availability.invalid")
    private Availability availability;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.reminder.invalid")
    private Reminder reminder;//TODO need to discuss

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.priority.invalid")
    private Priority priority;

    private DateTime completedTime;

    private DateTime lastNotificationTime;//TODO need to implement notifications.

    @EmailAlertField(
            name = "collaboratorIds",
            isUserField = true,
            recipientTemplates = {EmailNotificationTemplate.ACTIVITY_CREATED, EmailNotificationTemplate.ACTIVITY_MODIFIED},
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_CREATED, EmailNotificationTemplate.ACTIVITY_MODIFIED},
            requirePreviousValue = true,
            fieldType = EmailAlertFieldType.COLLECTION
    )
    private Collection<Long> collaboratorIds;

    private DateTime createdTime;

    private DateTime updatedTime;

    @SearchField(name = "note")
    private Note note;

    @SearchField(name = "isDone", readMethodName = "getDone")
    private boolean isDone;
    private transient CrmUser creator;
    private transient Collection<CrmUser> collaborators;
    private transient CrmUser assignee;
    private String location;
    private String eventId;
    private String referenceId;
    private boolean isActiveEvent;
    private boolean notifiable;
    private CheckInOutRecord checkInRecord;
    private CheckInOutRecord checkOutRecord;
    private long totalTimeSpent;
    private Long modifiedById;
    private CrmUser modifiedBy;

    public enum CompletionStatus {
        YES((byte) 1, true),
        NO((byte) 2, false);

        private final byte id;
        private final boolean status;

        CompletionStatus(final byte id, final boolean status) {
            this.id = id;
            this.status = status;
        }

        public static boolean getStatus(final Long id) {
            return 1 == id ? CompletionStatus.YES.status : CompletionStatus.NO.status;
        }

        public byte getId() {
            return id;
        }
    }

    public static String getURL(final String name) {
        return URLS.get(name);
    }

    public static void setURL(final String domain, final Configurations configurations) {
        final String summary = "summary";

        URLS.put(summary, domain + configurations.getConfig(summary));
    }

    @Override
    public Long id() {
        return id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setCreatorId(final Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(final Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(final Integer duration) {
        this.duration = duration;
    }

    public DateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(final DateTime completedTime) {
        this.completedTime = completedTime;
    }

    public DateTime getLastNotificationTime() {
        return lastNotificationTime;
    }

    public void setLastNotificationTime(final DateTime lastNotificationTime) {
        this.lastNotificationTime = lastNotificationTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public ActivityType getType() {
        return activityType;
    }

    public void setType(final ActivityType activityType) {
        this.activityType = activityType;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(final Availability availability) {
        this.availability = availability;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public void setReminder(final Reminder reminder) {
        this.reminder = reminder;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(final Priority priority) {
        this.priority = priority;
    }

    public Collection<Long> getCollaboratorIds() {
        return Objects.isNull(collaboratorIds) ? Collections.emptyList() : collaboratorIds;
    }

    public void setCollaboratorIds(final Collection<Long> collaboratorIds) {
        this.collaboratorIds = collaboratorIds;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(final Note note) {
        this.note = note;
    }

    public boolean getDone() {
        return isDone;
    }

    public void setDone(final boolean done) {
        isDone = done;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public Long orgId() {
        return this.orgId;
    }

    public void setCreator(final CrmUser creator) {
        this.creatorId = creator.id();
        this.creator = creator;
    }

    public CrmUser getCreator() {
        return creator;
    }

    public void setCollaborators(final Collection<CrmUser> collaborators) {
        this.collaborators = collaborators;
    }

    public Collection<CrmUser> getCollaborators() {
        return collaborators;
    }

    public void setAssignee(final CrmUser assignee) {
        this.assigneeId = assignee.id();
        this.assignee = assignee;
    }

    public CrmUser getAssignee() {
        return assignee;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public boolean isActiveEvent() {
        return isActiveEvent;
    }

    public void setActiveEvent(final boolean isActiveEvent) {
        this.isActiveEvent = isActiveEvent;
    }

    public CheckInOutRecord getCheckInRecord() {
        return checkInRecord;
    }

    public void setCheckInRecord(final CheckInOutRecord checkInRecord) {
        this.checkInRecord = checkInRecord;
    }

    public CheckInOutRecord getCheckOutRecord() {
        return checkOutRecord;
    }

    public void setCheckOutRecord(final CheckInOutRecord checkOutRecord) {
        this.checkOutRecord = checkOutRecord;
    }

    public long getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public void setTotalTimeSpent(final long totalTimeSpent) {
        this.totalTimeSpent = totalTimeSpent;
    }

    public Long getModifiedById() {
        return modifiedById;
    }

    public void setModifiedById(final Long modifiedById) {
        this.modifiedById = modifiedById;
    }

    public CrmUser getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(final CrmUser modifiedBy) {
        this.modifiedBy = modifiedBy;
        this.modifiedById = modifiedBy.id();
    }

    /**
     * Gets the check-out time of the activity
     *
     * @return The DateTime from check-out record, or null if not checked out
     */
    public DateTime getCheckedOutTime() {
        if (Objects.isNull(checkOutRecord)) {
            return null;
        }
        return new DateTime(checkOutRecord.getTime());
    }

    /**
     * Gets the check-in location of the activity
     *
     * @return The location string from check-in record, or null if not checked in
     */
    public String getCheckedInLocation() {
        if (Objects.isNull(checkInRecord)) {
            return null;
        }
        return checkInRecord.getLocation();
    }

    /**
     * Gets the check-in time of the activity
     *
     * @return The DateTime from check-in record, or null if not checked in
     */
    public DateTime getCheckedInTime() {
        if (Objects.isNull(checkInRecord)) {
            return null;
        }
        return new DateTime(checkInRecord.getTime());
    }

    /**
     * Gets the check-out location of the activity
     *
     * @return The location string from check-in record, or null if not checked in
     */
    public String getCheckedOutLocation() {
        if (Objects.isNull(checkOutRecord)) {
            return null;
        }
        return checkOutRecord.getLocation();
    }

    @Override
    public boolean notifiable() {
        return notifiable;
    }

    @Override
    public void setNotifiable(final boolean notifiable) {
        this.notifiable = notifiable;
    }

    @Override
    public Object identity() {
        return null;
    }
}
