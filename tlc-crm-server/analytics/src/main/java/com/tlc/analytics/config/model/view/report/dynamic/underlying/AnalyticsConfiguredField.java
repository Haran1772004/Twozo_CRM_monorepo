package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import com.tlc.crm.fields.models.StoreType;

import lombok.NonNull;

/**
 * <p>
 * Holds the analytics configured details for underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public record AnalyticsConfiguredField( @NonNull Long id, @NonNull String fieldName, boolean hidden, boolean isSortable,
                                        boolean isMandatory, @NonNull StoreType storeType )
{
}
