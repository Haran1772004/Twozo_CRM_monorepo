package com.tlc.analytics.config.manager.view;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.sql.resource.ANAREPORTFIELD;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * The ReportFieldManager class is responsible for managing the configuration and persistence of {@link ReportField} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ReportFieldManager extends AbstractAnalyticsConfigManager<ReportField>
{
    private static class Instance
    {
        private static final ReportFieldManager INSTANCE = new ReportFieldManager();
    }

    public static ReportFieldManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ReportField} model class
     * </p>
     *
     * @return The {@link ReportField}
     */
    @Override
    protected @NonNull Class<ReportField> getModelClass()
    {
        return ReportField.class;
    }

    /**
     * <p>
     * Gets the {@link ReportField} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAREPORTFIELD.TABLE );
    }
}
