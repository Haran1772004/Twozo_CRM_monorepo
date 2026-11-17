package com.tlc.analytics.config.keys.table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to table relation
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class TableRelationKeys
{
    public static final String REL_TYPE = "relType";
    public static final String PARENT_TABLE_ID = "parentTableId";
    public static final String CHILD_TABLE_ID = "childTableId";
    public static final String DISPLAY_NAME = "displayName";
    public static final String IS_DELETED = "isDeleted";
    public static final String MODIFIED_TIME = "modifiedTime";
    public static final String REL_COL_INFO = "relColInfo";
    public static final String REL_IN_PATHS = "relationsInPaths";

}
