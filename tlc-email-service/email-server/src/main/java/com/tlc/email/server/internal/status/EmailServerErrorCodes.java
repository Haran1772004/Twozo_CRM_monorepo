package com.tlc.email.server.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to mail.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum EmailServerErrorCodes implements ErrorCodeProvider {

    AUTO_SYNC_ACTION_FAILED(0x01),
    PUBLIC_KEY_NOT_FOUND(0x02),
    PUBLIC_KEY_ENCRYPTION_FAILED(0x03),
    ;

    private final int code;

    EmailServerErrorCodes(final int localCode) {
        this.code = EmailErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represents the email error code group.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class EmailErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new EmailErrorCodeGroup();

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @return prefix
         */
        @Override
        public int getPrefix() {
            return 0x12_0_0001;
        }
    }
}
