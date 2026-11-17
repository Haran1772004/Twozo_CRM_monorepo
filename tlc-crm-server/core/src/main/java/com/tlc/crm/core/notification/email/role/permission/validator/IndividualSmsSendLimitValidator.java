package com.tlc.crm.core.notification.email.role.permission.validator;

import com.tlc.crm.entity.role.permission.validator.AbstractLimitValidator;
import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Checker for checking the limitations of the individual sms send action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@ActionLimitValidator(type = LimitActionType.INDIVIDUAL_SMS)
public class IndividualSmsSendLimitValidator extends AbstractLimitValidator {

    private static final class Instance {
        private static final IndividualSmsSendLimitValidator INSTANCE = new IndividualSmsSendLimitValidator();
    }

    private IndividualSmsSendLimitValidator() {
    }

    @InstanceProvider
    public static IndividualSmsSendLimitValidator getInstance() {
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
        return LimitActionType.INDIVIDUAL_SMS;
    }
}
