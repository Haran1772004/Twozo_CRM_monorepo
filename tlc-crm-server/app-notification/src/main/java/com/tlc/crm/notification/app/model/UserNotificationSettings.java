package com.tlc.crm.notification.app.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A model that holds the notification settings configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class UserNotificationSettings implements TlcModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notification.invalid.user.id")
    private Long userId;

    private EmailNotificationSettings emailNotificationSettings;
    private SummaryNotificationSettings summaryNotificationSettings;
    private ActivityReminderNotificationSettings activityReminderNotificationSettings;
    private EmailReminderNotificationSettings emailReminderNotificationSettings;
    private AssignmentNotificationSettings assignmentNotificationSettings;
    private TrackingNotificationSettings trackingNotificationSettings;
    private ChatNotificationSettings chatNotificationSettings;
    private MentionAlertNotificationSettings mentionAlertNotificationSettings;
    private InAppNotificationSoundSettings inAppNotificationSoundSettings;

    public UserNotificationSettings() {
        emailNotificationSettings = new EmailNotificationSettings();
        summaryNotificationSettings = new SummaryNotificationSettings();
        assignmentNotificationSettings = new AssignmentNotificationSettings();
        trackingNotificationSettings = new TrackingNotificationSettings();
        activityReminderNotificationSettings = new ActivityReminderNotificationSettings(true,
                ReminderNotificationSettings.ReminderTime.FIFTEEN_MINUTES, ReminderNotificationSettings.ReminderType.ACTIVITY);
        emailReminderNotificationSettings = new EmailReminderNotificationSettings(true,
                ReminderNotificationSettings.ReminderTime.FIFTEEN_MINUTES, ReminderNotificationSettings.ReminderType.EMAIL);
        chatNotificationSettings = new ChatNotificationSettings();
        mentionAlertNotificationSettings = new MentionAlertNotificationSettings();
        inAppNotificationSoundSettings = new InAppNotificationSoundSettings();
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setEmailNotificationSettings(final EmailNotificationSettings emailNotificationSettings) {
        this.emailNotificationSettings = emailNotificationSettings;
    }

    public void setSummaryNotificationSettings(final SummaryNotificationSettings summaryNotificationSettings) {
        this.summaryNotificationSettings = summaryNotificationSettings;
    }

    public void setAssignmentNotificationSettings(final AssignmentNotificationSettings assignmentNotificationSettings) {
        this.assignmentNotificationSettings = assignmentNotificationSettings;
    }

    public void setTrackingNotificationSettings(final TrackingNotificationSettings trackingNotificationSettings) {
        this.trackingNotificationSettings = trackingNotificationSettings;
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

    public Long getUserId() {
        return userId;
    }

    public EmailNotificationSettings getEmailNotificationSettings() {
        return emailNotificationSettings;
    }

    public SummaryNotificationSettings getSummaryNotificationSettings() {
        return summaryNotificationSettings;
    }

    public AssignmentNotificationSettings getAssignmentNotificationSettings() {
        return assignmentNotificationSettings;
    }

    public TrackingNotificationSettings getTrackingNotificationSettings() {
        return trackingNotificationSettings;
    }

    public ActivityReminderNotificationSettings getActivityReminderNotificationSettings() {
        return activityReminderNotificationSettings;
    }

    public void setActivityReminderNotificationSettings(final ActivityReminderNotificationSettings activityReminderNotificationSettings) {
        this.activityReminderNotificationSettings = activityReminderNotificationSettings;
    }

    public EmailReminderNotificationSettings getEmailReminderNotificationSettings() {
        return emailReminderNotificationSettings;
    }

    public void setEmailReminderNotificationSettings(final EmailReminderNotificationSettings emailReminderNotificationSettings) {
        this.emailReminderNotificationSettings = emailReminderNotificationSettings;
    }

    public ChatNotificationSettings getChatNotificationSettings() {
        return chatNotificationSettings;
    }

    public void setChatNotificationSettings(final ChatNotificationSettings chatNotificationSettings) {
        this.chatNotificationSettings = chatNotificationSettings;
    }

    public MentionAlertNotificationSettings getMentionsAlertNotificationSettings() {
        return mentionAlertNotificationSettings;
    }

    public void setNoteMentionNotificationSettings(final MentionAlertNotificationSettings mentionAlertNotificationSettings) {
        this.mentionAlertNotificationSettings = mentionAlertNotificationSettings;
    }

    public InAppNotificationSoundSettings getInAppNotificationSoundSettings() {
        return inAppNotificationSoundSettings;
    }

    public void setInAppNotificationSoundSettings(final InAppNotificationSoundSettings inAppNotificationSoundSettings) {
        this.inAppNotificationSoundSettings = inAppNotificationSoundSettings;
    }
}
