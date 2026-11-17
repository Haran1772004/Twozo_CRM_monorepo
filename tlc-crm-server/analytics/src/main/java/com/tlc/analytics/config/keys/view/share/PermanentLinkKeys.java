package com.tlc.analytics.config.keys.view.share;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to view permanent link details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class PermanentLinkKeys
{
    public static final String VIEW_ID     = "viewId";
    public static final String LINK        = "link";
    public static final String PASSWORD    = "password";
    public static final String EXPIRY_TIME = "expiryTime";
    public static final String CREATOR     = "creator";
    public static final String CREATED_AT  = "createdAt";
    public static final String EDITOR      = "editor";
    public static final String EDITED_AT   = "editedAt";
}
