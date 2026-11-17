package com.tlc.crm.notifications.api.model.alert.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.export.model.AppExportAlertRequest;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.notifications.api.model.alert.ActivityAlert;
import com.tlc.crm.notifications.api.model.alert.ExportAlert;
import com.tlc.crm.notifications.api.model.alert.EntityAssignmentAlert;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.api.model.alert.SocialChatAlert;
import com.tlc.crm.notifications.api.model.alert.SourceMentionAlert;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Converts notification alerts related details.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class NotificationAlertConverter {

    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;

    private NotificationAlertConverter() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
    }

    private static class Instance {
        private static final NotificationAlertConverter INSTANCE = new NotificationAlertConverter();
    }

    /**
     * <p>
     * Gets {@link NotificationAlertConverter}.
     * </p>
     *
     * @return {@link NotificationAlertConverter}.
     */
    public static NotificationAlertConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts {@link ActivityAlert}.
     * </p>
     *
     * @param orgId        An organization's id.
     * @param activityJson {@link JsonObject}.
     * @return {@link ActivityAlert}.
     */
    public ActivityAlert convertNewActivityAlert(final Long orgId, final JsonObject activityJson) {
        final ActivityAlert newActivityAlert = new ActivityAlert();

        newActivityAlert.setActivityId(activityJson.getLong(NotificationAttributes.ID));
        newActivityAlert.setAssigneeId(activityJson.getLong(NotificationAttributes.ASSIGNEE_ID));
        setNotificationAlert(orgId, System.currentTimeMillis(), buildActivityJson(orgId, activityJson, null),
                NotificationAlert.Category.NEW_ACTIVITY, newActivityAlert);

        return newActivityAlert;
    }

    /**
     * <p>
     * Converts {@link ActivityAlert}.
     * </p>
     *
     * @param orgId        An organization's id.
     * @param alertTime    Timestamp to alert.
     * @param reminder     {@link Reminder}.
     * @param activityJson {@link JsonObject}.
     * @return {@link ActivityAlert}.
     */
    public ActivityAlert convertActivityReminderAlert(final Long orgId, final long alertTime, final Reminder reminder,
                                                      final JsonObject activityJson) {
        final ActivityAlert activityReminderAlert = new ActivityAlert();

        activityReminderAlert.setActivityId(activityJson.getLong(NotificationAttributes.ID));
        activityReminderAlert.setAssigneeId(activityJson.getLong(NotificationAttributes.ASSIGNEE_ID));
        setNotificationAlert(orgId, alertTime, buildActivityJson(orgId, activityJson, reminder),
                NotificationAlert.Category.ACTIVITY_REMINDER, activityReminderAlert);

        return activityReminderAlert;
    }

    /**
     * <p>
     * Converts {@link ActivityAlert}.
     * </p>
     *
     * @param orgId        An organization's id.
     * @param activityJson {@link JsonObject}.
     * @return {@link ActivityAlert}.
     */
    public ActivityAlert convertActivityCollaboratorAlert(final Long orgId, final JsonObject activityJson) {
        final ActivityAlert collaboratorAlert = new ActivityAlert();

        collaboratorAlert.setActivityId(activityJson.getLong(NotificationAttributes.ID));
        setNotificationAlert(orgId, System.currentTimeMillis(), buildActivityJson(orgId, activityJson, null),
                NotificationAlert.Category.COLLABORATOR_ADDED, collaboratorAlert);

        return collaboratorAlert;
    }

    /**
     * <p>
     * Converts {@link SocialChatAlert}.
     * </p>
     *
     * @param orgId       An organization's id.
     * @param assigneeIds Assignee's ids.
     * @param category    {@link NotificationAlert.Category}.
     * @param chatJson    {@link JsonObject}.
     * @return {@link SocialChatAlert}.
     */
    public SocialChatAlert convertChatAlert(final Long orgId, final Collection<Long> assigneeIds,
                                            final NotificationAlert.Category category,
                                            final JsonObject chatJson) {
        final SocialChatAlert socialChatAlert = new SocialChatAlert();

        socialChatAlert.setAssigneeIds(assigneeIds);
        setNotificationAlert(orgId, null, chatJson, category, socialChatAlert);

        return socialChatAlert;
    }

    /**
     * <p>
     * Converts {@link EntityAssignmentAlert}.
     * </p>
     *
     * @param orgId      An organization's id.
     * @param assigneeId Assignee's id.
     * @param entities   Entity data array.
     * @return {@link EntityAssignmentAlert}.
     */
    public EntityAssignmentAlert convertEntityAssignmentAlert(final Long orgId, final Long assigneeId, final JsonObject entities,
                                                              final NotificationAlert.Category category) {
        final EntityAssignmentAlert entityAssignmentAlert = new EntityAssignmentAlert();

        entityAssignmentAlert.setAssigneeId(assigneeId);
        setNotificationAlert(orgId, null, entities, category, entityAssignmentAlert);

        return entityAssignmentAlert;
    }

    /**
     * <p>
     * Converts {@link SourceMentionAlert}.
     * </p>
     *
     * @param orgId    An organization's id.
     * @param source   The {@link Source}.
     * @param tlcModel The {@link TlcModel}.
     * @param crmUser  The {@link CrmUser}.
     * @param category Represents the {@link  NotificationAlert.Category}
     * @return {@link SourceMentionAlert}.
     */
    public SourceMentionAlert convertSourceMentionAlert(final Long orgId, final Source source, final TlcModel tlcModel,
                                                     final CrmUser crmUser, final NotificationAlert.Category category) {
        final SourceMentionAlert sourceMentionAlert = new SourceMentionAlert();
        final JsonObject contentJson = Json.object();
        final JsonObject userJson = Json.object();
        final JsonObject sourceJson = Json.object();
        final Long userId = crmUser.id();
        final CrmUser mentionedBy = LoggedInUserInfo.getUser();

        switch (source) {
            case CONTACT -> {
                final Contact contact = (Contact) tlcModel;

                sourceJson.put(NotificationAttributes.NAME, contact.getFullName());
            }
            case COMPANY -> {
                final Company company = (Company) tlcModel;

                sourceJson.put(NotificationAttributes.NAME, company.getName());
            }
            case DEAL -> {
                final Deal deal = (Deal) tlcModel;

                sourceJson.put(NotificationAttributes.NAME, deal.getTitle());
            }
            case ACTIVITY -> {
                final ActivityHolder activityHolder = (ActivityHolder) tlcModel;
                final Activity activity = activityHolder.getActivity();

                sourceJson.put(NotificationAttributes.NAME, activity.getTitle());
            }
        }
        userJson.put(NotificationAttributes.ID, mentionedBy.id());
        userJson.put(NotificationAttributes.NAME, mentionedBy.getFullName());
        sourceJson.put(NotificationAttributes.ID, tlcModel.id());
        contentJson.put(NotificationAttributes.USER, userJson);
        contentJson.put(source.getName().toLowerCase(), sourceJson);

        sourceMentionAlert.setOrgId(orgId);
        sourceMentionAlert.setContent(contentJson);
        sourceMentionAlert.setAssigneeId(userId);
        sourceMentionAlert.setCategory(category);
        sourceMentionAlert.setSeverity(NotificationAlert.Severity.ALERT);

        return sourceMentionAlert;
    }

    /**
     * <p>
     * Converts {@link ExportAlert}.
     * </p>
     *
     * @param orgId   An organization's id.
     * @param request {@link AppExportAlertRequest}.
     * @param category {@link NotificationAlert.Category}.
     * @return {@link ExportAlert}.
     */
    public ExportAlert convertExportAlert(final Long orgId, final AppExportAlertRequest request,
                                         final NotificationAlert.Category category) {
        final ExportAlert exportAlert = new ExportAlert();
        final JsonObject contentJson = Json.object();

        contentJson.put(NotificationAttributes.ID, request.id());
        contentJson.put(NotificationAttributes.EXPORT_NAME, request.getExportName());

        if (Objects.nonNull(request.getFileDownloadUrl())) {
            contentJson.put(NotificationAttributes.FILE_DOWNLOAD_URL, request.getFileDownloadUrl());
        }

        exportAlert.setAssigneeId(request.getUserId());
        setNotificationAlert(orgId, System.currentTimeMillis(), contentJson, category, exportAlert);

        return exportAlert;
    }

    /**
     * <p>
     * Builds {@link JsonObject} for activity.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param json  Represents the {@link JsonObject}
     * @return the {@link JsonObject}
     */
    private JsonObject buildActivityJson(final long orgId, final JsonObject json, final Reminder reminder) {
        final JsonObject jsonObject = Json.object(json.getBytes());

        if (jsonObject.containsKey(NotificationAttributes.CONTACTS)) {
            final JsonArray contacts = jsonObject.getJsonArray(NotificationAttributes.CONTACTS);
            final Collection<Long> ids = new HashSet<>();
            final int size = contacts.size();

            for (int index = 0; index < size; index++) {
                ids.add((Long) contacts.get(index));
            }
            final Collection<Contact> activityContacts = contactManager.partialGet(orgId, ids);
            final JsonArray contactArray = Json.array();

            for (final Contact contact : activityContacts) {
                final JsonObject contactJson = Json.object();

                contactJson.put(NotificationAttributes.ID, contact.id()).put(NotificationAttributes.NAME, contact.getFullName());
                contactArray.put(contactJson);
            }
            jsonObject.put(NotificationAttributes.CONTACTS, contactArray);
        }

        if (jsonObject.containsKey(NotificationAttributes.DEAL)) {
            final JsonObject dealJson = jsonObject.getJsonObject(NotificationAttributes.DEAL);
            final String title = dealManager.partialGet(orgId, dealJson.getLong(NotificationAttributes.ID)).getTitle();

            dealJson.put(NotificationAttributes.NAME, title);
        }

        if (jsonObject.containsKey(NotificationAttributes.COMPANY)) {
            final JsonObject companyJson = jsonObject.getJsonObject(NotificationAttributes.COMPANY);
            final String name = companyManager.partialGet(orgId, companyJson.getLong(NotificationAttributes.ID)).getName();

            companyJson.put(NotificationAttributes.NAME, name);
        }

        if (Objects.isNull(reminder)) {
            jsonObject.remove(NotificationAttributes.REMINDER);
        } else {
            jsonObject.put(NotificationAttributes.REMINDER, reminder.getId());
        }

        jsonObject.remove(NotificationAttributes.COLLABORATOR_IDS).remove(NotificationAttributes.ASSIGNEE_ID);

        return jsonObject;
    }

    /**
     * <p>
     * Sets {@link NotificationAlert}.
     * </p>
     *
     * @param orgId             An organization's id.
     * @param timeStamp         Timestamp value.
     * @param content           {@link JsonObject} data.
     * @param category          {@link NotificationAlert.Category}.
     * @param notificationAlert {@link NotificationAlert}.
     */
    private void setNotificationAlert(final long orgId, final Long timeStamp, final JsonObject content,
                                      final NotificationAlert.Category category, final NotificationAlert notificationAlert) {
        notificationAlert.setOrgId(orgId);
        notificationAlert.setTimeStamp(timeStamp);
        notificationAlert.setContent(content);
        notificationAlert.setSeverity(NotificationAlert.Severity.ALERT);
        notificationAlert.setCategory(category);
    }
}