package com.tlc.crm.notifications.internal.loader.notifications;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoService;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Loads alerts into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface AppNotificationAlertLoader<T extends TlcModel> extends AutoService<AppNotificationEventType> {

    /**
     * <p>
     * Loads the notification based on the types
     * </p>    *
     * @param orgId  Represents an organization id
     * @param models Represents the {@link JsonObject}
     */
    void load(final long orgId, final Collection<T> models);
}