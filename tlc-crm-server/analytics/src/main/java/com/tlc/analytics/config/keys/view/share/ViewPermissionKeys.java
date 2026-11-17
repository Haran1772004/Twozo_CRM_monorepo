package com.tlc.analytics.config.keys.view.share;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to view permission
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ViewPermissionKeys
{
    public static final String VIEW_ID       = "viewId";
    public static final String ACCESS_LEVEL  = "accessLevel";
    public static final String SHARED_BY     = "sharedBy";
    public static final String SHARED_TIME   = "sharedTime";
    public static final String MODIFIED_BY   = "modifiedBy";
    public static final String MODIFIED_TIME = "modifiedTime";
}
