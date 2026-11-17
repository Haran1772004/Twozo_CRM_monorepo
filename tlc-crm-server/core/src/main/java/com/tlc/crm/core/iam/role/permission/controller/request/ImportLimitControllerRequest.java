package com.tlc.crm.core.iam.role.permission.controller.request;

import com.tlc.crm.core.iam.role.permission.controller.ImportLimitController;

import com.tlc.crm.security.permission.controller.request.LimitControllerRequest;

/**
 * <p>
 * Holds the request data of the {@link ImportLimitController}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ImportLimitControllerRequest extends LimitControllerRequest {

    private final boolean isCreateEvent;

    public ImportLimitControllerRequest(final boolean isCreateEvent) {
        this.isCreateEvent = isCreateEvent;
    }

    public boolean isCreateEvent() {
        return isCreateEvent;
    }
}
