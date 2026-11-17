package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import com.tlc.sql.common.api.dml.LimitClause;
import lombok.NonNull;

/**
 * Holds the {@link LimitClause} details for underlying data.
 *
 * @author KarthickV
 * @version 1.0
 */
public record AnalyticsLimit( @NonNull Integer start, @NonNull Integer limit )
{

    /**
     * Returns the {@link LimitClause}.
     *
     * @return {@link LimitClause}.
     */
    public LimitClause getLimitClause()
    {
        return new LimitClause( Math.max( 1, start ), Math.min( 100, limit ) );
    }

}
