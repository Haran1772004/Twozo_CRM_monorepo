package com.tlc.analytics.config.model.table;

import lombok.Getter;

/**
 * <p>
 * Represents the relationship type
 * </p>
 *
 * @author karthick-v
 * @version 1.0
 */
@Getter
public enum RelationshipType
{
    ONE_TO_ONE( 1 ),
    ONE_TO_MANY( 2 ),
    MANY_TO_ONE( 3 ),
    MANY_TO_MANY( 4 );

    private final int typeCode;

    RelationshipType(int joinType )
    {
        this.typeCode = joinType;
    }
}
