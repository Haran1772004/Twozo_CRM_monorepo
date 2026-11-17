package com.tlc.crm.notification.app.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Notification related error codes.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
public enum AppNotificationErrorCodes implements ErrorCodeProvider {

    INVALID_NOTIFICATION_TYPE(0x00),
    INVALID_REMINDER_TYPE(0x01),
    INVALID_REMINDER_TIME(0x02),
    INVALID_ALERT_TYPE(0x03),
    ;

    private final int code;

    AppNotificationErrorCodes(int localCode) {
        this.code = AppNotificationErrorCodes.AppNotificationErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class AppNotificationErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new AppNotificationErrorCodes.AppNotificationErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0001;
        }
    }
}
