package com.tlc.analytics.config.keys.data;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor ( access = AccessLevel.PRIVATE )
public class UnderlyingDataKeys
{
    public static final String ID = "id";
    public static final String TOTAL_RECORDS = "totalRecords";
    public static final String ROWS = "rows";
    public static final String STRUCTURE = "structure";
    public static final String COLUMNS = "columns";
    public static final String HIDDEN = "hidden";
    public static final String DISPLAY_NAME = "displayName";
    public static final String HEADERS = "headers";
    public static final String VALUES = "values";
    public static final String IS_SORTABLE = "isSortable";
    public static final String NAME = "name";
    public static final String FIELD_TYPE = "fieldType";
    public static final String IS_MANDATORY_FIELD = "isMandatoryField";
    public static final String HAS_MORE = "hasMore";
}
