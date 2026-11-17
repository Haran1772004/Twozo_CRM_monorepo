package com.tlc.analytics.config.action.api.table;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * <p>
 * Manages the column configuration related web actions
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction(path = "/analytics/config/table/column")
public class ColumnConfigAction extends AnalyticsConfigAction<ColumnDetail>
{
    @Override
    protected @NonNull Class<ColumnDetail> getModelClass()
    {
        return ColumnDetail.class;
    }

    @Override
    public @NonNull ConfigManager<ColumnDetail> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE()
                                         .getColumnManager();
    }
}
