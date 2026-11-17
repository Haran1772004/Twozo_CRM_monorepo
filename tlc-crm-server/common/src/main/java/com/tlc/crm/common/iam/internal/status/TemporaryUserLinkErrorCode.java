package com.tlc.crm.common.iam.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents the temporary user link error code.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum TemporaryUserLinkErrorCode implements ErrorCodeProvider {

    INVALID_CATEGORY(0x01),
    INVALID_EXPIRY_LINK(0x02),
    PASSWORD_MISMATCH(0x03),
    USER_EXISTS_CHECK_FAILED(0x04),
    USER_DETAIL_RETRIEVE_FAILED(0x05),
    FORGOT_PASSWORD_LINK_EXPIRED(0x06),
    FORGOT_PASSWORD_ACTION_FAILED(0x07),
    ;

    private final int code;

    TemporaryUserLinkErrorCode(final int localCode) {
        this.code = TemporaryUserLinkErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return code Represents the code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represents the temporary user link codes group.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class TemporaryUserLinkErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TemporaryUserLinkErrorCodesGroup();

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @return the prefix
         */
        @Override
        public int getPrefix() {
            return 0x10_0_00010;
        }
    }
}
