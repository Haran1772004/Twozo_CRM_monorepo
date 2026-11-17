package com.tlc.crm.notifications.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.models.NotificationState;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.fields.NotificationIconsKeys;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.page.NotificationAlertPageHandler;
import com.tlc.crm.notifications.service.AppNotificationManager;
import com.tlc.crm.notifications.api.model.AppNotification;

import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.notifications.service.NotificationAlertManager;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.social.common.model.Platform;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Manages app notification related web actions for storing, retrieving, read notifications and notification state set up
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@WebAction(path = "/app/notification")
public class AppNotificationManagement extends AuthorizationController {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_ACTIVITY_REMINDER_LIMIT = 10;
    private static final int DEFAULT_LIMIT_FOR_TOP_NOTIFICATIONS = 3;

    private final AppNotificationManager appNotificationManager;
    private final NotificationAlertManager notificationAlertManager;
    private final IconService iconService;
    private final I18nService i18nService;
    private final ModelValidator modelValidator;

    public AppNotificationManagement() {
        appNotificationManager = AppNotificationManager.getInstance();
        notificationAlertManager = NotificationAlertManager.getInstance();
        iconService = IconService.getInstance();
        i18nService = I18nServiceProvider.get();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(NotificationFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(NotificationFields.DATA.getName());
        final long orgId = request.orgId();
        final long userId = request.userId();
        final Locale locale = request.locale();

        switch (type) {
            case "markAllAsRead" -> markAllAsRead(orgId, userId, crmResponse);
            case "markAsRead" -> markAsRead(orgId, userId, data.getLong(NotificationFields.ID.getName()),
                    data.getString(NotificationAttributes.CATEGORY), crmResponse);
            case "updateState" -> updateNotificationState(orgId, userId, data, crmResponse);
            case "getState" -> crmResponse.put(NotificationFields.NOTIFICATION_STATE.getName(), getNotificationState(orgId, userId));
            case "pendingNotification" -> {
                final int pendingNotificationCount = appNotificationManager.getPendingNotificationCount(orgId, userId);

                if (NumericalConstant.ZERO < pendingNotificationCount) {
                    crmResponse.put(NotificationFields.PENDING_NOTIFICATION_COUNT.getName(), pendingNotificationCount);
                }
            }
            case "get" -> crmResponse.put(NotificationFields.NOTIFICATIONS.getName(), getNotifications(orgId, userId, data, locale, crmResponse));
            case "getTopNotifications" -> crmResponse.put(NotificationFields.NOTIFICATIONS.getName(), getTopNotifications(orgId, userId, locale));
            case "updatePendingNotification" -> resetPendingNotificationCount(orgId, userId, crmResponse);
            case "getAlerts" -> crmResponse.put(NotificationFields.ALERTS.getName(), getAlerts(orgId, userId, data, locale, crmResponse));
            case "getReminderActivities" -> crmResponse.put(NotificationFields.NOTIFICATIONS.getName(), getReminderActivities(orgId, userId, locale));
            case "markAsDelivered" -> markAsDelivered(orgId, userId, data.getLong(NotificationFields.ID.getName()), crmResponse);
        }
    }

    /**
     * <p>
     * Reads all the notification of the user based on the organization
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void markAllAsRead(final long orgId, final long userId, final CrmResponse crmResponse) {
        appNotificationManager.markAllAsRead(orgId, userId);
        crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.app.notification.mark.as.read.success"));
    }

    /**
     * <p>
     * Fetches the notification state for the given user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the notification state
     */
    private String getNotificationState(final long orgId, final long userId) {
        try {
            final NotificationState notificationState = appNotificationManager.getNotificationState(orgId, userId);

            if (Objects.isNull(notificationState)) {
                throw ErrorCode.get(NotificationErrorCodes.NOTIFICATION_STATE_NOT_FOUND, "i18n.app.notification.state.not.found");
            } else {
                return switch (notificationState) {
                    case ON -> NotificationState.ON.getName();
                    case OFF -> NotificationState.OFF.getName();
                };
            }
        } catch (Exception e) {
            throw ErrorCode.get(NotificationErrorCodes.NOTIFICATION_STATE_NOT_FOUND, "i18n.app.notification.state.not.found");
        }
    }

    /**
     * <p>
     * Updates the notification state based on the given data
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateNotificationState(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final AppNotification appNotification = new AppNotification();
        final String notificationState = data.optString(NotificationFields.NOTIFICATION_STATE.getName(), appNotification.getState());

        appNotification.setOrgId(orgId);
        appNotification.setUserId(userId);
        appNotification.setState(notificationState);
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(appNotification), Group.Update.class);

        if (errors.isEmpty()) {
            try {
                appNotificationManager.updateNotificationState(orgId, userId, NotificationState.getByName(notificationState));
                crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.app.notification.state.modification.success"));
            } catch (Exception e) {
                sendError(crmResponse, null, ErrorCode.get(NotificationErrorCodes.NOTIFICATION_STATE_NOT_FOUND, "i18n.app.notification.state.modification.failed"));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.STATE_MODIFICATION_FAILED, "i18n.app.notification.state.modification.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Marks the notification as read for the given notification id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param id          Represents the notification id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void markAsRead(final long orgId, final long userId, final long id, final String category,
                            final CrmResponse crmResponse) {

        switch (category) {
            case NotificationAttributes.ALERT -> {
                if (notificationAlertManager.exists(orgId, id)) {
                    notificationAlertManager.markAsRead(orgId, userId, id);
                    crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.app.alert.mark.as.read.success"));
                } else {
                    sendError(crmResponse, null, ErrorCode.get(NotificationErrorCodes.ALERT_NOT_FOUND, "i18n.app.notification.not.found"));
                }
            }
            case NotificationAttributes.NOTIFICATION -> {
                if (appNotificationManager.exists(orgId, userId, id)) {
                    appNotificationManager.markAsRead(orgId, userId, id);
                    crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.app.alert.mark.as.read.success"));
                } else {
                    sendError(crmResponse, null, ErrorCode.get(NotificationErrorCodes.NOTIFICATION_NOT_FOUND, "i18n.app.notification.not.found"));
                }
            }
        }
    }

    /**
     * <p>
     * Marks the notification as delivered for the given notification id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param id          Represents the notification id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void markAsDelivered(final long orgId, final long userId, final long id, final CrmResponse crmResponse) {

        if (notificationAlertManager.exists(orgId, id)) {
            notificationAlertManager.markAsDelivered(orgId, userId, List.of(id));
        } else {
            sendError(crmResponse, null, ErrorCode.get(NotificationErrorCodes.ALERT_NOT_FOUND, "i18n.app.notification.not.found"));
        }
    }

    /**
     * <p>
     * Fetches the notifications for the given user in an organization
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     * @return the {@link JsonArray}
     */
    private JsonArray getNotifications(final long orgId, final long userId, final JsonObject data, final Locale locale, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(NotificationFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(NotificationFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = appNotificationManager.getTotalCount(orgId, userId);
        final Collection<AppNotification> appNotifications = appNotificationManager.get(orgId, userId, start, limit);

        crmResponse.put(NotificationFields.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(NotificationFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        if (Objects.isNull(appNotifications)) {
            return Json.array();
        } else {
            return convertNotificationToJson(orgId, appNotifications, locale);
        }
    }

    /**
     * <p>
     * Fetches the the top notifications for the given user in an organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link JsonArray}
     */
    private JsonArray getTopNotifications(final long orgId, final long userId, final Locale locale) {
        try {
            final NotificationState notificationState = appNotificationManager.getNotificationState(orgId, userId);

            if (Objects.nonNull(notificationState) && NotificationState.ON == notificationState) {
                final Collection<AppNotification> appNotifications = appNotificationManager.getTopNotifications(orgId, userId, DEFAULT_START, DEFAULT_LIMIT_FOR_TOP_NOTIFICATIONS);

                if (Objects.isNull(appNotifications)) {
                    return Json.array();
                }
                return convertNotificationToJson(orgId, appNotifications, locale);
            } else {
                throw ErrorCode.get(NotificationErrorCodes.STATE_IS_TURNED_OFF, "i18n.app.notification.state.turned.off");
            }
        } catch (Exception e) {
            throw ErrorCode.get(NotificationErrorCodes.NOTIFICATION_STATE_NOT_FOUND, "i18n.app.notification.state.not.found");
        }
    }

    /**
     * <p>
     * Converts the notifications into {@link JsonArray}
     * </p>
     *
     * @param appNotifications Represents the collection of {@link AppNotification}
     * @return the {@link JsonArray}
     */
    private JsonArray convertNotificationToJson(final long orgId, final Collection<? extends AppNotification> appNotifications, final Locale locale) {
        final JsonArray jsonArray = Json.array();

        for (final AppNotification appNotification : appNotifications) {
            final JsonObject jsonObject = Json.object();
            final AppNotificationEventType type = appNotification.getType();
            final JsonObject notificationAsJson = appNotification.getNotificationAsJson();
            final Icon icon = fetchIcon(orgId, type, notificationAsJson);

            jsonObject.put(NotificationFields.ID.getName(), appNotification.id());
            jsonObject.put(NotificationFields.TYPE.getName(), type.name());
            jsonObject.put(NotificationFields.TIMESTAMP.getName(), appNotification.getActionTime());
            jsonObject.put(NotificationFields.IS_READ.getName(), appNotification.isRead());
            jsonObject.put(NotificationFields.DATA.getName(), notificationAsJson);

            if (Objects.nonNull(icon)) {
                jsonObject.put(NotificationFields.ICON.getName(), convertIconToJson(icon, locale));
            }

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the notifications into {@link JsonArray}
     * </p>
     *
     * @param notificationAlerts Represents the collection of {@link NotificationAlert}
     * @return the {@link JsonArray}
     */
    private JsonArray convertAlertsToJson(final long orgId, final Collection<NotificationAlert> notificationAlerts,
                                          final Locale locale) {
        final JsonArray jsonArray = Json.array();

        for (final NotificationAlert notificationAlert : notificationAlerts) {
            final JsonObject jsonObject = Json.object();
            final NotificationAlert.Category category = notificationAlert.getCategory();
            final JsonObject notificationAsJson = notificationAlert.getContent();
            final Icon icon = fetchIcon(orgId, category, notificationAsJson);

            jsonObject.put(NotificationFields.ID.getName(), notificationAlert.id());
            jsonObject.put(NotificationFields.TYPE.getName(), category);
            jsonObject.put(NotificationFields.TIMESTAMP.getName(), notificationAlert.getTimeStamp());
            jsonObject.put(NotificationFields.IS_READ.getName(), false);
            jsonObject.put(NotificationFields.DATA.getName(), notificationAsJson);

            if (Objects.nonNull(icon)) {
                jsonObject.put(NotificationFields.ICON.getName(), convertIconToJson(icon, locale));
            }

            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Fetches the icon based on the type and name
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param type         Represents the {@link AppNotificationEventType}
     * @param notification Represents the notification
     * @return the {@link Icon}
     */
    private Icon fetchIcon(final long orgId, final AppNotificationEventType type, final JsonObject notification) {

        return switch (type) {
            case CHAT_ASSIGNED, CHAT_INITIATED -> notification.containsKey(NotificationAttributes.PLATFORM) ?
                    iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE,
                            Platform.get(notification.getShort(NotificationAttributes.PLATFORM)).name().toLowerCase()) :
                    iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.CONVERSATION);

            default -> {
                final String iconName = switch (type) {
                    case CONTACT_ASSIGNED -> NotificationIconsKeys.CONTACT;
                    case COMPANY_ASSIGNED -> NotificationIconsKeys.COMPANY;
                    case DEAL_ASSIGNED -> NotificationIconsKeys.DEAL;
                    case PRODUCT_ASSIGNED, PRODUCTS_MODULE_ENABLED -> NotificationIconsKeys.PRODUCT;
                    case NEW_ACTIVITY, ACTIVITY_REMINDER -> {
                        final long activityId = notification.getLong(NotificationFields.ID.getName());
                        final Long iconId = appNotificationManager.getIconId(orgId, activityId);

                        if (Objects.isNull(iconId)) {
                            yield null;
                        }
                        yield iconService.getIconById(Icon.Type.ACTIVITY, iconId).getName();
                    }
                    case EMAIL_REPLIED, EMAIL_RECEIVED, EMAIL_OPENED, EMAIL_CLICKED -> NotificationIconsKeys.MAIL;
                    case COLLABORATOR_ADDED -> NotificationIconsKeys.COLLABORATOR;
                    case CONTACT_SYNCED -> NotificationIconsKeys.CONTACT_SYNCED;
                    case CALENDAR_SYNCED -> NotificationIconsKeys.CALENDAR_SYNCED;
                    case ADDED_TO_TEAM -> NotificationIconsKeys.TEAM;
                    case ROLE_CHANGED -> NotificationIconsKeys.KEY;
                    case CONTACT_NOTE_MENTIONED, COMPANY_NOTE_MENTIONED, DEAL_NOTE_MENTIONED,
                         ACTIVITY_PRIVATE_NOTE_MENTIONED, COMPANY_NOTE_COMMENT_MENTIONED,
                         CONTACT_NOTE_COMMENT_MENTIONED, DEAL_NOTE_COMMENT_MENTIONED -> NotificationIconsKeys.NOTE;
                    case EXPORTED, EXPORTING -> NotificationIconsKeys.EXPORT;
                    default -> null;
                };

                if (Objects.isNull(iconName)) {
                    yield iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationFields.ACTIVITY.getName());
                } else {
                    yield switch (type) {
                        case NEW_ACTIVITY, ACTIVITY_REMINDER -> iconService.fetchIconByTypeAndName(Icon.Type.ACTIVITY, iconName);
                        default -> iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, iconName);
                    };
                }
            }
        };

    }

    /**
     * <p>
     * Fetches the icon based on the type and name
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param notification Represents the notification
     * @return the {@link Icon}
     */
    private Icon fetchIcon(final long orgId, final NotificationAlert.Category category, final JsonObject notification) {

        return switch (category) {
            case ACTIVITY_REMINDER, NEW_ACTIVITY, COLLABORATOR_ADDED -> {
                final long activityId = notification.getLong(NotificationFields.ID.getName());
                final Long iconId = appNotificationManager.getIconId(orgId, activityId);

                if (Objects.isNull(iconId)) {
                    yield null;
                }

                yield iconService.fetchIconByTypeAndName(Icon.Type.ACTIVITY,
                        iconService.getIconById(Icon.Type.ACTIVITY, iconId).getName());
            }
            case CHAT_INITIATED, CHAT_ASSIGNED -> notification.containsKey(NotificationAttributes.PLATFORM) ?
                    iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE, Platform.get
                            (notification.getShort(NotificationAttributes.PLATFORM)).name().toLowerCase()) : null;
            case COMPANY_ASSIGNED -> iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.COMPANY);
            case CONTACT_ASSIGNED -> iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.CONTACT);
            case DEAL_ASSIGNED -> iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.DEAL);
            case PRODUCT_ASSIGNED -> iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.PRODUCT);
            case CONTACT_NOTE_MENTIONED, COMPANY_NOTE_MENTIONED, DEAL_NOTE_MENTIONED, ACTIVITY_PRIVATE_NOTE_MENTIONED,
                 COMPANY_NOTE_COMMENT_MENTIONED, CONTACT_NOTE_COMMENT_MENTIONED, DEAL_NOTE_COMMENT_MENTIONED ->
                    iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.NOTE);
            case EXPORTED -> iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, NotificationIconsKeys.EXPORT);
        };
    }

    /**
     * <p>
     * Converts the icon to json object
     * </p>
     *
     * @param icon   Represents the {@link Icon}
     * @param locale Represents the {@link Locale}
     * @return the {@link JsonObject}
     */
    private JsonObject convertIconToJson(final Icon icon, final Locale locale) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationIconsKeys.ID, icon.id());
        jsonObject.put(NotificationIconsKeys.NAME, icon.getName());
        jsonObject.put(NotificationIconsKeys.DISPLAY_NAME, i18nService.getResolver(locale).get(icon.getI18nName()));

        return jsonObject;
    }

    /**
     * <p>
     * Resets the pending notification count to zero for the given user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void resetPendingNotificationCount(final long orgId, final long userId, final CrmResponse crmResponse) {
        try {
            appNotificationManager.resetPendingNotificationCount(orgId, userId);
            crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.app.notification.update.success"));
        } catch (Exception e) {
            sendError(crmResponse, null, ErrorCode.get(NotificationErrorCodes.STATE_MODIFICATION_FAILED, crmResponse.resolveI18nString("i18n.app.notification.update.failed")));
        }
    }

    /**
     * <p>
     * Gets the reminder notifications for the user in given organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param locale Represents the {@link Locale}
     * @return the {@link JsonArray}
     */
    private JsonArray getAlerts(final long orgId, final long userId, final JsonObject data, final Locale locale, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(NotificationFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(NotificationFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long currentTime = Instant.now().toEpochMilli() - TimeUnit.MILLISECONDS.toMillis(500);
        final NotificationAlertPageHandler notificationAlertPageHandler = new NotificationAlertPageHandler(start, limit, currentTime, orgId, userId);
        final long totalRecords = notificationAlertPageHandler.totalRecordsCount();

        if (NumericalConstant.ZERO == totalRecords) {
            crmResponse.put(NotificationFields.TOTAL_RECORDS.getName(), NumericalConstant.ZERO);
            crmResponse.put(NotificationFields.HAS_MORE.getName(), false);
        } else {
            final Collection<NotificationAlert> notificationAlerts = notificationAlertPageHandler.fetchPageData().getData();

            crmResponse.put(NotificationFields.TOTAL_RECORDS.getName(), totalRecords);
            crmResponse.put(NotificationFields.HAS_MORE.getName(), notificationAlertPageHandler.hasMore());

            return convertAlertsToJson(orgId, notificationAlerts, locale);
        }
        return Json.array();
    }

    /**
     * <p>
     * Gets the reminder activities array.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return The reminder activities array.
     */
    private JsonArray getReminderActivities(final long orgId, final long userId, final Locale locale) {
        final Collection<NotificationAlert> activityReminderAlerts = notificationAlertManager.getActivityReminderAlerts(orgId, userId, DEFAULT_START, DEFAULT_ACTIVITY_REMINDER_LIMIT);
        final JsonArray activityReminderArray = Json.array();

        for (final NotificationAlert activityReminderAlert : activityReminderAlerts) {
            final JsonObject reminderJson = Json.object();
            final JsonObject content = activityReminderAlert.getContent();

            reminderJson.put(NotificationAttributes.ID, activityReminderAlert.id());
            reminderJson.put(NotificationAttributes.ACTIVITY_ID, content.opt(NotificationAttributes.ID));
            reminderJson.put(NotificationAttributes.ACTIVITY_TITLE, content.opt(NotificationAttributes.ACTIVITY_TITLE));
            reminderJson.put(NotificationAttributes.ACTIVITY_TYPE, content.opt(NotificationAttributes.ACTIVITY_TYPE));

            if (content.containsKey(NotificationAttributes.CONTACTS)) {
                reminderJson.put(NotificationAttributes.CONTACTS, content.getJsonArray(NotificationAttributes.CONTACTS));
            }
            reminderJson.put(NotificationAttributes.REMINDER, content.opt(NotificationAttributes.REMINDER));
            reminderJson.put(NotificationAttributes.CATEGORY, AppNotificationEventType.ACTIVITY_REMINDER.name());
            final Icon icon = fetchIcon(orgId, AppNotificationEventType.ACTIVITY_REMINDER, content);

            if (Objects.nonNull(icon)) {
                reminderJson.put(NotificationAttributes.ICON, convertIconToJson(icon, locale));
            }
            activityReminderArray.put(reminderJson);
        }
        return activityReminderArray;
    }
}