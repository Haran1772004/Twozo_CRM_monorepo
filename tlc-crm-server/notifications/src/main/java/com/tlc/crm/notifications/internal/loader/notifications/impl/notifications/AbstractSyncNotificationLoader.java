package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.Json;
import com.tlc.crm.entity.models.settings.SyncSettings;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Loads sync settings notification into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractSyncNotificationLoader<T extends SyncSettings> extends AbstractNotificationLoader<T> {

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id.
     * @param models Represents sync settings models.
     */
    @Override
    public void load(final long orgId, final Collection<T> models) {
        final Collection<SyncSettings> syncSettings = (Collection<SyncSettings>) models;
        final SyncSettings settings = syncSettings.stream().findFirst().get();
        final long userId = settings.getUserId();

        saveNotification(orgId, getSender(orgId, userId), getReceivers(orgId, List.of(userId)),
                Json.object().put(NotificationAttributes.PROVIDER, settings.getProvider()));
    }
}
