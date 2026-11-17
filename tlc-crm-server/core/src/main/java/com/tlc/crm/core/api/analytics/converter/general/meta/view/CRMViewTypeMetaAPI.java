package com.tlc.crm.core.api.analytics.converter.general.meta.view;

import com.tlc.commons.json.JsonObject;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to deal with view type meta
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface CRMViewTypeMetaAPI
{
    /**
     * <p>
     * Returns an instance of the {@link JsonObject} for view type list
     * </p>
     *
     * @return the {@link JsonObject} instance for view type list
     */
    @NonNull
    default JsonObject getViewTypeList( final I18nResolver i18nResolver )
    {
        return CRMViewTypeMetaGenerator.INSTANCE()
                                       .getViewTypeList( i18nResolver );
    }
}
