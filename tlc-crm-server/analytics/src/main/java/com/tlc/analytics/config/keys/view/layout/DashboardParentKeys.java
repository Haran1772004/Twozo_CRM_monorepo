package com.tlc.analytics.config.keys.view.layout;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to dashboard parent view
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class DashboardParentKeys
{
    public static final String DASH_ID      = "dashId";
    public static final String ROW_INDEX    = "rowIndex";
    public static final String COLUMN_INDEX = "columnIndex";
    public static final String TITLE        = "title";
    public static final String VIEW_ID      = "viewId";
    public static final String TYPE         = "type";
    public static final String WIDTH        = "width";
    public static final String HEIGHT       = "height";
}
