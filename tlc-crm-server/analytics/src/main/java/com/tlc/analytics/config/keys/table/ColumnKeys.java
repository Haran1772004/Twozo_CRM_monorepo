package com.tlc.analytics.config.keys.table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to column details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ColumnKeys
{
    public static final String TABLE_ID   = "tableId";
    public static final String DATA_TYPE  = "dataType";
    public static final String NAME       = "name";
    public static final String TABLE_INFO = "tableInfo";
}
