package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;

import com.tlc.i18n.I18nResolver;

import java.util.List;
import java.util.Map;

/**
 * Holds all the contextual data required to process one X-Y axis pair
 * for building graph/report data.
 *
 * @author Ryan Sam Daniel N
 * @version 1.0
 */
public record ReportAxisContext(Long orgId,
                                GraphAxis<Object> xAxis,
                                GraphAxis<Object> yAxis,
                                Map<Long, CrmUser> crmUserMap,
                                DisplayValueResolver displayValueResolver,
                                I18nResolver i18nResolver) {
    public List<AxisValue<Object>> getXAxisValues() {
        return xAxis.getAxisValues();
    }

    public List<AxisValue<Object>> getYAxisValues() {
        return yAxis.getAxisValues();
    }

    public Long getXAxisColumnId() {
        return xAxis.getReportField().getColumnId();
    }

    public Long getYAxisColumnId() {
        return yAxis.getReportField().getColumnId();
    }

    public String getXAxisLabel() {
        return xAxis.getReportField().getLabel();
    }

    public String getYAxisLabel() {
        return yAxis.getReportField().getLabel();
    }

    public String getXAxisFunction() {
        return xAxis.getReportField().getFunctionName();
    }
}
