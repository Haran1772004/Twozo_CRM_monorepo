package com.tlc.crm.common.iam.api.permissions.model.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.permissions.model.converter.config.ConverterConfig;
import com.tlc.crm.security.permission.model.Permission;
import com.tlc.crm.security.permission.model.Permissions;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Permissions}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionConverter<U extends Permission, T extends Permissions<U>> {

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Permissions}
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param permissionJson Which contains the request data
     * @return {@link Permissions} filled with the data
     */
    T convert(final Long orgId, final JsonObject permissionJson);

    /**
     * <p>
     * Converts the permissions into {@link JsonObject}.
     * </p>
     *
     * @param permissions     Represents the permissions attributes.
     * @param converterConfig Represents the configurations which needs for converting the permissions.
     * @return Converted {@link JsonObject}.
     */
    JsonObject convert(final T permissions, final ConverterConfig converterConfig);
}
