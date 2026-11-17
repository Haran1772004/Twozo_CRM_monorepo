package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.validator.TlcModel;

/**
 * Result of executing a workflow V2 action node.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 */
public final class ActionOutcome {

    public enum OutcomeType {

        SYNC_SUCCESS,
        SYNC_FAILURE,
        ASYNC_PENDING
    }

    private final OutcomeType type;
    private final TlcModel model;

    private ActionOutcome(final OutcomeType type, final TlcModel model) {
        this.type = type;
        this.model = model;
    }

    public static ActionOutcome syncSuccess(final TlcModel model) {
        return new ActionOutcome(OutcomeType.SYNC_SUCCESS, model);
    }

    public static ActionOutcome syncFailure() {
        return new ActionOutcome(OutcomeType.SYNC_FAILURE, null);
    }

    public static ActionOutcome asyncPending() {
        return new ActionOutcome(OutcomeType.ASYNC_PENDING, null);
    }

    public OutcomeType type() {
        return type;
    }

    public TlcModel model() {
        return model;
    }

    public boolean isSyncSuccess() {
        return OutcomeType.SYNC_SUCCESS == type;
    }

    public boolean isSyncFailure() {
        return OutcomeType.SYNC_FAILURE == type;
    }

    public boolean isAsyncPending() {
        return OutcomeType.ASYNC_PENDING == type;
    }

}
