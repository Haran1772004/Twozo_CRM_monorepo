package com.tlc.crm.core.notification.email.role.permission.validator;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.entity.role.permission.validator.AbstractLimitValidator;

import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Checker for checking the limitations of the bulk email send action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@ActionLimitValidator(type = LimitActionType.BULK_EMAIL)
public class BulkEmailSendLimitValidator extends AbstractLimitValidator {

    private final CoreMailManager coreMailManager;

    private static final class Instance {
        private static final BulkEmailSendLimitValidator INSTANCE = new BulkEmailSendLimitValidator();
    }

    private BulkEmailSendLimitValidator() {
        coreMailManager = CoreMailManager.getInstance();
    }

    @InstanceProvider
    public static BulkEmailSendLimitValidator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organisation id.
     * @param userId Represents an user id.
     * @return Used count of the action.
     */
    @Override
    protected long getActionCount(long orgId, long userId) {
        return coreMailManager.getOutboundMessageCountOnDay(orgId, userId, MessageDispatchType.BULK);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link LimitActionType}.
     */
    @Override
    protected LimitActionType getLimitActionType() {
        return LimitActionType.BULK_EMAIL;
    }
}
