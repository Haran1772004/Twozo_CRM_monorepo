package com.tlc.crm.core.api.analytics.general.keys.layout;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Represents the json keys related to child views
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMChildViewKeys
{
    public static final String VIEW_NAME       = "viewName";
    public static final String VIEW_PROPERTIES = "viewProperties";
    public static final String VIEW_POSITION   = "position";
    public static final String METRICS = "metrics";
}
