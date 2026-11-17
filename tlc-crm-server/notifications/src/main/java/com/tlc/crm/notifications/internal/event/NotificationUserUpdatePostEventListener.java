package com.tlc.crm.notifications.internal.event;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.CommonEvent;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notifications.internal.event.handler.TelephoneUserUpdateHandler;
import com.tlc.crm.notifications.internal.event.handler.TelephoneUserUpdateHandlerRegistry;
import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles post-process actions for user update operations by delegating to
 * registered {@link TelephoneUserUpdateHandler} implementations obtained from
 * {@link TelephoneUserUpdateHandlerRegistry}. Each telephony provider's logic is
 * encapsulated in its own handler, allowing new providers to be added without
 * modifying this class.
 *
 * @author Kishore J
 * @version 1.0
 */
public class NotificationUserUpdatePostEventListener implements PostEventListener {

    private final TelephoneUserUpdateHandlerRegistry handlerRegistry;

    private NotificationUserUpdatePostEventListener() {
        handlerRegistry = TelephoneUserUpdateHandlerRegistry.getInstance();
    }

    private static class Instance {
        private static final NotificationUserUpdatePostEventListener INSTANCE = new NotificationUserUpdatePostEventListener();
    }

    /**
     * Returns the singleton instance of {@link NotificationUserUpdatePostEventListener}.
     *
     * @return the singleton instance
     */
    public static NotificationUserUpdatePostEventListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final long orgId = eventData.getOrgId();
        final Collection<TlcModel> models = eventData.getValues();
        final Map<Long, TlcModel> previousValues = eventData.getPreviousValues();
        final int size = models.size();
        final Collection<CrmUser> crmUsers = new ArrayList<>(size);
        final Set<Long> deactivatedUserIds = new HashSet<>(size);
        final Collection<CrmUser> previousUsers = new ArrayList<>(size);

        if (Objects.nonNull(previousValues)) {
            for (final TlcModel previousValue : previousValues.values()) {

                if (previousValue instanceof final CrmUser previousCrmUser) {
                    previousUsers.add(previousCrmUser);
                }
            }
        }

        for (final TlcModel model : models) {

            if (model instanceof final CrmUser crmUser) {
                crmUsers.add(crmUser);

                if (Objects.equals(crmUser.getState(), User.State.DEACTIVATED)) {
                    deactivatedUserIds.add(crmUser.id());
                }
            }
        }

        for (final TelephoneUserUpdateHandler handler : handlerRegistry.getHandlers()) {

            if (!deactivatedUserIds.isEmpty()) {
                handler.handleDeactivatedUsers(orgId, deactivatedUserIds);
            }
            handler.handleUserUpdate(orgId, crmUsers, previousUsers);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event event() {
        return CommonEvent.USER_UPDATE;
    }
}