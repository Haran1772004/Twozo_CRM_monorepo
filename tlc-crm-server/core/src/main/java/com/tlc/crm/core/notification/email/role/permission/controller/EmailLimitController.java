package com.tlc.crm.core.notification.email.role.permission.controller;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.role.permission.controller.request.EmailLimitControllerRequest;

import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.entity.role.permission.controller.AbstractLimitController;

import com.tlc.crm.security.permission.controller.request.LimitControllerRequest;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Checker for checking the limitations of create the records via import.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EmailLimitController extends AbstractLimitController<EmailLimitControllerRequest> {

    private final CoreMailManager coreMailManager;

    public EmailLimitController() {
        coreMailManager = CoreMailManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param checkerRequest Represents the {@link LimitControllerRequest}.
     * @return True, if the action limit has not exceeded.
     */
    @Override
    public boolean canProceed(final EmailLimitControllerRequest checkerRequest) {
        return canProceed(checkerRequest.getLimitActionType());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents an organisation id.
     * @param userId              Represents an user id.
     * @param limitActionType Represents the {@link LimitActionType}.
     * @return Used count of the action.
     */
    @Override
    protected long getActionCount(final long orgId, final long userId, final LimitActionType limitActionType) {
        return switch (limitActionType) {
            case INDIVIDUAL_EMAIL -> coreMailManager.getOutboundMessageCountOnDay(orgId, userId, MessageDispatchType.SINGLE);
            case BULK_EMAIL -> coreMailManager.getOutboundMessageCountOnDay(orgId, userId, MessageDispatchType.BULK);
            default -> 0;
        };
    }
}
