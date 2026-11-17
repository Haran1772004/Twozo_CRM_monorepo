package com.tlc.crm.core.api.analytics.manager.config.timePeriod;

import com.tlc.crm.core.api.analytics.entity.config.timePeriod.CRMTimePeriodField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMTIMEPERIODFIELD;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * Provides an abstract way to manage the time period fields details
 * </p>
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class TimePeriodFieldManager extends CRMAbstractReportsManager<CRMTimePeriodField>
{
    private static class Instance
    {
        private static final TimePeriodFieldManager INSTANCE = new TimePeriodFieldManager();
    }

    public static TimePeriodFieldManager INSTANCE()
    {
        return TimePeriodFieldManager.Instance.INSTANCE;
    }

    @NotNull
    protected Class<CRMTimePeriodField> getModelClass()
    {
        return CRMTimePeriodField.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMTIMEPERIODFIELD.TABLE );
    }
}
