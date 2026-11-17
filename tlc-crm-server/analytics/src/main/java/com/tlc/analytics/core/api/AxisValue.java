package com.tlc.analytics.core.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a value on an axis.
 *
 * @author KarthikV
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor( staticName = "create" )
public class AxisValue<T>
{
    private final T value;
}
