package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.service.UserPreferenceHolderService;

import com.tlc.crm.workflow.v2.api.model.CustomSpanUnit;
import com.tlc.crm.workflow.v2.api.model.PredefinedSpanUnit;
import com.tlc.crm.workflow.v2.api.model.SpanConfiguration;
import com.tlc.crm.workflow.v2.api.model.SpanType;
import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import java.util.Objects;

/**
 * Computes delay/wait {@code FIRE_AT} from span configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class SpanCalculator {

    private SpanCalculator() {
    }

    /**
     * Calculates fire time as UTC epoch millis from now.
     *
     * @param spanConfiguration delay/wait span
     * @return fire-at UTC epoch millis (comparable to {@code Instant.now().toEpochMilli()})
     */
    public static long fireAt(final Long orgId, final Long userId, final SpanConfiguration spanConfiguration) {
        return fireAt(orgId, userId, spanConfiguration, Instant.now());
    }

    /**
     * Calculates fire time from a given instant (tests).
     *
     * @param spanConfiguration delay/wait span
     * @param now               current time
     * @return fire-at UTC epoch millis (comparable to {@code Instant.now().toEpochMilli()})
     */
    public static long fireAt(final Long orgId, final Long userId, final SpanConfiguration spanConfiguration, final Instant now) {
        final UserPreferenceHolder userPreferenceHolder = UserPreferenceHolderService.get().get(orgId, userId);
        final UserRegionalPreference userRegionalPreference = userPreferenceHolder.get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

        if (Objects.isNull(spanConfiguration) || Objects.isNull(spanConfiguration.getType())) {
            throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_CONFIGURATION, "i18n.workflow.span.invalid");
        }

        final ZoneId userZone = timezoneInfo.getTimeZone().toZoneId();
        ZonedDateTime fireAt = now.atZone(userZone);

        if (SpanType.PREDEFINED == spanConfiguration.getType()) {
            fireAt = applyPredefined(fireAt, spanConfiguration.getPredefinedSpanUnit());
        } else {
            fireAt = applyCustom(fireAt, spanConfiguration.getAmount(), spanConfiguration.getCustomSpanUnit());
        }

        if (Objects.nonNull(spanConfiguration.getDueTime()) && !spanConfiguration.getDueTime().isBlank()) {
            fireAt = fireAt.with(parseDueTime(spanConfiguration.getDueTime()));
        }
        if (spanConfiguration.getSkipWeekends()) {
            fireAt = skipWeekend(fireAt);
        }
        return fireAt.toInstant().toEpochMilli();
    }

    /**
     * Applies the predefined span unit to the given ZonedDateTime.
     *
     * @param from Represents the ZonedDateTime.
     * @param unit Represents the predefined span unit.
     * @return The ZonedDateTime.
     */
    private static ZonedDateTime applyPredefined(final ZonedDateTime from, final PredefinedSpanUnit unit) {
        if (Objects.isNull(unit)) {
            throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_CONFIGURATION, "i18n.workflow.invalid.span.unit");
        }

        return switch (unit) {
            case ONE_DAY -> from.plusDays(1);
            case THREE_DAYS -> from.plusDays(3);
            case FIVE_DAYS -> from.plusDays(5);
            case ONE_WEEK -> from.plusWeeks(1);
            case NEXT_MONDAY -> nextDayOfWeek(from, DayOfWeek.MONDAY);
            case NEXT_TUESDAY -> nextDayOfWeek(from, DayOfWeek.TUESDAY);
            case NEXT_WEDNESDAY -> nextDayOfWeek(from, DayOfWeek.WEDNESDAY);
            case NEXT_THURSDAY -> nextDayOfWeek(from, DayOfWeek.THURSDAY);
            case NEXT_FRIDAY -> nextDayOfWeek(from, DayOfWeek.FRIDAY);
            case NEXT_SATURDAY -> nextDayOfWeek(from, DayOfWeek.SATURDAY);
            case NEXT_SUNDAY -> nextDayOfWeek(from, DayOfWeek.SUNDAY);
            case ONE_HOUR -> from.plusHours(1);
            case EIGHT_HOURS -> from.plusHours(8);
            case TWELVE_HOURS -> from.plusHours(12);
            case FIVE_MINUTES -> from.plusMinutes(5);
            case FIFTEEN_MINUTES -> from.plusMinutes(15);
            case THIRTY_MINUTES -> from.plusMinutes(30);
        };
    }

    /**
     * Applies the custom span unit to the given ZonedDateTime.
     *
     * @param from   Represents the ZonedDateTime.
     * @param amount Represents the amount.
     * @param unit   Represents the custom span unit.
     * @return The ZonedDateTime.
     */
    private static ZonedDateTime applyCustom(final ZonedDateTime from, final Integer amount, final CustomSpanUnit unit) {
        if (Objects.isNull(amount) || amount <= 0 || Objects.isNull(unit)) {
            throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_CONFIGURATION, "i18n.workflow.invalid.span.unit");
        }

        return switch (unit) {
            case MINUTES -> from.plusMinutes(amount);
            case HOURS -> from.plusHours(amount);
            case DAYS -> from.plusDays(amount);
            case WEEKS -> from.plusWeeks(amount);
        };
    }

    /**
     * Calculates the next day of the week.
     *
     * @param from      Represents the ZonedDateTime.
     * @param dayOfWeek Represents the day of the week.
     * @return The ZonedDateTime.
     */
    private static ZonedDateTime nextDayOfWeek(final ZonedDateTime from, final DayOfWeek dayOfWeek) {
        return from.with(TemporalAdjusters.next(dayOfWeek));
    }

    /**
     * Parses the due time.
     *
     * @param dueTime Represents the due time.
     * @return The LocalTime.
     */
    private static LocalTime parseDueTime(final String dueTime) {
        final String[] parts = dueTime.split(":");
        final int hour = Integer.parseInt(parts[0]);
        final int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        return LocalTime.of(hour, minute);
    }

    /**
     * Skips the weekend.
     *
     * @param fireAt Represents the fire at.
     * @return The ZonedDateTime.
     */
    private static ZonedDateTime skipWeekend(final ZonedDateTime fireAt) {
        ZonedDateTime result = fireAt;

        while (result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY) {
            result = result.plusDays(1);
        }

        return result;
    }
}
