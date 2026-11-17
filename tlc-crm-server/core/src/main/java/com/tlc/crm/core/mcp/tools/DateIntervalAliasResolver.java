package com.tlc.crm.core.mcp.tools;

import com.tlc.crm.entity.filter.date.DateIntervals;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves friendly date interval aliases to {@link DateIntervals} enum values.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class DateIntervalAliasResolver {

    private static final Map<String, DateIntervals> ALIASES = Map.ofEntries(
            Map.entry("today", DateIntervals.TODAY),
            Map.entry("yesterday", DateIntervals.YESTERDAY),
            Map.entry("this_week", DateIntervals.THIS_WEEK),
            Map.entry("last_week", DateIntervals.LAST_WEEK),
            Map.entry("next_week", DateIntervals.NEXT_WEEK),
            Map.entry("this_month", DateIntervals.THIS_MONTH),
            Map.entry("last_month", DateIntervals.LAST_MONTH),
            Map.entry("next_month", DateIntervals.NEXT_MONTH),
            Map.entry("this_year", DateIntervals.THIS_YEAR),
            Map.entry("last_year", DateIntervals.LAST_YEAR),
            Map.entry("this_quarter", DateIntervals.THIS_QUARTER),
            Map.entry("last_quarter", DateIntervals.LAST_QUARTER),
            Map.entry("next_quarter", DateIntervals.NEXT_QUARTER),
            Map.entry("tomorrow", DateIntervals.TOMORROW),
            Map.entry("today_or_later", DateIntervals.TODAY_OR_LATER),
            Map.entry("yesterday_or_later", DateIntervals.YESTERDAY_OR_LATER),
            Map.entry("till_now", DateIntervals.TILL_NOW)
    );

    private DateIntervalAliasResolver() {
    }

    /**
     * Resolves a friendly alias or enum name to a {@link DateIntervals} value.
     *
     * @param alias interval alias (e.g. "today", "this week") or enum name (e.g. "TODAY")
     * @return resolved date interval
     * @throws IllegalArgumentException if the alias is unknown
     */
    public static DateIntervals resolve(final String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Date interval alias is required.");
        }

        final String normalized = alias.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        if (ALIASES.containsKey(normalized)) {
            return ALIASES.get(normalized);
        }

        try {
            return DateIntervals.valueOf(alias.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Unknown date interval: '%s'. Valid examples: today, yesterday, this_week, this_month, last_week, last_month, this_year, last_year, etc,.", alias));
        }
    }
}