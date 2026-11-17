package com.tlc.analytics.config.model.view.range;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * {@link NumericRange} is a specific implementation of the {@link GenericCustomRange} class for handling numeric-based custom ranges.
 * It utilizes Float as the type for both from and to values, representing the range of numeric values.
 * Extend this class to create custom numeric ranges within the context of view columns.
 *
 * @author Aswath
 * @version 1.0
 * @see GenericCustomRange
 */
@SuperBuilder
@Getter
public class NumericRange extends GenericCustomRange<Float>
{}
