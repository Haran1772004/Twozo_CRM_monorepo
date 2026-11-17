package com.tlc.crm.common.iam.api.preferences.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.preferences.model.type.PreferenceType;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Language;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.io.Serializable;

/**
 * <p>
 * Model represents the {@link RegionalPreference} details
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface RegionalPreference <T extends PreferenceType> extends Preference<T>, Serializable {

    void setTimezoneInfo(final TimezoneInfo timezoneInfo);

    void setDateFormat(final DateFormat dateFormat);

    void setTimeFormat(final TimeFormat timeFormat);

    void setLanguage(final Language language);

    DateFormat getDateFormat();

    TimeFormat getTimeFormat();

    TimezoneInfo getTimezoneInfo();

    Language getLanguage();

    /**
     * <p>
     * Represents the Date format pattern.
     * </p>
     */
    enum DateFormat {

        RELATIVE_DATE((short) 1),
        MONTH_DATE_YEAR((short) 2),
        DAY_MONTH_YEAR((short) 3),
        ;

        private final short id;

        DateFormat(final short id) {
            this.id = id;
        }

        public static DateFormat get(final short id) {
            return switch (id) {
                case 1 -> RELATIVE_DATE;
                case 2 -> MONTH_DATE_YEAR;
                case 3 -> DAY_MONTH_YEAR;
                default -> throw ErrorCode.get(CommonErrorCodes.INVALID_DATE_FORMAT);
            };
        }

        public short getId() {
            return id;
        }
    }

    /**
     * <p>
     * Represents the time format pattern.
     * </p>
     */
    enum TimeFormat {

        TWELVE_HOURS((short) 1),
        TWENTY_FOUR_HOURS((short) 2),
        ;

        private final short id;

        TimeFormat(final short id) {
            this.id = id;
        }

        public static TimeFormat get(final short id) {
            return switch (id) {
                case 1 -> TWELVE_HOURS;
                case 2 -> TWENTY_FOUR_HOURS;
                default -> throw ErrorCode.get(CommonErrorCodes.INVALID_TIME_FORMAT);
            };
        }

        public short getId() {
            return id;
        }
    }
}
