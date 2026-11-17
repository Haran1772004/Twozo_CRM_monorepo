package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * @author PandiarajkumarG
 */
public enum SettingsErrorCodes implements ErrorCodeProvider {

    CREATE_ACTION_FAILED(0x00),
    DELETE_ACTION_FAILED(0x01),
    UPDATE_ACTION_FAILED(0x02),
    RENAME_SOURCE_UNKNOWN(0x03),
    SOURCE_CONFIG_UPDATE_ACTION_FAILED(0x04),
    UNABLE_TO_UPDATE_CONFIG_AS_SOURCE_IS_DISABLED(0x5),
    MENU_NOT_FOUND(0x6),
    IMMOVABLE_MENU_ORDER_CANNOT_BE_CHANGED(0x7),
    MENU_CONFIG_NOT_FOUND(0x8),
    SOURCE_NAME_ALREADY_EXISTS(0x9),
    ;

    private final int code;

    SettingsErrorCodes(final int localCode) {
        this.code = SettingsErrorCodes.SettingsErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    private static class SettingsErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SettingsErrorCodes.SettingsErrorCodesGroup();
        @Override
        public int getPrefix()
        {
            return 0x10_0_0005;
        }
    }

}
