package com.tlc.crm.entity.list.summary.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.Collection;
import java.util.TreeSet;

/**
 * <p>
 * Represents the count summary for the list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class CountSummary implements ListSummary {

    private static final SummaryType SUMMARY_TYPE = () -> "countSummary";

    private final Collection<CountSummaryData> countSummaryData;

    public CountSummary() {
        countSummaryData = new TreeSet<>();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SummaryType}.
     */
    @Override
    public SummaryType getType() {
        return SUMMARY_TYPE;
    }

    /**
     * {@inheritDoc}
     *
     * @return Summary details as {@link JsonObject}.
     */
    @Override
    public <T> T getDetails() {
        final JsonArray countSummaryArray = Json.array();

        for (final CountSummaryData summaryDetails : countSummaryData) {
            countSummaryArray.put(summaryDetails.toJson());
        }
        return (T) countSummaryArray;
    }

    /**
     * <p>
     * Adds the summary data into count summary.
     * </p>
     *
     * @param countSummaryData Represents the {@link CountSummaryData}.
     */
    public void addSummaryData(final CountSummaryData countSummaryData) {
        this.countSummaryData.add(countSummaryData);
    }
}