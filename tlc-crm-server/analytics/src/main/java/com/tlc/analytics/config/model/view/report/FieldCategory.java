package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the field types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor
@Getter
public enum FieldCategory
{
    CATEGORICAL( 1 ),
    AGGREGATE( 2 );

    private final int typeCode;

    public static FieldCategory getFieldType(final int typeCode )
    {
        switch( typeCode )
        {
            case 1 -> {
                return CATEGORICAL;
            }
            case 2 -> {
                return AGGREGATE;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_FIELD_TYPE,
                                            "i18n.analytics.invalid.field.type" );
        }
    }

    /**
     * <p>
     * Gets the {@link FieldCategory} details based on the type code
     * </p>
     *
     * @param typeCode Represents the type code
     * @return The {@link FieldCategory} details
     */
    public static FieldCategory get(final int typeCode )
    {
        return switch ( typeCode )
        {
            case 1 -> CATEGORICAL;
            case 2 -> AGGREGATE;
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_FIELD_TYPE,
                                            "i18n.analytics.invalid.field.type" );
        };
    }
}
