package com.tlc.analytics.config.manager.function;

import com.tlc.analytics.config.function.Function;
import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.sql.resource.ANAFUNCTION;

import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * The {@link  PresetFunctionManager} manages the configuration details for preset functions
 * for the analytics
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Accessors( fluent = true )
@RequiredArgsConstructor
public class PresetFunctionManager extends AbstractAnalyticsConfigManager<Function>
{

    private static class Instance
    {
        private static final PresetFunctionManager INSTANCE = new PresetFunctionManager();
    }

    public static PresetFunctionManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<Function> getModelClass()
    {
        return Function.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable()
    {
        return Table.get( ANAFUNCTION.TABLE );
    }
}
