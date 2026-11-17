package com.tlc.analytics.core.api;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.core.api.internal.DataViewRequest;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a request for chart data visualization.
 * This class extends the {@link DataViewRequest} class to specifically handle requests
 * for generating chart views.
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
public class ChartRequest extends DataViewRequest<ChartView>
{
    private Long    orgId;
    private Long    userId;
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
}
