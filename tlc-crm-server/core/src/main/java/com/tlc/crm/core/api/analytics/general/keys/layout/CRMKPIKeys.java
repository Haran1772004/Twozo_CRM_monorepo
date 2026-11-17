package com.tlc.crm.core.api.analytics.general.keys.layout;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to KPI view
 * </p>
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMKPIKeys
{
    public static final String COMPARE_TO      = "compareTo";
    public static final String COMPARE_UNIT_ID = "compareUnitId";
    public static final String COMPARE_UNIT_NAME = "compareUnitName";
    public static final String FROM            = "from";
    public static final String TO              = "to";
    public static final String DURATION = "duration";
    public static final String TIME_UNIT_ID = "timeUnitId";
    public static final String TIME_UNIT_NAME = "timeUnitName";
    public static final Object CUSTOM = "custom";
    public static final String VALUE = "value";
    public static final String PREVIOUS_VALUE = "previousValue";
    public static final String CURRENT_VALUE = "currentValue";
    public static final String INDICATOR = "indicator";
    public static final String SYMBOL = "symbol";
    public static final String TIME_UNIT = "timeUnit";
}
