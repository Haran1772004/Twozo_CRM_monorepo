package com.tlc.crm.core.notification.email.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

/**
 * <p>
 * Represents the supported delete modes for email thread actions.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public enum DeleteMode {

    SOFT,
    HARD,
    ;

    /**
     * <p>
     * Resolves the delete mode from the incoming request data.
     * </p>
     *
     * @param data Represents the request {@link JsonObject}
     * @return The resolved {@link DeleteMode}
     */
    public static DeleteMode get(final JsonObject data) {
        return data.containsKey(CoreEmailFields.IS_SOFT_DELETE.getName()) ? SOFT : HARD;
    }
}