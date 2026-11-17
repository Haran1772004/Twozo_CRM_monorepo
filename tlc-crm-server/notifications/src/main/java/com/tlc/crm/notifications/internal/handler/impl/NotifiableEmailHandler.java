package com.tlc.crm.notifications.internal.handler.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.model.TrackingNotificationSettings;
import com.tlc.crm.notifications.internal.handler.registry.NotifiableModelHandlerRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Handles {@link EntityEmail} for notification/alert purposes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotifiableModelHandlerRegistry.class)
public final class NotifiableEmailHandler extends AbstractNotifiableModelHandler<EntityEmail> {

    private NotifiableEmailHandler() {
    }

    private static class Instance {
        private static final NotifiableEmailHandler INSTANCE = new NotifiableEmailHandler();
    }

    /**
     * <p>
     * Gets {@link NotifiableEmailHandler}.
     * </p>
     *
     * @return {@link NotifiableEmailHandler}.
     */
    public static NotifiableEmailHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * @param orgId  Represents an organization id.
     * @param emails Collection {@link EntityEmail}.
     */
    @Override
    public void handle(final Long orgId, final Collection<EntityEmail> emails) {
        final Collection<AppNotificationEventType> notificationEventTypes = new ArrayList<>(Arrays.asList(
                EntityEmail.class.getAnnotation(Notifiable.class).types()));
        final Optional<EntityEmail> optionalEntityEmail = emails.stream().findFirst();

        if (optionalEntityEmail.isPresent()) {
            final EntityEmail email = optionalEntityEmail.get();
            final Long userId = email.getUserId();
            final String inReplyTo = email.getMetaData().getInReplyTo();
            final boolean isNotReply = Objects.isNull(inReplyTo);

            if (Objects.nonNull(userId)) {
                final Optional<TrackingNotificationSettings> settings =
                        notificationSettingsManager.retrieveTrackingNotificationSettings(orgId, userId);

                if (settings.isPresent() && !settings.get().isRepliedEmail() || isNotReply) {
                    notificationEventTypes.remove(AppNotificationEventType.EMAIL_REPLIED);
                }

                if (!isNotReply) {
                    notificationEventTypes.remove(AppNotificationEventType.EMAIL_RECEIVED);
                }
            }
            load(orgId, emails, notificationEventTypes);
        }
    }

    /**
     * <p>
     * Gets {@link CrmModelType}.
     * </p>
     *
     * @return {@link CrmModelType}.
     */
    @Override
    public CrmModelType getType() {
        return CrmModelType.EMAIL;
    }
}