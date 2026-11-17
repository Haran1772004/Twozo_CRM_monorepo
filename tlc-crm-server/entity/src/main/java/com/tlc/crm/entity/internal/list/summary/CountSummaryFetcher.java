package com.tlc.crm.entity.internal.list.summary;

import com.tlc.crm.entity.list.summary.ListSummaryFetcher;
import com.tlc.crm.entity.list.summary.model.CountSummary;
import com.tlc.crm.entity.list.summary.model.CountSummaryData;
import com.tlc.crm.entity.list.summary.model.SummaryFetchRequest;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides the count summary for the list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class CountSummaryFetcher implements ListSummaryFetcher<CountSummary> {

    private static final short ZERO = 0;
    private static final String TOTAL = "i18n.list.summary.type.total";

    /**
     * {@inheritDoc}
     *
     * @param summaryFetchRequest Holds the details of the {@link CountSummaryFetcher}.
     * @return Count summary details.
     */
    @Override
    public CountSummary getSummary(final SummaryFetchRequest summaryFetchRequest) {
        final I18nResolutionAdapter i18NResolutionAdapter = summaryFetchRequest.i18NResolutionAdapter();
        final CountSummary countSummary = new CountSummary();
        final Collection<CountSummaryData> countSummaries = getCountSummaries(summaryFetchRequest);
        final CountSummaryData countSummaryData = new CountSummaryData(ZERO, summaryFetchRequest.totalCount(), resolveI18nString(TOTAL, i18NResolutionAdapter));

        countSummary.addSummaryData(countSummaryData);
        countSummaries.forEach(countSummary::addSummaryData);
        return countSummary;
    }

    /**
     * <p>
     * Resolves the I18N string using the given {@link I18nResolver}.
     * </p>
     *
     * @param i18nString              Represents the I18N string.
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return Resolved string.
     */
    protected final String resolveI18nString(final String i18nString, final I18nResolutionAdapter i18NResolutionAdapter) {
        return Objects.isNull(i18NResolutionAdapter) ? i18nString : i18NResolutionAdapter.get(i18nString);
    }

    /**
     * <p>
     * Fetches the count summaries for the list API.
     * </p>
     *
     * @param summaryFetchRequest Holds the details of the {@link CountSummaryFetcher}.
     * @return Collection of {@link CountSummaryData}.
     */
    protected abstract Collection<CountSummaryData> getCountSummaries(final SummaryFetchRequest summaryFetchRequest);
}