package com.tlc.crm.security.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of the role permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum PermissionErrorCodes implements ErrorCodeProvider {

    ACTION_NOT_FOUND(0x01),
    CATEGORY_NOT_FOUND(0x02),
    INVALID_SCOPE(0x03),
    SCOPE_NOT_FOUND(0x04),
    PERMISSION_DENIED(0x05),
    PERMISSIONS_NOT_FOUND(0x06),
    CREATE_ACCESS_DENIED(0x07),
    UPDATE_ACCESS_DENIED(0x08),
    FAILED_TO_REGISTER_PERMISSION_VERIFIER_ACTIONS(0x09),
    PERMISSION_NOT_FOUND(0x10),
    DELETE_ACCESS_DENIED(0x11),
    INVALID_OPERATION(0x12),
    FAILED_TO_REGISTER_LIMIT_PERMISSION_VALIDATOR_ACTIONS(0x13),
    NO_LIMIT_VALIDATOR_FOUND(0x14),
    INVALID_TYPE(0x15),
    UNSUPPORTED_OPERATION(0x16),
    PERMISSION_ROLE_UPDATE_FAILED(0x17),
    UPDATE_ACTION_FAILED(0x18),
    ROLE_NOT_FOUND(0x019),
    FIELD_ACCESS_LEVEL_NOT_FOUND(0x20),
    FIELD_PERMISSION_NOT_FOUND(0x21),
    ;

    private final int code;

    PermissionErrorCodes(int localCode) {
        this.code = RolePermissionErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class RolePermissionErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new RolePermissionErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0001;
        }
    }
}
