package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMFavourite;

import com.tlc.crm.core.sql.resource.CRMFAVOURITEVIEW;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the dashboard favourite
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMFavouriteManager extends CRMAbstractReportsManager<CRMFavourite>
{
    private static class Instance
    {
        private static final CRMFavouriteManager INSTANCE = new CRMFavouriteManager();
    }

    public static CRMFavouriteManager INSTANCE()
    {
        return CRMFavouriteManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMFavourite> getModelClass()
    {
        return CRMFavourite.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMFAVOURITEVIEW.TABLE );
    }
}
