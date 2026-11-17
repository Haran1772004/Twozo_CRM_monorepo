package com.tlc.crm.core.api.analytics.converter.general.meta.view;

import com.tlc.analytics.config.model.view.report.chart.GraphType;

import com.tlc.crm.core.api.analytics.general.keys.api.action.list.CRMViewTypeListKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;

import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way for retrieve the view type
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class CRMViewTypeMetaGenerator
{
    private CRMViewTypeMetaGenerator()
    {
    }

    private static class Instance
    {
        private static final CRMViewTypeMetaGenerator INSTANCE = new CRMViewTypeMetaGenerator();
    }

    public static CRMViewTypeMetaGenerator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     *    Retrieves the {@link GraphType}
     * </p>
     *
     * @param i18nResolver An instance of {@link I18nResolver} for handling internationalization
     * @return the response as a {@link JsonObject}
     */
    public @NonNull JsonObject getViewTypeList( final @NonNull I18nResolver i18nResolver )
    {
        final JsonObject responseData = Json.object();
        final JsonArray viewTypeList  = Json.array();
        final IconService iconService = IconService.getInstance();

        try
        {
            for( final CRMViewType crmViewType : CRMViewType.values() )
            {
                final String aliasName = crmViewType.getAliasName();
                final JsonObject chartJson = Json.object();
                final Icon icon = iconService.fetchIconByTypeAndName( Icon.Type.CHART, aliasName );
                final JsonObject iconObject = Json.object();

                iconObject.put( CommonKeys.DISPLAY_NAME, i18nResolver.get( icon.getI18nName() ) );
                iconObject.put( CommonKeys.NAME, icon.getName() );
                iconObject.put( CRMReportKeys.ID, icon.id() );

                chartJson.put( CRMReportKeys.ID, crmViewType.getTypeCode() );
                chartJson.put( CommonKeys.VIEW_BY, aliasName );
                chartJson.put( CommonKeys.ICON, iconObject );
                viewTypeList.put( chartJson );
            }
            responseData.put( CRMViewTypeListKeys.VIEW_LIST, viewTypeList );

            return responseData;
        }
        catch( final Exception error )
        {
            final JsonObject errorResponse = Json.object();
            errorResponse.put( "ERROR", "ERROR in fetching VIEW_TYPE_LIST" );
            return errorResponse;
        }
    }
}
