package com.tlc.crm.security.permission.validator;

import com.tlc.crm.security.permission.validator.request.LimitValidatorRequest;

/**
 * <p>
 * Checker for checking the limitations of the given requested action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface LimitValidator {

    /**
     * <p>
     * Checks whether the requested action limit has exceeded or not.
     * </p>
     *
     * @param validatorRequest Represents the {@link LimitValidatorRequest}.
     * @return True, if the action limit has not exceeded.
     */
    boolean validate(final LimitValidatorRequest validatorRequest);
}
