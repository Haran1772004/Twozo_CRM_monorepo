package com.tlc.analytics.config.manager.view.path;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.path.PathInfo;
import com.tlc.analytics.sql.resource.ANAPATH;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * The path manager class is responsible for managing the configuration and persistence of {@link PathInfo} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class PathManager extends AbstractAnalyticsConfigManager< PathInfo >
{
    private static class Instance
    {
        private static final PathManager INSTANCE = new PathManager();
    }

    public static PathManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<PathInfo> getModelClass()
    {
        return PathInfo.class;
    }

    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAPATH.TABLE );
    }
}
