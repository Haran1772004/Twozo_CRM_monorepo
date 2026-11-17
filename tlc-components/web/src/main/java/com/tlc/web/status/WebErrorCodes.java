package com.tlc.web.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of web module.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum WebErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_HTTP_METHOD(0x01),
    CONNECTION_NOT_AVAILABLE(0x02),
    INVALID_CONNECTION_RESPONSE(0x30),
    UNSUPPORTED_FILE_EXTENSION(0x31),
    FILE_CONVERSION_FAILED(0x32),
    ENTITY_TO_BYTE_CONVERSION_FAILED(0x33),
    UNSUPPORTED_ACTION(0x34),
    INVALID_TOKEN_ISSUER(0X35),
    SERVICE_NOT_INITIALIZED(0x36),
    CSRF_SESSION_ALREADY_EXISTS(0x37),
    INVALID_TOKEN_SOURCE(0x38),
    HTTP_REQUEST_FAILED(0x39),
    VERSIONED_ACTION_NOT_FOUND(0x40),
    DEFAULT_VERSIONED_ACTION_NOT_FOUND(0x41),
    ;

    private final int code;

    WebErrorCodes(final int localCode) {
        this.code = WebErrorCodes.WebErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Provides the error code group of web module.
     * </p>
     */
    private static class WebErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new WebErrorCodes.WebErrorCodeGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x200_0_0001;
        }
    }
}
