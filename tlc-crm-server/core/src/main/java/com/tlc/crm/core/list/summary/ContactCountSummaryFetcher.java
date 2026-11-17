package com.tlc.crm.core.list.summary;

import com.tlc.crm.core.api.deal.manager.ListStatisticsManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;

import com.tlc.crm.entity.internal.list.summary.CountSummaryFetcher;
import com.tlc.crm.entity.list.model.Statistics;
import com.tlc.crm.entity.list.summary.model.CountSummaryData;
import com.tlc.crm.entity.list.summary.model.SummaryFetchRequest;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.TreeSet;

/**
 * <p>
 * Provides the count summary for the contact list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ContactCountSummaryFetcher extends CountSummaryFetcher {

    private final ListStatisticsManager listStatisticsManager;
    private final SourceActivityManager sourceActivityManager;

    private ContactCountSummaryFetcher() {
        listStatisticsManager = ListStatisticsManager.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
    }

    private static final class Instance {
        private static final ContactCountSummaryFetcher COUNT_SUMMARY_FETCHER = new ContactCountSummaryFetcher();
    }

    public static ContactCountSummaryFetcher getInstance() {
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
        final long orgId = summaryFetchRequest.orgId();
        final Collection<CountSummaryData> summaryDetails = new TreeSet<>();
        final Statistics statistics = listStatisticsManager.getContactStatistics(orgId, summaryFetchRequest.selectQuery());
        final I18nResolutionAdapter i18NResolutionAdapter = summaryFetchRequest.i18NResolutionAdapter();

        statistics.getStats().forEach((statisticType, count) -> {
            final CountSummaryData summaryData = new CountSummaryData(statisticType.getOrder(), count,
                    resolveI18nString(statisticType.getDisplayName(), i18NResolutionAdapter));

            summaryDetails.add(summaryData);
        });
        return summaryDetails;
    }
}