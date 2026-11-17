package com.tlc.crm.core.api.analytics.integ.general.analytics.builders;

import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.data.UnderlyingDataBuilder;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.dashboard.DashboardBuilder;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.fields.FieldBuilder;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.view.ViewBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Responsible for building various components of CRM reports.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access =  AccessLevel.PRIVATE )
public class CRMReportsBuilder implements ViewBuilder,
                                          DashboardBuilder,
                                          FieldBuilder,
                                          UnderlyingDataBuilder
{

    private static class Instance
    {
        private static final CRMReportsBuilder INSTANCE = new CRMReportsBuilder();
    }

    public static CRMReportsBuilder INSTANCE()
    {
        return Instance.INSTANCE;
    }

}
