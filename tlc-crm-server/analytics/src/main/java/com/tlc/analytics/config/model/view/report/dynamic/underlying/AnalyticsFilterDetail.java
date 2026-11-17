package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.view.filter.OperatorType;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the analytics filter details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
public class AnalyticsFilterDetail extends BaseModel
{
    private Long filterFieldId;

    private OperatorType operatorType;

    private Object values;

    private AnalyticsRange range;
}
