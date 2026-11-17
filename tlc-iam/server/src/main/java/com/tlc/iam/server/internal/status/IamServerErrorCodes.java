package com.tlc.iam.server.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Holds IAM server related error codes
 *
 * @author Selvakumar
 * @version 1.0
 */
public enum IamServerErrorCodes implements ErrorCodeProvider {

    CORRUPTED_JWT_TOKEN_IDENTIFIED(0x01),
    IAM_USER_NOT_FOUND(0x02),
    ORG_NOT_FOUND(0x03),
    INVALID_JWT_ISSUER(0x04),
    INVALID_JWT_GROUP(0x05),
    INVALID_ASYMMETRIC_KEYS_PATH(0x06),
    SESSION_NOT_FOUND(0x07);

    private final int code;

    IamServerErrorCodes(int localCode) {
        this.code = IamErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class IamErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new IamErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x01_0_0010;
        }
    }

}
