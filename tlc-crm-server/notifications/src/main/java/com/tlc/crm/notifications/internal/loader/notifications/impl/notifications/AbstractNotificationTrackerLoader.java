package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.model.NotificationTrackingDetails;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Loads tracker notification into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractNotificationTrackerLoader extends AbstractNotificationLoader<NotificationTrackingDetails>
        implements SecureUserTaskProcessor {

    /**
     * <p>
     * Whether {@link EmailTrackingDetail.Type} is of child's type.
     * </p>
     *
     * @param type {@link EmailTrackingDetail.Type}.
     * @return Whether {@link EmailTrackingDetail.Type} is of child's type.
     */
    protected abstract boolean isOfSameType(final EmailTrackingDetail.Type type);

    /**
     * {@inheritDoc}
     *
     * @param orgId                       Represents an organization id.
     * @param notificationTrackingDetails Represents the {@link JsonObject}.
     */
    @Override
    public void load(final long orgId, final Collection<NotificationTrackingDetails> notificationTrackingDetails) {
        final NotificationTrackingDetails trackingDetails = notificationTrackingDetails.stream().findFirst().get();

        if (isOfSameType(trackingDetails.getType())) {
            final long userId = trackingDetails.getUserId();

            secureProcess(orgId, userId, () -> {
                saveNotification(orgId, getSender(orgId, userId), getReceivers(orgId, List.of(userId)),
                        appNotificationConverter.convertTrackingDetailsToJson(notificationTrackingDetails));
            });
        }
    }
}