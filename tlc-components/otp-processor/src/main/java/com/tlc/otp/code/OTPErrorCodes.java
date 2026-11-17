package com.tlc.otp.code;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error code for otp modules
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum OTPErrorCodes implements ErrorCodeProvider {

    VALIDATION_FAILED(0x01),
    UNSUPPORTED_OPERATION(0x02),
    INVALID_OTP_TYPE(0x03),
    INVALID_OTP_CHANNEL(0x04),
    OTP_CATEGORY_ALREADY_EXISTS(0x05),
    ;

    private final int code;

    OTPErrorCodes(final int localCode) {
        this.code = OTPErrorCodes.Group.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class Group implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new OTPErrorCodes.Group();

        @Override
        public int getPrefix() {
            return 0x00_0_00013;
        }
    }
}
