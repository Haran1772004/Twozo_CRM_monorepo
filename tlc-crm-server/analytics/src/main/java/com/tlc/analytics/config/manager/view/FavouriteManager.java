package com.tlc.analytics.config.manager.view;

import com.tlc.analytics.config.model.view.fav.Favourite;
import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.sql.resource.ANAFAVOURITEVIEW;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * The FavManager class is responsible for managing the configuration and persistence of {@link Favourite} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FavouriteManager extends AbstractAnalyticsConfigManager<Favourite>
{
    private static class Instance
    {
        private static final FavouriteManager INSTANCE = new FavouriteManager();
    }

    public static FavouriteManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link Favourite} model class
     * </p>
     *
     * @return The {@link Favourite}
     */
    @Override
    protected @NonNull Class<Favourite> getModelClass()
    {
        return Favourite.class;
    }

    /**
     * <p>
     * Gets the {@link ANAFAVOURITEVIEW} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAFAVOURITEVIEW.TABLE );
    }
}
