package com.tlc.crm.entity.role.permission.handler.request;

import com.tlc.crm.entity.role.permission.handler.CommonApiPermissionHandler;
import com.tlc.crm.security.permission.handler.request.ApiHandlerRequest;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

/**
 * <p>
 * Holds the request data of the {@link CommonApiPermissionHandler}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CommonApiHandlerRequest extends ApiHandlerRequest {

    private final Category.Type categoryType;
    private final Action.Type actionType;

    public CommonApiHandlerRequest(final Category.Type categoryType, final Action.Type actionType,
                                   final VerificationRequest<?> request, final VerificationResponse response) {
        super(request, response);
        this.categoryType = categoryType;
        this.actionType = actionType;
    }

    public Category.Type getCategoryType() {
        return categoryType;
    }

    public Action.Type getActionType() {
        return actionType;
    }
}
