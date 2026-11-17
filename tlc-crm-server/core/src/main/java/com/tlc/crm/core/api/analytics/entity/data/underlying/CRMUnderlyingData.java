package com.tlc.crm.core.api.analytics.entity.data.underlying;

import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsUnderlyingData;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm underlying data details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
public class CRMUnderlyingData extends AnalyticsUnderlyingData
{
    private Integer moduleId;


    private Long childViewId;
}
