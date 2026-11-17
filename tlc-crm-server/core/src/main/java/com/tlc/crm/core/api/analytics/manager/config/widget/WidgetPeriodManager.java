package com.tlc.crm.core.api.analytics.manager.config.widget;

import com.tlc.crm.core.api.analytics.entity.config.widget.WidgetPeriod;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMKPIPERIOD;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manages the widget period related field API
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class WidgetPeriodManager extends CRMAbstractReportsManager<WidgetPeriod>
{
    private static class Instance
    {
        private static final WidgetPeriodManager INSTANCE = new WidgetPeriodManager();
    }

    public static WidgetPeriodManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    protected Class<WidgetPeriod> getModelClass()
    {
        return WidgetPeriod.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMKPIPERIOD.TABLE );
    }
}
