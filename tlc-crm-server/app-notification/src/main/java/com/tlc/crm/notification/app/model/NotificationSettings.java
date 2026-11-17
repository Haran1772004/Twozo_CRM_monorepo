package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * A common abstract base class for various notification configurations.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public abstract class NotificationSettings {

    protected String jsonString;

    /**
     * <p>
     * Converts the configuration properties into a JSON object
     * </p>
     *
     * @return A JSON representation of the configuration settings
     */
    public abstract JsonObject configAsJson();

    /**
     * <p>
     * Converts the configuration properties into a JSON string representation.
     * </p>
     *
     * @return A string representing the configuration in the specified format.
     */
    public abstract String getConfigAsJsonString();
}
