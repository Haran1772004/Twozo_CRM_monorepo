package com.tlc.crm.notifications.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.notification.email.api.model.NotificationTrackingDetails;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.notifications.api.model.AppNotification;

import com.tlc.crm.notifications.api.model.NotificationUser;
import com.tlc.crm.notifications.api.model.alert.ActivityAlert;
import com.tlc.crm.notifications.api.model.alert.ExportAlert;
import com.tlc.crm.notifications.api.model.alert.EntityAssignmentAlert;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.api.model.alert.SocialChatAlert;

import com.tlc.crm.notifications.api.model.alert.SourceMentionAlert;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notification.app.AppNotificationEventType;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import com.tlc.email.common.model.EmailMetaData;
import com.tlc.social.common.model.Message;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Converter used to convert the models to {@link JsonObject}
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class AppNotificationConverter {

    private final DateTimeUtil dateTimeUtil;
    private final ActivityTypeManager activityTypeManager;

    private AppNotificationConverter() {
        dateTimeUtil = DateTimeUtil.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
    }

    public static AppNotificationConverter getInstance() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final AppNotificationConverter INSTANCE = new AppNotificationConverter();
    }

    /**
     * <p>
     * Converts the activities to {@link JsonArray}
     * </p>
     *
     * @param activityHolders Represents the activity holders.
     * @return the {@link JsonArray}
     */
    public JsonArray convertActivities(final Collection<ActivityHolder> activityHolders) {
        final JsonArray jsonArray = Json.array();
        final Collection<ActivityHolder> notifiableActivities = activityHolders.stream().filter(holder -> holder.getActivity().notifiable()).toList();

        for (final ActivityHolder activityHolder : notifiableActivities) {
            final JsonObject activityJson = Json.object();
            final Company company = activityHolder.getCompany();
            final Deal deal = activityHolder.getDeal();
            final Activity activity = activityHolder.getActivity();
            final Reminder reminder = activity.getReminder();
            final Collection<Long> collaboratorIds = activity.getCollaboratorIds();
            final JsonArray contactArray = Json.array();

            activity.setType(activityTypeManager.get(activity.orgId(), activity.getType().id()));

            for (final Contact contact : activityHolder.getContacts()) {
                contactArray.put(contact.id());
            }

            if (!Json.isEmpty(contactArray)) {
                activityJson.put(NotificationAttributes.CONTACTS, contactArray);
            }

            if (Objects.nonNull(deal)) {
                final JsonObject dealJson = Json.object();

                dealJson.put(NotificationAttributes.ID, deal.id());
                activityJson.put(NotificationAttributes.DEAL, dealJson);
            }

            if (Objects.nonNull(company)) {
                final JsonObject companyJson = Json.object();

                companyJson.put(NotificationAttributes.ID, company.id());
                activityJson.put(NotificationAttributes.COMPANY, companyJson);
            }

            if (!collaboratorIds.isEmpty()) {
                final JsonArray collaboratorIdsArray = Json.array();

                collaboratorIds.forEach(collaboratorIdsArray::put);
                activityJson.put(NotificationAttributes.COLLABORATOR_IDS, collaboratorIdsArray);
            }

            if (Objects.nonNull(reminder)) {
                activityJson.put(NotificationAttributes.REMINDER, reminder.name().toLowerCase());
            }

            activityJson.put(NotificationAttributes.ID, activity.id())
                    .put(NotificationAttributes.ACTIVITY_TITLE, activity.getTitle())
                    .put(NotificationAttributes.START_TIME, dateTimeUtil.getTimeStamp(activity.getStartTime()))
                    .put(NotificationAttributes.END_TIME, dateTimeUtil.getTimeStamp(activity.getEndTime()))
                    .put(NotificationAttributes.ASSIGNEE_ID, activity.getAssigneeId());

            jsonArray.put(activityJson);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the companies to {@link JsonObject}
     * </p>
     *
     * @param companies Represents the companies
     * @return the {@link JsonObject}
     */
    public JsonObject convertCompaniesToJson(final Collection<Company> companies) {
        final JsonArray jsonArray = Json.array();

        for (final Company company : companies) {
            final JsonObject companyJson = Json.object();

            companyJson.put(NotificationAttributes.ID, company.id()).put(NotificationAttributes.NAME, company.getName())
                    .put(NotificationAttributes.OWNER_ID, company.getSalesOwner().id());
            jsonArray.put(companyJson);
        }
        return Json.object().put(NotificationAttributes.COMPANIES, jsonArray);
    }

    /**
     * <p>
     * Converts the contacts to {@link JsonObject}
     * </p>
     *
     * @param contacts Represents the contacts
     * @return the {@link JsonObject}
     */
    public JsonObject convertContactsToJson(final Collection<Contact> contacts) {
        final JsonArray jsonArray = Json.array();

        for (final Contact contact : contacts) {
            jsonArray.put(Json.object().
                    put(NotificationAttributes.ID, contact.id()).
                    put(NotificationAttributes.NAME, contact.getFullName()));
        }
        return Json.object().put(NotificationAttributes.CONTACTS, jsonArray);
    }

    /**
     * <p>
     * Converts the deals to {@link JsonObject}
     * </p>
     *
     * @param deals Represents the deals
     * @return the {@link JsonObject}
     */
    public JsonObject convertDealsToJson(final Collection<Deal> deals) {
        final JsonArray jsonArray = Json.array();

        for (final Deal deal : deals) {
            jsonArray.put(Json.object().
                    put(NotificationAttributes.ID, deal.id()).
                    put(NotificationAttributes.NAME, deal.getTitle()));
        }
        return Json.object().put(NotificationAttributes.DEALS, jsonArray);
    }

    /**
     * <p>
     * Converts the products to {@link JsonObject}
     * </p>
     *
     * @param products Represents the products
     * @return the {@link JsonObject}
     */
    public JsonObject convertProductsToJson(final Collection<Product> products) {
        final JsonArray jsonArray = Json.array();

        for (final Product product : products) {
            jsonArray.put(Json.object().
                    put(NotificationAttributes.ID, product.id()).
                    put(NotificationAttributes.NAME, product.getName()));
        }
        return Json.object().put(NotificationAttributes.PRODUCTS, jsonArray);
    }

    /**
     * <p>
     * Converts the crm roles to json
     * </p>
     *
     * @param crmRoles Represents the {@link CrmRole}
     * @return the {@link JsonObject}
     */
    public JsonArray convertRolesToJson(final Collection<CrmRole> crmRoles) {
        final JsonArray jsonArray = Json.array();

        for (final CrmRole crmRole : crmRoles) {
            final JsonObject roleJson = Json.object();

            roleJson.put(NotificationAttributes.ID, crmRole.id());
            roleJson.put(NotificationAttributes.OLD_VALUE, crmRole.getName()).
                    put(NotificationAttributes.USER_IDS, crmRole.getUserIds());
            jsonArray.put(roleJson);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the email notifications to {@link JsonObject}
     * </p>
     *
     * @param emails Represents the collection of {@link EntityEmail}
     * @return the {@link JsonObject}
     */
    public JsonArray convertEmailsToJson(final Collection<EntityEmail> emails) {
        final JsonArray jsonArray = Json.array();

        for (final EntityEmail email : emails) {
            final long userId = email.getUserId();
            final Long id = email.getCrmId();

            if (userId > 0 && id > 0) {
                final EmailMetaData emailData = email.getMetaData();
                final JsonObject emailJson = Json.object();

                emailJson.put(NotificationAttributes.ID, id);
                emailJson.put(NotificationAttributes.USER_ID, userId);
                emailJson.put(NotificationAttributes.SUBJECT, email.getSubject());
                emailJson.put(NotificationAttributes.IS_INITIAL_MESSAGE, emailData.isInitialMessage());
                emailJson.put(NotificationAttributes.EMAIL, email.getSender().getEmailAddress());
                jsonArray.put(emailJson);
            }
        }

        return jsonArray;
    }

    /**
     * <p>
     * Constructs the given data into {@link AppNotification}
     * </p>
     *
     * @param orgId                    Represents an organization id
     * @param sender                   Represents the sender
     * @param receivers                Represents the receivers
     * @param jsonObject               Represents the {@link JsonObject}
     * @param appNotificationEventType Represents the {@link AppNotificationEventType}
     * @return the {@link AppNotification}
     */
    public AppNotification constructNotification(final long orgId, final NotificationUser sender, final List<NotificationUser> receivers,
                                                 final JsonObject jsonObject, final AppNotificationEventType appNotificationEventType) {
        final AppNotification appNotification = new AppNotification();

        appNotification.setRecipients(receivers);
        appNotification.setSender(sender);
        appNotification.setActionTime(Instant.now().toEpochMilli());
        appNotification.setNotification(jsonObject.toString());
        appNotification.setType(appNotificationEventType);
        appNotification.setOrgId(orgId);

        return appNotification;
    }

    /**
     * <p>
     * Converts the {@link NotificationTrackingDetails} to {@link JsonObject}
     * </p>
     *
     * @param notificationTrackingDetails Represents the collection of {@link NotificationTrackingDetails}
     * @return the {@link JsonObject}
     */
    public JsonObject convertTrackingDetailsToJson(final Collection<NotificationTrackingDetails> notificationTrackingDetails) {
        final Optional<NotificationTrackingDetails> notificationTrackingDetail = notificationTrackingDetails.stream().findFirst();

        if (notificationTrackingDetail.isPresent()) {
            final JsonObject jsonObject = Json.object();
            final NotificationTrackingDetails trackingDetail = notificationTrackingDetail.get();

            jsonObject.put(NotificationAttributes.ORG_ID, trackingDetail.orgId());
            jsonObject.put(NotificationAttributes.USER_ID, trackingDetail.getUserId());
            jsonObject.put(NotificationAttributes.TYPE, trackingDetail.getType().getName());
            jsonObject.put(NotificationAttributes.ID, trackingDetail.id());
            jsonObject.put(NotificationAttributes.SUBJECT, trackingDetail.getSubject());

            return jsonObject;

        }
        return Json.object();
    }

    /**
     * <p>
     * Converts the collection of {@link CrmChat} to {@link JsonObject}
     * </p>
     *
     * @param crmChats Represents the collection of {@link CrmChat}
     * @return the {@link JsonObject}
     */
    public JsonObject convertChatsToJson(final Collection<CrmChat> crmChats) {
        final JsonObject jsonObject = Json.object();
        final JsonArray jsonArray = Json.array();

        for (final CrmChat chat : crmChats) {
            final JsonObject chatJson = Json.object();
            final Message lastMessage = chat.getLastMessage();
            final RecipientProfile recipientProfile = chat.getProfile();
            final Platform platform = chat.getPlatform();
            final Long userId = chat.getOwnerId();

            chatJson.put(NotificationAttributes.ID, chat.getCrmChatId());
            chatJson.put(NotificationAttributes.ORG_ID, chat.orgId());
            chatJson.put(NotificationAttributes.CHAT_ID, chat.getCrmChatId());

            if (Objects.nonNull(userId)) {
                chatJson.put(NotificationAttributes.OWNER_ID, chat.getOwnerId());
            }

            if (Objects.nonNull(lastMessage)) {
                chatJson.put(NotificationAttributes.SENT_TIME, chat.getLastMessageTime());
                chatJson.put(NotificationAttributes.MESSAGE, chat.getRecentMessagePreview(NumericalConstant.TWO_HUNDRED));
            }

            if (Objects.nonNull(recipientProfile)) {
                if (Platform.WHATSAPP == platform) {
                    final String name = recipientProfile.getName();
                    final int delimiterIndex = name.lastIndexOf("|");

                    if (delimiterIndex != -1) {
                        final StringBuilder recipientBuilder = new StringBuilder(
                                name.substring(NumericalConstant.ZERO, delimiterIndex));

                        recipientBuilder.append(" (").append(name.substring(delimiterIndex + NumericalConstant.ONE)).append(")");
                        chatJson.put(NotificationAttributes.RECIPIENT_NAME, recipientBuilder.toString());
                    } else {
                        chatJson.put(NotificationAttributes.RECIPIENT_NAME, recipientProfile.getName());
                    }
                } else {
                    chatJson.put(NotificationAttributes.RECIPIENT_NAME, recipientProfile.getName());
                }
            }

            if (Objects.nonNull(platform)) {
                chatJson.put(NotificationAttributes.PLATFORM, platform.getId());
            }
            jsonArray.put(chatJson);
        }
        jsonObject.put(NotificationAttributes.CHATS, jsonArray);

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link SocialChatAlert}.
     * </p>
     *
     * @param isRead          Whether the alert is read.
     * @param userId          A user's id.
     * @param socialChatAlert {@link SocialChatAlert}.
     * @return {@link SocialChatAlert}.
     */
    public AppNotification convertChatAlert(final boolean isRead, final Long userId,
                                            final SocialChatAlert socialChatAlert) {

        return convert(isRead, userId, NotificationAlert.Category.CHAT_INITIATED == socialChatAlert.getCategory() ?
                AppNotificationEventType.CHAT_INITIATED : AppNotificationEventType.CHAT_ASSIGNED, socialChatAlert);
    }

    /**
     * <p>
     * Converts {@link ActivityAlert}.
     * </p>
     *
     * @param isRead        Whether the alert is read.
     * @param userId        A user's id.
     * @param activityAlert {@link ActivityAlert}.
     * @return {@link ActivityAlert}.
     */
    public AppNotification convertActivityAlert(final boolean isRead, final Long userId, final ActivityAlert activityAlert) {
        final AppNotificationEventType notificationEventType = switch (activityAlert.getCategory()) {
            case NEW_ACTIVITY -> AppNotificationEventType.NEW_ACTIVITY;
            case ACTIVITY_REMINDER -> AppNotificationEventType.ACTIVITY_REMINDER;
            case COLLABORATOR_ADDED -> AppNotificationEventType.COLLABORATOR_ADDED;
            default -> null;
        };

        return Objects.isNull(notificationEventType) ? null : convert(isRead, userId, notificationEventType, activityAlert);
    }

    /**
     * <p>
     * Converts {@link EntityAssignmentAlert}.
     * </p>
     *
     * @param isRead                Whether the alert is read.
     * @param userId                A user's id.
     * @param entityAssignmentAlert {@link ActivityAlert}.
     * @return {@link EntityAssignmentAlert}.
     */
    public AppNotification convertEntityAssignmentAlert(final boolean isRead, final Long userId,
                                                        final EntityAssignmentAlert entityAssignmentAlert) {

        final AppNotificationEventType eventType = switch (entityAssignmentAlert.getCategory()) {
            case COMPANY_ASSIGNED -> AppNotificationEventType.COMPANY_ASSIGNED;
            case CONTACT_ASSIGNED -> AppNotificationEventType.CONTACT_ASSIGNED;
            case DEAL_ASSIGNED -> AppNotificationEventType.DEAL_ASSIGNED;
            case PRODUCT_ASSIGNED -> AppNotificationEventType.PRODUCT_ASSIGNED;
            default -> null;
        };

        return Objects.isNull(eventType) ? null : convert(isRead, userId, eventType, entityAssignmentAlert);
    }

    /**
     * <p>
     * Converts {@link SourceMentionAlert}.
     * </p>
     *
     * @param isRead                Whether the alert is read.
     * @param userId                A user's id.
     * @param sourceMentionAlert {@link SourceMentionAlert}.
     * @return {@link AppNotification}.
     */
    public AppNotification convertSourceMentionAlert(final boolean isRead, final long userId, final SourceMentionAlert sourceMentionAlert) {
        final AppNotificationEventType eventType = switch (sourceMentionAlert.getCategory()) {
            case CONTACT_NOTE_MENTIONED -> AppNotificationEventType.CONTACT_NOTE_MENTIONED;
            case COMPANY_NOTE_MENTIONED -> AppNotificationEventType.COMPANY_NOTE_MENTIONED;
            case DEAL_NOTE_MENTIONED -> AppNotificationEventType.DEAL_NOTE_MENTIONED;
            case ACTIVITY_PRIVATE_NOTE_MENTIONED -> AppNotificationEventType.ACTIVITY_PRIVATE_NOTE_MENTIONED;
            case CONTACT_NOTE_COMMENT_MENTIONED -> AppNotificationEventType.CONTACT_NOTE_COMMENT_MENTIONED;
            case COMPANY_NOTE_COMMENT_MENTIONED -> AppNotificationEventType.COMPANY_NOTE_COMMENT_MENTIONED;
            case DEAL_NOTE_COMMENT_MENTIONED -> AppNotificationEventType.DEAL_NOTE_COMMENT_MENTIONED;
            default -> null;
        };
        return Objects.isNull(eventType) ? null : convert(isRead, userId, eventType, sourceMentionAlert);
    }

    /**
     * <p>
     * Converts {@link ExportAlert}.
     * </p>
     *
     * @param isRead      Whether the alert is read.
     * @param userId      A user's id.
     * @param exportAlert {@link ExportAlert}.
     * @return {@link AppNotification}.
     */
    public AppNotification convertExportAlert(final boolean isRead, final long userId, final ExportAlert exportAlert) {

        return convert(isRead, userId, AppNotificationEventType.EXPORTED, exportAlert);
    }

    /**
     * <p>
     * Converts {@link NotificationAlert} to {@link AppNotification}.
     * </p>
     *
     * @param isRead            Whether the alert is read.
     * @param userId            A user's id.
     * @param eventType         {@link AppNotificationEventType}.
     * @param notificationAlert {@link NotificationAlert}
     * @return {@link AppNotification}.
     */
    private AppNotification convert(final boolean isRead, final Long userId, final AppNotificationEventType eventType,
                                    final NotificationAlert notificationAlert) {
        final Long orgId = notificationAlert.orgId();
        final AppNotification appNotification = new AppNotification();
        final NotificationUser sender = new NotificationUser();
        final NotificationUser receiver = new NotificationUser();

        receiver.setOrgId(orgId);
        receiver.setId(userId);
        sender.setOrgId(orgId);
        sender.setId(userId);
        appNotification.setRead(isRead);
        appNotification.setRecipients(List.of(receiver));
        appNotification.setSender(sender);
        appNotification.setActionTime(notificationAlert.getTimeStamp());
        appNotification.setNotification(notificationAlert.getContent().toString());
        appNotification.setType(eventType);
        appNotification.setOrgId(orgId);

        return appNotification;
    }
}
