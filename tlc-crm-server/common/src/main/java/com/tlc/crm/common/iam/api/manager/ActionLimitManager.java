package com.tlc.crm.common.iam.api.manager;

import com.tlc.crm.common.iam.internal.manager.impl.ActionLimitManagerImpl;

import com.tlc.crm.security.permission.model.ActionLimitDetails;
import com.tlc.crm.security.permission.model.LimitActionType;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the action limit count related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ActionLimitManager {

    static ActionLimitManager getInstance() {
        return ActionLimitManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the remaining action limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     * @return Remaining action count of the given {@link LimitActionType}.
     */
    Optional<Long> getRemainingActionLimit(final ActionLimitDetails actionLimitDetails);

    /**
     * <p>
     * Fetches the currently used limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     * @return Currently used count of the given {@link LimitActionType}.
     */
    long getCurrentlyUsedLimit(final ActionLimitDetails actionLimitDetails);

    /**
     * <p>
     * Stores the remaining action count of the given {@link LimitActionType}.
     * </p>
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    void store(final ActionLimitDetails actionLimitDetails);

    /**
     * <p>
     * Updates the remaining action count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    void updateRemainingActionLimit(final long orgId, final Collection<ActionLimitDetails> actionLimitDetails);

    /**
     * <p>
     * Updates the currently used action limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    void updateCurrentlyUsedLimit(final long orgId, final Collection<ActionLimitDetails> actionLimitDetails);

    /**
     * <p>
     * Removes the remaining action count of the given {@link LimitActionType}.
     * </p>
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    void remove(final ActionLimitDetails actionLimitDetails);

    /**
     * <p>
     * Fetches the count of the expired action limit count.
     * </p>
     *
     * @return Expired action limits count.
     */
    long getExpiredActionLimitsCount();

    /**
     * <p>
     * Fetches the expired action limit IDs.
     * </p>
     *
     * @param maxNumberOfRecords Represents the count which refers maximum number of records to be fetched.
     * @return Expired action limit count IDs.
     */
    Collection<Long> getExpiredActionLimitIds(final int maxNumberOfRecords);

    /**
     * <p>
     * Deletes the action limit history using IDs.
     * </p>
     *
     * @param actionLimitIds Action limit IDs.
     */
    void delete(final Collection<Long> actionLimitIds);
}