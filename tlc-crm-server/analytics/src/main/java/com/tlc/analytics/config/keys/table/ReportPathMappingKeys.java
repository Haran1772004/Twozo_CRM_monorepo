package com.tlc.analytics.config.keys.table;

import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to {@link ReportPathMapping}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ReportPathMappingKeys
{
    public static final String PATH_ID = "pathId";
    public static final String VIEW_ID = "viewId";
    public static final String PATH_INFO = "pathInfo";
    public static final String ROOT_TABLE_ID = "rootTableId";
}
