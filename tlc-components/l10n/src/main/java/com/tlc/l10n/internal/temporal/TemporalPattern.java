package com.tlc.l10n.internal.temporal;

import com.tlc.l10n.temporal.TemporalPatternType;

/**
 * <p>
 * Provides the date-time patterns.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface TemporalPattern {

    /**
     * <p>
     * Provides the pattern of date time.
     * </p>
     *
     * @return Date time pattern as {@link String}.
     */
    String get();

    /**
     * <p>
     * Provides the array of {@link TemporalPattern} for the given {@link TemporalPatternType}.
     * </p>
     *
     * @param type {@link TemporalPatternType}.
     * @return Array of {@link TemporalPattern}.
     */
    static TemporalPattern[] getPatterns(final TemporalPatternType type) {
        return switch (type) {
            case INTERNATIONAL_TIME -> InternationalTimePattern.values();
            case MERIDIAN_TIME -> MeridianTimePattern.values();
            case DATE -> DatePattern.values();
            case DAY_INCLUSIVE_DATE_TIME -> DayInclusiveDateTimePatterns.values();
            case REGULAR_DATE_TIME -> RegularDateTimePattern.values();
            case ABBREVIATED_MONTH -> AbbreviatedMonthPattern.values();
            case ABBREVIATED_DATE -> AbbreviatedDatePattern.values();
        };
    }

    /**
     * Represents the date patterns
     */
    enum DatePattern implements TemporalPattern {

        MONTH_DAY_YEAR("MM/dd/yyyy"),
        MONTH_DAY_YEAR_WITHOUT_LEADING_ZEROS1("M/d/yy"),
        MONTH_DAY_YEAR_WITHOUT_LEADING_ZEROS2("MM/d/yy"),
        DATE_1("dd/MM/yyyy"),
        REVERSED_DATE_1("yyyy/MM/dd"),
        DATE_2("dd-MM-yyyy"),
        REVERSED_DATE_2("yyyy-MM-dd"),
        DATE_3("dd.MM.yyyy"),
        REVERSED_DATE_3("yyyy.MM.dd"),
        DATE_4("dd MM yyyy"),
        REVERSED_DATE_4("yyyy MM dd"),
        DAY_MONTH_YEAR_WITHOUT_LEADING_ZEROS1("d/M/yy"),
        DAY_MONTH_YEAR_WITHOUT_LEADING_ZEROS2("dd/MM/yy"),
        YEAR_MONTH_DAY("yy/MM/dd"),
        YEAR_MONTH_DAY_WITHOUT_LEADING_ZEROS("yy/M/d"),
        MONTH_ABBREVIATION_DAY_YEAR("MMM dd, yyyy"),
        DAY_MONTH_ABBREVIATION_YEAR("dd MMM, yyyy"),
        DAY_MONTH_ABBREVIATION_YEAR1("dd/MMM/yyyy"),
        DAY_MONTH_ABBREVIATION_YEAR2("dd-MMM-yyyy"),
        DAY_MONTH_ABBREVIATION_YEAR3("dd.MMM.yyyy"),
        DAY_FULL_MONTH_YEAR("dd MMMM, yyyy"),
        YEAR_MONTH_ABBREVIATION_DAY("yyyy, MMM dd"),
        ;

        private final String pattern;

        DatePattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get() {
            return pattern;
        }
    }

    /**
     * Represents the 24-hour-based time patterns
     */
    enum InternationalTimePattern implements TemporalPattern {

        TIME("HH:mm:ss"),
        TIME_WITHOUT_SECONDS("HH:mm"),
        ;

        private final String pattern;

        InternationalTimePattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get() {
            return pattern;
        }
    }

    /**
     * Represents the 12-hour-based time patterns with meridian components
     */
    enum MeridianTimePattern implements TemporalPattern {

        MERIDIAN_TIME("h:m:s a"),
        MERIDIAN_TIME_WITHOUT_SECONDS("h:m a");

        private final String pattern;

        MeridianTimePattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get() {
            return pattern;
        }
    }

    /**
     * Represents the date and time patterns regularly used with and without meridian
     */
    enum RegularDateTimePattern implements TemporalPattern {

        DATE_TIME_1_WITH_MERIDIAN_TIME_AND_WITH_SECONDS("M/d/y h:m:s a"),
        DATE_TIME_2_WITH_MERIDIAN_TIME_AND_WITH_SECONDS("M-d-y h:m:s a"),
        DATE_TIME_3_WITH_MERIDIAN_TIME_AND_WITH_SECONDS("d/M/y h:m:s a"),
        DATE_TIME_4_WITH_MERIDIAN_TIME_AND_WITH_SECONDS("d-M-y h:m:s a"),
        DATE_TIME_5_WITH_MERIDIAN_TIME_AND_WITH_SECONDS("y-M-d h:m:s a"),
        DATE_TIME_6_WITH_MERIDIAN_TIME_AND_WITH_SECONDS("y/M/d h:m:s a"),

        DATE_TIME_1_WITH_MERIDIAN_TIME_AND_WITHOUT_SECONDS("M/d/y h:m a"),
        DATE_TIME_2_WITH_MERIDIAN_TIME_AND_WITHOUT_SECONDS("M-d-y h:m a"),
        DATE_TIME_3_WITH_MERIDIAN_TIME_AND_WITHOUT_SECONDS("d/M/y h:m a"),
        DATE_TIME_4_WITH_MERIDIAN_TIME_AND_WITHOUT_SECONDS("d-M-y h:m a"),
        DATE_TIME_5_WITH_MERIDIAN_TIME_AND_WITHOUT_SECONDS("y-M-d h:m a"),
        DATE_TIME_6_WITH_MERIDIAN_TIME_AND_WITHOUT_SECONDS("y/M/d h:m a"),

        DATE_TIME_1_WITHOUT_MERIDIAN_TIME_AND_WITHOUT_SECONDS("M/d/y h:m"),
        DATE_TIME_2_WITHOUT_MERIDIAN_TIME_AND_WITHOUT_SECONDS("M-d-y h:m"),
        DATE_TIME_3_WITHOUT_MERIDIAN_TIME_AND_WITHOUT_SECONDS("d/M/y h:m"),
        DATE_TIME_4_WITHOUT_MERIDIAN_TIME_AND_WITHOUT_SECONDS("d-M-y h:m"),
        DATE_TIME_5_WITHOUT_MERIDIAN_TIME_AND_WITHOUT_SECONDS("y-M-d h:m"),
        DATE_TIME_6_WITHOUT_MERIDIAN_TIME_AND_WITHOUT_SECONDS("y/M/d h:m"),
        ;

        private final String pattern;

        RegularDateTimePattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get() {
            return pattern;
        }
    }

    /**
     * Represents the extended date and time, including the day of the week
     */
    enum DayInclusiveDateTimePatterns implements TemporalPattern {

        DATE_MONTH_YEAR_1("E dd MMM, yyyy hh:mm a"),
        MONTH_DATE_YEAR_1("E MMM dd, yyyy hh:mm a"),
        DATE_MONTH_YEAR_2("E dd MMM, yyyy HH:mm"),
        MONTH_DATE_YEAR_2("E MMM dd, yyyy HH:mm"),
        ;

        private final String pattern;

        DayInclusiveDateTimePatterns(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get() {
            return pattern;
        }
    }

    /**
     * <p>
     * Represents the abbreviated pattern for month
     * </p>
     */
    enum AbbreviatedMonthPattern implements TemporalPattern {

        MONTH("MMM"),
        ;

        private final String pattern;

        AbbreviatedMonthPattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         *
         * @return Date time pattern as {@link String}
         */
        @Override
        public String get() {
            return pattern;
        }
    }

    /**
     * <p>
     * Represents the abbreviated pattern for date
     * </p>
     */
    enum AbbreviatedDatePattern implements TemporalPattern {

        DATE_MONTH("dd MMM"),
        ;

        private final String pattern;

        AbbreviatedDatePattern(final String pattern) {
            this.pattern = pattern;
        }

        /**
         * {@inheritDoc}
         *
         * @return Date time pattern as {@link String}
         */
        @Override
        public String get() {
            return pattern;
        }
    }
}
