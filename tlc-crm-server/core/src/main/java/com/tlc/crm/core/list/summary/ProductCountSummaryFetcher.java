package com.tlc.crm.core.list.summary;

import com.tlc.crm.entity.internal.list.summary.CountSummaryFetcher;
import com.tlc.crm.entity.list.summary.model.CountSummaryData;
import com.tlc.crm.entity.list.summary.model.SummaryFetchRequest;

import java.util.Collection;
import java.util.Collections;

/**
 * <p>
 * Provides the count summary for the deal list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ProductCountSummaryFetcher extends CountSummaryFetcher {

    private ProductCountSummaryFetcher() {
    }

    private static final class Instance {
        private static final ProductCountSummaryFetcher COUNT_SUMMARY_FETCHER = new ProductCountSummaryFetcher();
    }

    public static ProductCountSummaryFetcher getInstance() {
        return Instance.COUNT_SUMMARY_FETCHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param summaryFetchRequest Holds the details of the {@link CountSummaryFetcher}.
     * @return Collection of {@link CountSummaryData}.
     */
    @Override
    protected final Collection<CountSummaryData> getCountSummaries(final SummaryFetchRequest summaryFetchRequest) {
        return Collections.emptySet();
    }
}