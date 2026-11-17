package com.tlc.analytics.config.manager.view.permission;

import com.tlc.analytics.config.model.view.share.PermanentLink;
import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.sql.resource.ANAPERMANENTLINK;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * The Permanent link manager class is responsible for managing the configuration and persistence of {@link PermanentLink} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class PermanentLinkManager extends AbstractAnalyticsConfigManager<PermanentLink>
{
    private static class Instance
    {
        private static final PermanentLinkManager INSTANCE = new PermanentLinkManager();
    }

    public static PermanentLinkManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link PermanentLink} model class
     * </p>
     *
     * @return The {@link PermanentLink}
     */
    @Override
    protected @NonNull Class<PermanentLink> getModelClass()
    {
        return PermanentLink.class;
    }

    /**
     * <p>
     * Gets the {@link ANAPERMANENTLINK} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAPERMANENTLINK.TABLE );
    }
}
