package com.tlc.analytics.config.keys.view.report.chart;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to chart type details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ChartTypeKeys
{
    public static final String TYPE             = "type";
    public static final String SUB_TYPES        =  "subTypes" ;
    public static final String IS_ENABLED       = "isEnabled" ;
    public static final String CHART_PROPERTIES = "chartProperties" ;
}
