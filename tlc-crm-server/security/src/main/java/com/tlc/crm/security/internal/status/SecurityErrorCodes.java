package com.tlc.crm.security.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes for security
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public enum SecurityErrorCodes implements ErrorCodeProvider {

    USER_NOT_FOUND(0x01),
    SECURITY_CONTEXT_NOT_FOUND(0x02),
    VALUE_RESOLVER_ALREADY_EXISTS(0x03),
    ;

    private final int code;

    SecurityErrorCodes(int localCode) {
        this.code = SecurityErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class SecurityErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SecurityErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0003;
        }
    }
}
