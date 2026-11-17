package com.tlc.web.client.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Manages the error codes associated with the web client module
 *
 * @author Selvakumar
 * @version 1.0
 */
public enum WebClientErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_HTTP_METHOD(0x01),
    OBJECT_TO_BYTES_ARRAY_CONVERSION_FAILED(0x02),
    VERTX_INITIALIZATION_FAILED(0x03),
    OBJECT_SERIALIZATION_FAILED(0x04),
    OBJECT_DESERIALIZATION_FAILED(0x05),
    VERTX_NOT_INITIALIZED(0x06),
    WEBCLIENT_NOT_INITIALIZED(0x07),
    WEB_CLUSTER_CONFIG_FILE_NOT_FOUND(0x08),
    EVENTS_ARE_ALREADY_REGISTERED_UNDER_GROUP(0x09),
    INVALID_CLUSTER_MANAGER_TYPE(0x10),
    ;

    private final int code;

    WebClientErrorCodes(int localCode) {
        this.code = WebClientErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class WebClientErrorCodesGroup implements ErrorCodeGroup {
        private static final WebClientErrorCodesGroup GROUP = new WebClientErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x16_0_0001;
        }
    }
}

