package com.tlc.crm.notifications.calendar.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to calendar
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public enum CalendarErrorCodes implements ErrorCodeProvider {

    CREATE_ACTION_FAILED(0x1),
    EMAIL_NOT_FOUND(0x2),
    SYNC_EMAIL_NOT_FOUND(0x3),
    INVALID_EVENT(0x4),
    INVALID_LINKED_SOURCE_STATUS(0x5),
    CALENDAR_SYNC_ACTION_FAILED(0x6),
    CALENDAR_SYNC_SERVICE_NOT_INITIALIZED(0x7),
    CALENDAR_ALREADY_SYNCED(0x8),
    CALENDAR_SYNC_STOP_FAILED(0x9),
    ;

    private final int code;

    CalendarErrorCodes(final int localCode) {
        this.code = CalendarErrorCodes.CalendarErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class CalendarErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new CalendarErrorCodes.CalendarErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x50_0_0001;
        }
    }
}