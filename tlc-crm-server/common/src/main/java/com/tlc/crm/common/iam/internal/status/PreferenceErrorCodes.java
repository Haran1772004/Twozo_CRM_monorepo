package com.tlc.crm.common.iam.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of crm settings module
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum PreferenceErrorCodes implements ErrorCodeProvider {

    PREFERENCE_BUILDER_ALREADY_REGISTERED(0x1),
    PREFERENCE_ALREADY_EXIST(0x02),
    ;
    
    private final int code;

    PreferenceErrorCodes(int localCode) {
        this.code = SettingsPreferenceErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class SettingsPreferenceErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SettingsPreferenceErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0001;
        }
    }
}
