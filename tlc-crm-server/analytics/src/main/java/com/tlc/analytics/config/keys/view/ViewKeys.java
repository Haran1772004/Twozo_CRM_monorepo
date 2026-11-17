package com.tlc.analytics.config.keys.view;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Represents the json keys related to view
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ViewKeys
{
    public static final String VIEW_TYPE_ID   = "viewTypeId";
    public static final String CREATOR        = "creator";
    public static final String EDITOR         = "editor";
    public static final String CREATOR_AT     = "createdAt";
    public static final String IS_CUSTOM_VIEW = "isCustomView";
    public static final String IS_HIDDEN      = "isHidden";
    public static final String DESCRIPTION    = "description";
    public static final String PERMA_LINK     = "permanentLink";
    public static final String MESSAGE = "message";
    public static final String INDEX = "index";
}
