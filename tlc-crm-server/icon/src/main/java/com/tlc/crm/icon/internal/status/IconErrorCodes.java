package com.tlc.crm.icon.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Manages the error codes associated with the icon module
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum IconErrorCodes implements ErrorCodeProvider {

    INVALID_TYPE(0x01),
    NO_ICONS_REGISTERED_FOR_TYPE(0x02);

    private final int code;

    IconErrorCodes(int localCode) {
        this.code = IconErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class IconErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new IconErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0010;
        }
    }
}
