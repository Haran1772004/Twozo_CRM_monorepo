package com.tlc.crm.core.api.analytics.entity.integ.reports;


import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.Getter;

/**
 * <p>
 * Manages CRM Filed Category
 * </p>
 *
 * @author James
 * @version 1.0
 */

@Getter
public enum FieldCategory
{
    PRIMARY_FIELD( 1 ),
    MAPPER_FIELD( 2 ),
    ACTUAL_FIELD( 3 ),
    FILTER_FIELD( 4 );

    private final int type;

    FieldCategory(int type)
    {
        this.type = type;
    }

    /**
     * Retrieves the {@link FieldCategory} enum corresponding to a given type code
     *
     * @param typeCode The type code to look up
     * @return The {@link FieldCategory} enum associated with the provided code
     */
    public static FieldCategory getType( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> PRIMARY_FIELD;
            case 2 -> MAPPER_FIELD;
            case 3 -> ACTUAL_FIELD;
            case 4 -> FILTER_FIELD;
            default -> throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD_CATEGORY_TYPE,
                    "i18n.analytics.invalid.field.category.type");
        };
    }

}
