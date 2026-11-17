package com.tlc.analytics.config.manager.view.path;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.sql.resource.ANAREPORTPATHMAPPING;

import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

/**
 * <p>
 * The report path manager class is responsible for managing the configuration and persistence of {@link ReportPathMapping} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
class ReportPathManager extends AbstractAnalyticsConfigManager< ReportPathMapping >
{
    private static class Instance
    {
        private static final ReportPathManager INSTANCE = new ReportPathManager();
    }

    public static ReportPathManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class< ReportPathMapping > getModelClass()
    {
        return ReportPathMapping.class;
    }

    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAREPORTPATHMAPPING.TABLE );
    }
}