package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import lombok.NonNull;

/**
 * Holds the range value details for underlying data.
 *
 * @author KarthickV
 * @version 1.0
 */
public record AnalyticsRange( @NonNull Long from, @NonNull Long to )
{
}
