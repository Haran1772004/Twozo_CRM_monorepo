package com.tlc.analytics.config.model.view.range;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * {@link DateRange} is a specific implementation of the {@link GenericCustomRange} class for handling date-based custom ranges.
 * It utilizes String as the type for both from and to values, representing the textual representation of dates.
 * Extend this class to create custom date ranges within the context of view columns.
 *
 * @author Aswath
 * @version 1.0
 * @see GenericCustomRange
 */
@SuperBuilder
@Getter
public class DateRange extends GenericCustomRange<String>
{}
