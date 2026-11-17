package com.tlc.crm.entity.list.summary;

import com.tlc.crm.entity.list.summary.model.ListSummary;
import com.tlc.crm.entity.list.summary.model.SummaryFetchRequest;

/**
 * <p>
 * Provides the summary for the list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ListSummaryFetcher<T extends ListSummary> {

    /**
     * <p>
     * Fetches the summary of the list API.
     * </p>
     *
     * @param summaryFetchRequest Holds the details of the {@link ListSummaryFetcher}.
     * @return Summary details.
     */
    T getSummary(final SummaryFetchRequest summaryFetchRequest);
}