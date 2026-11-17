package com.tlc.crm.common.util;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.l10n.internal.temporal.TemporalPattern;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date and time operations based on user preferences.
 *
 * @author saravanan
 * @version 1.0
 */
public class DateUtils {

    /**
     * Converts a timestamp to a formatted date string based on user preferences.
     *
     * @param timeStamp Timestamp in milliseconds.
     * @return Formatted date string.
     */
    public static String getFormattedDateString(final long timeStamp) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();
        final ZonedDateTime targetTime = Instant.ofEpochMilli(timeStamp).atZone(timezoneInfo.getTimeZone().toZoneId());

        return switch (userRegionalPreference.getDateFormat()) {
            case RELATIVE_DATE -> TemporalServiceProvider.get(timeStamp).convertTimeStampToRelativeTime(timezoneInfo);
            case MONTH_DATE_YEAR ->
                    DateTimeFormatter.ofPattern(TemporalPattern.DatePattern.MONTH_DAY_YEAR.get()).format(targetTime);
            case DAY_MONTH_YEAR ->
                    DateTimeFormatter.ofPattern(TemporalPattern.DatePattern.DATE_1.get()).format(targetTime);
        };
    }
}
