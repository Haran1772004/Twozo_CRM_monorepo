package com.tlc.analytics.config.keys;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to base model
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CommonBaseModelKeys
{
    public static final String DISPLAY_NAME         = "displayName";
    public static final String DISPLAY_NAME_COLUMN  = "DISPLAY_NAME";
    public static final String MODIFIED_TIME        = "modifiedTime";
    public static final String MODIFIED_TIME_COLUMN = "MODIFIED_TIME";
    public static final String IS_DELETED           = "isDeleted";
    public static final String IS_DELETED_COLUMN    = "IS_DELETED";
    public static final String CURRENT_TIME         = "currentTime";
    public static final String REL_ID = "relId";
    public static final String PARENT_COLUMN_ID = "parentColumnId";
    public static final String CHILD_COLUMN_ID = "childColumnId";
    public static final String JOIN_CONDITION = "joinCondition";
}
