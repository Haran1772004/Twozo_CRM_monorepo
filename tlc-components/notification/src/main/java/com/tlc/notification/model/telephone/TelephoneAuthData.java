package com.tlc.notification.model.telephone;

import com.tlc.commons.json.JsonObject;
import com.tlc.validator.TlcModel;

/**
 * Represents the authentication details for a telephone
 *
 * @author Sathish
 * @version 1.0
 */
public interface TelephoneAuthData extends TlcModel, TelephoneModel {

    /**
     * Serializes the authentication credentials into a JSON structure for storage.
     *
     * @return a JSON object with the authentication data nested under the "authenticate" key
     */
    JsonObject toJson();
}