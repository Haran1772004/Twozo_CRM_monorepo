package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Loads email notification into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractEmailNotificationLoader extends AbstractNotificationLoader<EntityEmail>
        implements SecureUserTaskProcessor {

    /**
     * <p>
     * Process email notification json.
     * </p>
     *
     * @param orgId     Represents an organization id.
     * @param emailJson {@link JsonObject}.
     */
    protected abstract void processEmailNotificationJson(final long orgId, final JsonObject emailJson);

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id.
     * @param emails {@link EntityEmail} collection.
     */
    @Override
    public void load(final long orgId, Collection<EntityEmail> emails) {
        final JsonArray emailArray = appNotificationConverter.convertEmailsToJson(emails);
        final int size = emails.size();

        for (int index = 0; index < size; index++) {
            final JsonObject json = emailArray.getJsonObject(index);
            final long userId = json.getLong(NotificationAttributes.USER_ID);

            processEmailNotificationJson(orgId, json);
            secureProcess(orgId, userId, () -> saveNotification(orgId, getSender(orgId, userId),
                    getReceivers(orgId, List.of(userId)), json));
        }
    }
}
