package com.tlc.entity.converter;

import com.tlc.commons.functions.TriFunction;

import com.tlc.entity.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;

/**
 * Represents the parameters required for data conversion by a converter.
 */
@Getter
@Accessors( fluent = true )
@AllArgsConstructor( staticName = "create", access = AccessLevel.PACKAGE )
@NonNull
public class ConverterParams<T>
{
    private final Field field;
    private final String tableName;
    private final Entity<?> entity;
    private final T target;
    private final TriFunction<String, String, Object, Object> valueTransformer;
}
