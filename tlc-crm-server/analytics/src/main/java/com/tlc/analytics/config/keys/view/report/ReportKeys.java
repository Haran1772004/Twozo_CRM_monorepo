package com.tlc.analytics.config.keys.view.report;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to report details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ReportKeys
{
    public static final String ROOT_TABLE_ID = "rootTableId";
    public static final String TITLE         = "title";
    public static final String REPORT_FIELDS = "reportFields";
    public static final String FILTER_FIELDS = "filterFields";
    public static final String GRAPH_TYPE = "graphType";
    public static final String COLUMN = "column";
    public static final String VALUE = "value";
    public static final String OPERATOR = "operator";
    public static final String OPERATION = "operation";
}
