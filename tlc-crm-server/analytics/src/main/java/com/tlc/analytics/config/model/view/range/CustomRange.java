package com.tlc.analytics.config.model.view.range;

import com.tlc.analytics.config.model.BaseModel;

import lombok.experimental.SuperBuilder;

/**
 * {@link CustomRange} is an abstract class defining methods for retrieving details about a custom range,
 * typically used in the context of a view column. It includes methods to get the from and to values
 * of the range, both of type T.
 *
 * @param <T> The type of the values in the custom range.
 *
 * @author Aswath
 * @version 1.0
 * @see BaseModel
 */
@SuperBuilder
public abstract class CustomRange<T> extends BaseModel
{
    /**
     * Retrieves the from value of the custom range.
     *
     * @return The from value as an object of type T.
     */
    abstract public T getFromValue();

    /**
     * Retrieves the to value of the custom range.
     *
     * @return The to value as an object of type T.
     */
    abstract public T getTOValue();
}
