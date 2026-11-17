package com.tlc.l10n.temporal.interval;

/**
 * <p>
 * Provides the immutable model for intervals.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record Interval<T extends Number, U extends Number>(T start, U end) {

}
