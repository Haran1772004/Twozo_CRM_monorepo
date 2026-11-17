package com.tlc.analytics.core.api;

import com.tlc.analytics.config.model.view.report.dynamic.filter.AnalyticsFilterData;
import com.tlc.analytics.core.api.internal.DataViewRequest;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the {@link FilterDataRequest} details
 * </p>
 *
 * @author james
 * @version 1.0
 */
@Getter
@SuperBuilder
public class FilterDataRequest extends DataViewRequest<AnalyticsFilterData>
{
    private Long orgId;
    private Long userId;
}
