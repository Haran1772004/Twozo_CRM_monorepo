package com.tlc.scheduler.internal.resource;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Manages the error codes associated with the scheduler module
 *
 * @author sundar
 * @version 1.0
 */
public enum SchedulerErrorCodes implements ErrorCodeProvider {

    INVALID_SCHEDULER_POOL(0X0001),
    INVALID_ROUND_ROBIN_LIMIT_TYPE(0X0002),
    ;

    private final int code;

    SchedulerErrorCodes(int localCode) {
        this.code = SchedulerErrorCodes.SchedulerErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class SchedulerErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SchedulerErrorCodes.SchedulerErrorCodesGroup();

        /**
         * {@inheritDoc}
         *
         * @return
         */
        @Override
        public int getPrefix() {
            return 0x10_0_0004;
        }
    }
}