package com.tlc.queue.internal.resource;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Manages the error codes associated with the queue module
 * @author Ashok Selvakumar
 * @version 1.0
 */
public enum QueueErrorCodes implements ErrorCodeProvider {

    SUBSCRIPTION_TOPICS_SHOULD_NOT_BE_EMPTY(0x01),
    STREAM_CONSUMER_CLOSED(0x02),
    RECORD_PUBLISH_ACTION_FAILED(0x03),
    TOPIC_NOT_FOUND(0x04),
    RECORD_CONSUMPTION_FAILED(0x05),
    TOPIC_VALIDATION_OR_CREATION_FAILED(0x06),
    INVALID_PROVIDER(0x07),
    AZURE_ACCESS_TOKEN_NOT_FOUND(0x08),
    AZURE_ACCESS_TOKEN_FETCH_FAILED(0X09),
    UNSUPPORTED_AUTHENTICATION_CALLBACK(0X10),
    AZURE_MANAGED_IDENTITY_CREDENTIAL_AUTHENTICATION_UNAVAILABLE(0X11),
    AZURE_AUTHENTICATION_ERROR(0x12);

    private final int code;

    QueueErrorCodes(int localCode) {
        this.code = QueueErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class QueueErrorCodesGroup implements ErrorCodeGroup {
        private static final QueueErrorCodesGroup GROUP = new QueueErrorCodesGroup();
        @Override
        public int getPrefix() {
            return 0x15_0_0001;
        }
    }
}
