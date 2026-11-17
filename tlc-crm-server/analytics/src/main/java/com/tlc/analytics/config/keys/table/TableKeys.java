package com.tlc.analytics.config.keys.table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to table details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class TableKeys
{
    public static final String NAME       = "name";
    public static final String CREATOR    = "creator";
    public static final String CREATED_AT = "createdAt";
    public static final String EDITOR     = "editor";
    public static final String JOINS_INFO = "joinsInfo";
    public static final String LEFT_TO_RIGHT_RELATION = "leftToRightRelation";
    public static final String RIGHT_TO_LEFT_RELATION = "rightToLeftRelation";
}
