package com.tlc.sync.service.oauth.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of sync service
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum SyncErrorCodes implements ErrorCodeProvider {

    JNDI_LOOKUP_FAILED(0x01),
    INVALID_PROVIDER(0x02),
    INVALID_REFRESH_TOKEN(0x03),
    INVALID_EMAIL_PROVIDER(0x04),
    INVALID_ADDRESS(0x05),
    INVALID_ACCESS_TOKEN(0X06),
    BAD_REQUEST_ERROR(0x07),
    INVALID_USER(0x08),
    EMAIL_ADDRESS_FETCH_FAILED(0x09),
    INVALID_SERVICE_TYPE(0x10),
    SYNC_SERVICE_NOT_INITIALIZED(0x11),
    CLIENT_CREDENTIAL_TOKEN_FETCH_FAILED(0X12),
    INVALID_TOKEN_TYPE(0x13),
    UNSUPPORTED_ACTION(0x14),
    SYNC_SERVICE_NOT_SUPPORTED(0x15),
    INSUFFICIENT_PERMISSIONS(0x16),
    AUTHORIZATION_FAILED(0x17),
    ;
    private final int code;

    SyncErrorCodes(final int localCode) {
        this.code = SyncErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represents the {@link SyncErrorCodesGroup}.
     * </P>
     *
     * @author NandiniRakAS
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class SyncErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SyncErrorCodesGroup();

        /**
         * {@inheritDoc}
         *
         * @return prefix
         */
        @Override
        public int getPrefix() {
            return 0x70_0_0001;
        }
    }
}
