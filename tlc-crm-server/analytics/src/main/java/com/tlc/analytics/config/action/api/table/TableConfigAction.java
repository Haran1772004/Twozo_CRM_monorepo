package com.tlc.analytics.config.action.api.table;

import com.tlc.analytics.config.action.AnalyticsConfigAction;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.table.TableDetail;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.web.WebAction;

/**
 * <p>
 * Manages the table configuration related web actions
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see AnalyticsConfigAction
 */
@WebAction(path = "/analytics/config/table")
public class TableConfigAction extends AnalyticsConfigAction<TableDetail>
{
    @Override
    public ConfigManager<TableDetail> getConfigManager()
    {
        return AnalyticsConfigManagerImpl.INSTANCE().getTableManager();
    }

    @Override
    protected Class<TableDetail> getModelClass()
    {
        return TableDetail.class;
    }
}
