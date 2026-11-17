package com.tlc.crm.security.permission.controller;

import com.tlc.crm.security.permission.controller.request.LimitControllerRequest;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Checker for checking the limitations of the given requested action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface LimitController<T extends LimitControllerRequest> {

    /**
     * <p>
     * Checks whether the requested action limit has exceeded or not and allows only the limited entries.
     * </p>
     *
     * @param checkerRequest Represents the {@link LimitControllerRequest}.
     * @return True, if the action limit has not exceeded.
     */
    boolean canProceed(final T checkerRequest);

    /**
     * <p>
     * Clears the history of the limit checking activity.
     * </p>
     *
     * @param limitActionType Represents the type of the limit action.
     */
    void clearLimitHistory(final LimitActionType limitActionType);
}
