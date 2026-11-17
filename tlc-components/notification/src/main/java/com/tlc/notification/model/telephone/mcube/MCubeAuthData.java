package com.tlc.notification.model.telephone.mcube;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.notification.internal.fields.TelephoneFields;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.TelephoneAuthData;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the MCube-specific authentication details for telephony.
 * MCube uses a secret key and an auth token for API access.
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeAuthData implements TelephoneAuthData {

    /**
     * MCube authorization token passed in the HTTP_AUTHORIZATION header.
     */
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.mcube.auth.invalid.auth.token")
    private final String authToken;

    /**
     * Constructs an {@link MCubeAuthData} with the given credentials.
     *
     * @param authToken the MCube authorization token
     */
    public MCubeAuthData(final String authToken) {
        this.authToken = authToken;
    }

    /**
     * Constructs authentication data from a JSON object.
     *
     * @param authenticationJson JSON containing secretKey and authToken fields
     */
    public MCubeAuthData(final JsonObject authenticationJson) {
        this(authenticationJson.getString(TelephoneFields.AUTH_TOKEN));
    }

    public String getAuthToken() {
        return authToken;
    }

    @Override
    public JsonObject toJson() {
        return Json.object().put(TelephoneFields.AUTHENTICATE,
                Json.object().put(TelephoneFields.AUTH_TOKEN, getAuthToken()));
    }

    @Override
    public Long id() {
        throw ErrorCode.get(NotificationErrorCodes.UNSUPPORTED_OPERATION);
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
