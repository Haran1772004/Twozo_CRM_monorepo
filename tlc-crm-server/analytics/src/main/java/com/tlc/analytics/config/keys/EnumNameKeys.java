package com.tlc.analytics.config.keys;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to enum name
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public final class EnumNameKeys
{
    public static final String DATA_TYPE          = "dataType";
    public static final String FILTER             = "filter";
    public static final String OPERATION_TYPE     = "operationType";
    public static final String GRAPH_TYPE         = "graphType";
    public static final String FIELD_TYPE         = "fieldType";
    public static final String FIELD_CATEGORY     = "fieldCategory";
    public static final String COMPARATOR_TYPE    = "comparatorType";
}
