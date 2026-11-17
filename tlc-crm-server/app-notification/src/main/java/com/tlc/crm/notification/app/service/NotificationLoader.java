package com.tlc.crm.notification.app.service;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * Provides the services for the notification
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface NotificationLoader {

    /**
     * <p>
     * Loads the notification based on the types
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonObject}
     */
    void load(final long orgId, final JsonObject jsonObject);
}
