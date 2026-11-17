package com.tlc.analytics.core.api.internal;

import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.response.AbstractResponseProvider;

import java.util.function.Predicate;

/**
 * This class serves as an abstract base class for generating response providers specific to report data.
 * It extends the {@link AbstractResponseProvider} class to handle common aspects of response provision.
 *
 * <p>
 * - `V`: Represents the type of report data (must extend {@link AnalyticsReport})
 * - `Req`: Represents the type of request for report data (must extend {@link DataViewRequest})
 * - `Resp`: Represents the type of view response object
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
abstract public class ReportResponseProvider<V extends AnalyticsReport, Req extends DataViewRequest<V>, Resp extends ViewResponse<Req>>
        extends AbstractResponseProvider<V, Req, Resp>
{
    /**
     * {@inheritDoc}
     *
     * @param req          The view request for which the response is being checked.
     * @param cacheGenTime The timestamp when the cached response was generated.
     * @return True if the cached response is outdated, otherwise false.
     */
    @Override
    protected boolean isViewModified( final Req req, final Long cacheGenTime )
    {
        final V view = req.getView();

        final Predicate<BaseField> modTimePredicate = field -> true;

        return view.getReportFields()
                   .stream()
                   .anyMatch( modTimePredicate )
               ||
               view.getFilterFields()
                   .stream()
                   .anyMatch( modTimePredicate );
    }
}
