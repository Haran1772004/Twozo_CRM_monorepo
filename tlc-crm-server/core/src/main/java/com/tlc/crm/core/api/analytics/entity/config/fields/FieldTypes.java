package com.tlc.crm.core.api.analytics.entity.config.fields;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the field types
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum FieldTypes
{
    STATIC( 1 ),
    FORMULA( 2 ),
    DYNAMIC( 3 );

    private final int typeCode;

    /**
     * Retrieves the {@link FieldTypes} enum corresponding to a given type code.
     *
     * @param typeCode The type code.
     * @return The {@link FieldTypes} enum associated with the provided code.
     */
    public static FieldTypes getFieldType( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> STATIC;
            case 2 -> FORMULA;
            case 3 -> DYNAMIC;
            default -> throw new IllegalArgumentException("Invalid type code: " + typeCode);
        };
    }
}
