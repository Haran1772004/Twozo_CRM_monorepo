package com.tlc.crm.zapier.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Represents the zapier error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ZapierErrorCodes implements ErrorCodeProvider {

    WEBHOOK_SUBSCRIPTION_FAILED(0x01),
    WEBHOOK_UNSUBSCRIPTION_FAILED(0x02),
    INVALID_ZAP_EVENT(0x03),
    WEBHOOK_SUBSCRIPTION_RECORD_NOT_FOUND(0x04),
    INVALID_DROPDOWN_FIELD(0x05),
    INVALID_SOURCE(0x06),
    INVALID_ZAP_ACTION(0x07),
    ACTION_FIELDS_NOT_FOUND(0x08),
    DATA_VALIDATION_FAILED(0x09),
    ENTITY_NOT_FOUND(0x10),
    CREATE_ACTION_FAILED(0x11),
    UPDATE_ACTION_FAILED(0x12),
    INSTALLATION_FAILED(0x13),
    UNINSTALLATION_FAILED(0x14),
    UNSUPPORTED_ACTION(0x15),
    ;

    private final int code;

    ZapierErrorCodes(final int localCode) {
        this.code = ZapierErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class ZapierErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ZapierErrorCodeGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x80_0_0001;
        }
    }
}
