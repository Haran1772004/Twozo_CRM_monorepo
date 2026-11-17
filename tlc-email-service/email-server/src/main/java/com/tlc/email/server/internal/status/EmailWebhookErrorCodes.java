package com.tlc.email.server.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error webhook codes related to mail.
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum EmailWebhookErrorCodes implements ErrorCodeProvider {

    SUBSCRIPTION_ACTION_FAILED(0x01),
    PRIVATE_KEY_NOT_FOUND(0x02),
    FAILED_TO_GENERATE_SIGNATURE(0x03),
    FAILED_TO_DECRYPT_DATA_KEY(0x04),
    FAILED_TO_LOAD_PRIVATE_KEY(0x05),
    DECRYPT_ACTION_FAILED(0x06),
    SIGNATURE_VERIFICATION_FAILED(0x07),
    FAILED_TO_DECRYPT_CONTENT(0x08),
    ;

    private final int code;

    EmailWebhookErrorCodes(final int localCode) {
        this.code = EmailWebhookErrorCodeGroup.GROUP.getConvertedCode(localCode);
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
     * Represents the email webhook error code group.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class EmailWebhookErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new EmailWebhookErrorCodeGroup();

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @return prefix
         */
        @Override
        public int getPrefix() {
            return 0x52_0_0001;
        }
    }
}
