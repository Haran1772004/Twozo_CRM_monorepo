package com.tlc.crm.core.features.api.quota.util;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.PeriodOffset;
import com.tlc.l10n.temporal.service.TemporalService;

import java.util.List;

/**
 * Utility class for resolving date intervals from JSON criteria.
 *
 * @author kishore J
 * @version 1.0
 */
public final class DateIntervalResolver {

    private DateIntervalResolver() {
    }

    /**
     * <p>
     * Resolves the date interval from the given JSON object containing name and id fields.
     * </p>
     *
     * @param dateIntervalJson The JSON object containing the date interval with 'name' and 'id' fields
     * @return The resolved {@link Interval} with start and end timestamps
     */
    public static Interval<Long, Long> resolve(final JsonObject dateIntervalJson) {
        final TemporalService temporalService = TemporalServiceProvider.get();
        final String name = dateIntervalJson.getString(QuotaListFields.NAME);
        final short id = dateIntervalJson.getShort(QuotaListFields.ID);
        final DateInterval dateInterval = switch (name) {
            case QuotaFields.MONTH -> DateInterval.MONTHS;
            case QuotaFields.QUARTER -> DateInterval.QUARTERS;
            default -> DateInterval.YEARS;
        };

        if (dateInterval == DateInterval.QUARTERS) {
            final List<Interval<Long, Long>> quarterIntervals = List.copyOf(temporalService.getQuarterIntervals(PeriodOffset.CURRENT));

            return quarterIntervals.get(id - 1);
        }
        final PeriodOffset[] periodOffsets = PeriodOffset.values();

        return temporalService.getPeriodInterval(periodOffsets[id - 1], dateInterval);
    }
}