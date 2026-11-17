package com.tlc.crm.security.permission.handler;

import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.handler.request.ApiHandlerRequest;

/**
 * <p>
 * Handles the {@link RolePermission} of the requested APIs.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ApiPermissionHandler<T extends ApiHandlerRequest> {

    /**
     * <p>
     * Handles the {@link RolePermission} of the requested API.
     * </p>
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    void handle(final T apiHandlerRequest);
}
