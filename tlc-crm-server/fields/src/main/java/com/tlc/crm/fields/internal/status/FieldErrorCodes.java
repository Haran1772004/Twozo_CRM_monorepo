package com.tlc.crm.fields.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * @author KarthickV
 * @version 1.0
 */
public enum FieldErrorCodes implements ErrorCodeProvider {

    INVALID_CRM_FIELD_STORE_TYPE(0x00),
    FAILED_LOAD_CRM_FIELD_STORES_INTO_CACHE(0x01),
    FIELD_SOURCE_NOT_FOUND(0x02),
    FIELD_SUBTYPE_COLUMN_NOT_FOUND(0x03),
    ENTITY_NOT_FOUND(0x04),
    INVALID_CRM_FIELD_STORE_SUBTYPE(0x05),
    UPDATE_ACTION_FAILED(0x06)
    ;

    private final int code;

    FieldErrorCodes(int localCode) {
        this.code = FieldErrorCodes.FieldErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    private static class FieldErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new FieldErrorCodes.FieldErrorCodesGroup();
        @Override
        public int getPrefix()
        {
            return 0x10_0_0001;
        }
    }
}
