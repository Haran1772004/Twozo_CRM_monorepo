package com.tlc.crm.security.permission.model;

import com.tlc.commons.json.JsonObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * Represents the details of the registered API.
 * </p>
 *
 * @param type           Represents the API type.
 * @param registeredPath Represents the registered path of the API.
 *
 * @author Sathish
 * @version 1.0
 */
public record ApiDetails(String type, String registeredPath) implements Serializable {

    public ApiDetails(final JsonObject apiDetailsJson) {
        this(apiDetailsJson.getString(RequisitesAttributes.TYPE.getName()), apiDetailsJson.getString(RequisitesAttributes.REGISTERED_PATH.getName()));
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final ApiDetails apiDetails = (ApiDetails) object;

        return Objects.equals(type, apiDetails.type) && Objects.equals(registeredPath, apiDetails.registeredPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, registeredPath);
    }
}
