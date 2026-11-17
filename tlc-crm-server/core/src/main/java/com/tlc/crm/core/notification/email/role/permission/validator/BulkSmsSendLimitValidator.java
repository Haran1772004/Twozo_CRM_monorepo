package com.tlc.crm.core.notification.email.role.permission.validator;

import com.tlc.crm.entity.role.permission.validator.AbstractLimitValidator;
import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Checker for checking the limitations of the bulk sms send action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@ActionLimitValidator(type = LimitActionType.BULK_SMS)
public class BulkSmsSendLimitValidator extends AbstractLimitValidator {

    private static final class Instance {
        private static final BulkSmsSendLimitValidator INSTANCE = new BulkSmsSendLimitValidator();
    }

    private BulkSmsSendLimitValidator() {
    }

    @InstanceProvider
    public static BulkSmsSendLimitValidator getInstance() {
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
    protected long getActionCount(final long orgId, final long userId) {
        return 0;//TODO Define this logic while implementing the SMS feature.
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link LimitActionType}.
     */
    @Override
    protected LimitActionType getLimitActionType() {
        return LimitActionType.BULK_SMS;
    }
}
