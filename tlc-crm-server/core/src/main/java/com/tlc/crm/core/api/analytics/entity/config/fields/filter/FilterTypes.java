package com.tlc.crm.core.api.analytics.entity.config.fields.filter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.commons.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the filter types
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum FilterTypes
{
    COLUMN_FILTER( 1, "i18n.crm.analytics.filter.type.column.filter" ),
    VALUE_FILTER( 2, "i18n.crm.analytics.filter.type.value.filter" );

    private final int typeCode;
    private final String i18nKey;

    /**
     * Retrieves the {@link FilterTypes} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link FilterTypes} enum associated with the provided code.
     */
    public static FilterTypes getFilterTypes( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return COLUMN_FILTER;
            }
            case 2 ->
            {
                return VALUE_FILTER;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_FILTER_TYPE,
                                    "i18n.analytics.invalid.filter.types" );
        }
    }
}
