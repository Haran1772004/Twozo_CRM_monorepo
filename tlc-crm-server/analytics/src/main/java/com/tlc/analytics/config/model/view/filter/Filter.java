package com.tlc.analytics.config.model.view.filter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the filter types
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum Filter
{
    COMPARISON( 1 ),
    LIST( 2 ),
    RANGE( 3 ),
    TEXT( 4 ),
    NUMERIC( 5 );

    private final int typeCode;

    /**
     * Retrieves the {@link Filter} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link Filter} enum associated with the provided code.
     * @throws IllegalArgumentException if the provided code does not match any known operation type.
     */
    public static Filter getFilterType( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> COMPARISON;
            case 2 -> LIST;
            case 3 -> RANGE;
            case 4 -> TEXT;
            case 5 -> NUMERIC;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_FILTER_TYPE, "i18n.analytics.invalid.filter.type");
        };
    }
}
