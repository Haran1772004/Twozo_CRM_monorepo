package com.tlc.crm.core.notification.email.role.permission.controller.request;

import com.tlc.crm.core.iam.role.permission.controller.ImportLimitController;
import com.tlc.crm.security.permission.controller.request.LimitControllerRequest;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Holds the request data of the {@link ImportLimitController}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EmailLimitControllerRequest extends LimitControllerRequest {

    private final LimitActionType limitActionType;

    public EmailLimitControllerRequest(final LimitActionType limitActionType) {
        this.limitActionType = limitActionType;
    }

    public LimitActionType getLimitActionType() {
        return limitActionType;
    }
}
