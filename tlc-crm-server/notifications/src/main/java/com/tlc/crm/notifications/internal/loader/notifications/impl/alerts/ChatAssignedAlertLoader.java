package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.model.ChatNotificationSettings;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.iam.common.models.User;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the notification of the chat assignment type
 *
 * @author Maharaja S
 * @version 1.0
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ChatAssignedAlertLoader extends AbstractAppNotificationAlertLoader<CrmChat> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatAssignedAlertLoader.class);

    private final UserService userService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;

    private ChatAssignedAlertLoader() {
        userService = UserService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
    }

    private static class Instance {
        private static final ChatAssignedAlertLoader INSTANCE = new ChatAssignedAlertLoader();
    }

    /**
     * <p>
     * Gets {@link ChatAssignedAlertLoader}.
     * </p>
     *
     * @return {@link ChatAssignedAlertLoader}.
     */
    public static ChatAssignedAlertLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets {@link AppNotificationEventType}.
     * </p>
     *
     * @return {@link AppNotificationEventType}.
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.CHAT_ASSIGNED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param crmChats Represents the {@link CrmChat}.
     */
    @Override
    public void load(final long orgId, final Collection<CrmChat> crmChats) {
        final Long userId = LoggedInUserInfo.getUser().id();
        final Collection<Long> chatOwnerIds = crmChats.stream().map(CrmChat::getOwnerId).filter(Objects::nonNull).toList();

        if (!chatOwnerIds.isEmpty() && !Objects.equals(userId, chatOwnerIds.stream().findFirst().get())) {
            fetchUserAndSaveAlert(orgId, userId, appNotificationConverter.convertChatsToJson(crmChats).getJsonArray(NotificationAttributes.CHATS));
        }
    }

    /**
     * <p>
     * Fetches the user and save notification
     * </p>
     *
     * @param orgId     Represents the org id
     * @param userId    Represents the user id
     * @param chatArray Represents the chat array
     */
    private void fetchUserAndSaveAlert(final Long orgId, final Long userId, final JsonArray chatArray) {
        final String notificationType = AppNotificationEventType.CHAT_ASSIGNED.name();
        final CrmUser crmUser = userService.getUser(orgId, userId);
        final Long iamUserId = crmUser.getIamId();
        final EventCallback<User> userFetchCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final User user) {
                try {
                    final int chatArraySize = chatArray.size();
                    final JsonObject userJson = Json.object();
                    final Optional<ChatNotificationSettings> optionalChatNotificationSettings =
                            notificationSettingsManager.retrieveChatNotificationSettings(orgId, userId);

                    userJson.put(NotificationAttributes.ID, userId);
                    userJson.put(NotificationAttributes.NAME, user.getFullName());

                    if (optionalChatNotificationSettings.isPresent() && optionalChatNotificationSettings.get().isChatAssigned()) {

                        for (int index = 0; index < chatArraySize; index++) {
                            final JsonObject chatJson = chatArray.getJsonObject(index);

                            chatJson.put(NotificationAttributes.USER, userJson);
                            notificationAlertManager.storeChatAlert(notificationAlertConverter.convertChatAlert(orgId,
                                    List.of(chatJson.getLong(NotificationAttributes.OWNER_ID)),
                                    NotificationAlert.Category.CHAT_ASSIGNED, chatJson));
                        }
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Notification: Failed to save the notification. Reason:{}, Org Id:{}, User Id:{}, Notification type:{}", throwable.getMessage(), orgId, userId, notificationType);
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                LOGGER.error("Notification: Failed to fetch the user. Reason:{}, Org Id:{}, User Id:{}, Notification type:{}", cause.getMessage(), orgId, userId, notificationType);
            }
        };

        iamUserService.fetchUser(organizationService.getIamOrgId(orgId), iamUserId, userFetchCallback);
    }
}