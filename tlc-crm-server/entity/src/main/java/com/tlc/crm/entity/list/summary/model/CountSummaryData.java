package com.tlc.crm.entity.list.summary.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.fields.ListActionFields;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * <p>
 * Represents the data of the {@link CountSummary} for the list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CountSummaryData implements Comparable<CountSummaryData> {

    private final short order;
    private final long count;
    private final String displayName;

    public CountSummaryData(final short order, final long count, final String displayName) {
        this.order = order;
        this.count = count;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getCount() {
        return count;
    }

    /**
     * <p>
     * Converts the {@link CountSummaryData} into {@link JsonObject}.
     * </p>
     *
     * @return Converted {@link JsonObject}.
     */
    public JsonObject toJson() {
        final JsonObject summaryData = Json.object();

        summaryData.put(ListActionFields.DISPLAY_NAME, displayName);
        summaryData.put(ListActionFields.COUNT, count);
        return summaryData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof CountSummaryData countSummaryData)) {
            return false;
        }
        return count == countSummaryData.count && Objects.equals(displayName, countSummaryData.displayName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(displayName, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NotNull final CountSummaryData countSummaryData) {
        return Short.compare(order, countSummaryData.order);
    }
}