package com.tlc.analytics.core.api;

import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.core.api.internal.DataViewRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A model that holds the {@link KPIRequest} details
 *
 * @author jamesh
 * @version 1.0
 */
@Getter
@SuperBuilder
public class KPIRequest extends DataViewRequest<KPIView>
{
    private Long    orgId;
    private Long userId;
    private Long    viewId;
    private String  action;
    private Long    height;
    private Long    width;
    private boolean firstTimeLoading;
    private boolean fromDashboard;
    private boolean hasUserFilter;
    private boolean applyDashUserFilter;
    private boolean editMode;
    private boolean hasLegend;
    private boolean isDrillDownApplied;

    @Setter
    private KPIComparator kpiComparator;
}
