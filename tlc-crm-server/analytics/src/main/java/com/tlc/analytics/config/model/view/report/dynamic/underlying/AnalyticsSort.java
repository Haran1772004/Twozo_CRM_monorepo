package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import com.tlc.crm.common.models.SortType;

import lombok.NonNull;

/**
 * <p>
 * Holds the sorting criteria for analytics
 * </p>
 *
 * @param fieldId Represents the field id
 * @param type    Represents the sort type
 * @author pandiarajkumar G
 * @version 1.0
 */
public record AnalyticsSort( @NonNull Long fieldId, @NonNull SortType type )
{
}
