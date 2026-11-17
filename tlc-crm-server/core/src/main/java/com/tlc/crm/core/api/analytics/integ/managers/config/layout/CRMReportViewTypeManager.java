package com.tlc.crm.core.api.analytics.integ.managers.config.layout;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMReportsViewType;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMReportViewTypeManager extends CRMAbstractReportsManager<CRMReportsViewType>
{

    private static class Instance
    {
        private static final CRMReportViewTypeManager INSTANCE = new CRMReportViewTypeManager();
    }

    public static CRMReportViewTypeManager INSTANCE()
    {
        return CRMReportViewTypeManager.Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<CRMReportsViewType> getModelClass()
    {
        return CRMReportsViewType.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( "CRMViewType" );
    }
}
