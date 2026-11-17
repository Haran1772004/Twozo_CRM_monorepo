package com.tlc.notification.model.telephone.telecmi;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.notification.internal.fields.TelephoneFields;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.TelephoneAuthData;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the TeleCMI-specific authentication details for a telephone
 *
 * @author kishore J
 * @version 1.0
 */
public class TeleCMIAuthData implements TelephoneAuthData {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.telecmi.auth.invalid.app.id")
    private final Long appId;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.telecmi.auth.invalid.secret")
    private final String secret;

    public TeleCMIAuthData(final Long appId, final String secret) {
        this.appId = appId;
        this.secret = secret;
    }

    public TeleCMIAuthData(final JsonObject authenticationJson) {
        this(authenticationJson.getLong(TelephoneFields.APP_ID), authenticationJson.getString(TelephoneFields.SECRET));
    }

    public Long getAppId() {
        return appId;
    }

    public String getSecret() {
        return secret;
    }

    /**
     * Serializes the authentication credentials into a JSON structure for storage.
     *
     * @return a JSON object with the authentication data nested under the "authenticate" key
     */
    @Override
    public JsonObject toJson() {
        return Json.object().put(TelephoneFields.AUTHENTICATE,
                Json.object().put(TelephoneFields.APP_ID, appId).put(TelephoneFields.SECRET, secret));
    }

    @Override
    public Long id() {
        return appId;
    }

    @Override
    public Long orgId() {
        throw ErrorCode.get(NotificationErrorCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(NotificationErrorCodes.UNSUPPORTED_OPERATION);
    }
}