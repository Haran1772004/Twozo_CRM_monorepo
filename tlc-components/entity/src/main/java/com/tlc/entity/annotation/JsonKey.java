package com.tlc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field should be serialized/deserialized using the specified JSON key.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface JsonKey
{
    /**
     * The key to be used for serialization/deserialization.
     *
     * @return The JSON key.
     */
    String key();
}