package com.tlc.entity.converter;

import com.tlc.commons.functions.TriFunction;
import com.tlc.entity.Entity;

import lombok.NonNull;

/**
 * Interface for accessing converters to convert data between different representations.
 * Provides static methods for obtaining converter instances.
 *
 * @param <T> The type of target for conversion.
 */
public interface Converter<T>
{
    /**
     * Converts data from a {@link Entity} to the target representation.
     *
     * @param entity           The source entity to convert from.
     * @param target           The target object to convert into.
     * @param valueTransformer A function to transform column values during conversion.
     * @throws Exception If an error occurs during the conversion process.
     */
    void convert( @NonNull final Entity<?> entity,
                  @NonNull final T target,
                  @NonNull final TriFunction<String, String, Object, Object> valueTransformer ) throws Exception;


    /**
     * Converts data from a {@link Entity} to the target representation.
     * Uses a default transformation function that preserves column values as it is.
     *
     * @param entity The source entity to convert from.
     * @param target The target object to convert into.
     * @throws Exception If an error occurs during the conversion process.
     */
    default void convert( @NonNull final Entity<?> entity, @NonNull final T target ) throws Exception
    {
        convert( entity, target, ( tableName, columnName, value ) -> value );
    }
}
