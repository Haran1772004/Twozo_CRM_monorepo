package com.tlc.crm.core.api.analytics.manager.config.widget;

import com.tlc.crm.core.api.analytics.entity.config.widget.WidgetCompareUnit;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMKPICOMPAREFIELD;

import com.tlc.sql.common.api.dml.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manages the widget compare related field API
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class WidgetCompareManager extends CRMAbstractReportsManager<WidgetCompareUnit>
{
    private static class Instance
    {
        private static final WidgetCompareManager INSTANCE = new WidgetCompareManager();
    }

    public static WidgetCompareManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    protected Class<WidgetCompareUnit> getModelClass()
    {
        return WidgetCompareUnit.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMKPICOMPAREFIELD.TABLE );
    }
}