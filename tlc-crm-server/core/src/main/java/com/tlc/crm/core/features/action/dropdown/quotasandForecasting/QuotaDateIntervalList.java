package com.tlc.crm.core.features.action.dropdown.quotasandForecasting;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.PeriodOffset;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.l10n.temporal.interval.DateInterval.MONTHS;
import static com.tlc.l10n.temporal.interval.DateInterval.QUARTERS;
import static com.tlc.l10n.temporal.interval.DateInterval.YEARS;

/**
 * <p>
 * Manages the date interval related dropdown actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@StaticDropdown(name = "quotaDateInterval")
public class QuotaDateIntervalList implements DropdownAction {

    private static final Collection<DateInterval> DATE_INTERVALS = Arrays.asList(MONTHS, QUARTERS, YEARS);
    private static final String MONTH = "By Month";
    private static final String QUARTER = "By Quarter";
    private static final String YEAR = "By Year";

    public QuotaDateIntervalList() {
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return The {@link OptionContainer}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Locale locale = dropdownRequest.locale();
        final TimezoneInfo timezoneInfo = dropdownRequest.getTimezoneInfo();
        final OptionContainer optionContainer = new OptionContainer();
        final TemporalService temporalService = TemporalServiceProvider.get();

        for (final DateInterval dateInterval : DATE_INTERVALS) {
            final JsonArray periodArray = Json.array();
            final String intervalLabel = switch (dateInterval) {
                case MONTHS -> MONTH;
                case QUARTERS -> QUARTER;
                default -> YEAR;
            };

            if (Objects.equals(dateInterval, QUARTERS)) {
                final List<Interval<Long, Long>> quarterIntervals = List.copyOf(temporalService.getQuarterIntervals(PeriodOffset.CURRENT));
                final int size = quarterIntervals.size();

                for (int index = 0; index < size; index++) {
                    final short id = (short) (index + 1);
                    final Interval<Long, Long> interval = quarterIntervals.get(index);

                    final JsonObject periodJson = Json.object()
                            .put(QuotaFields.ID, id)
                            .put(QuotaFields.START_TIME, interval.start())
                            .put(QuotaFields.END_TIME, interval.end())
                            .put(QuotaFields.LABEL, temporalService.formatPeriodInterval(interval, dateInterval, timezoneInfo, locale));

                    periodArray.put(periodJson);
                }
            } else {
                final PeriodOffset[] periodOffsets = PeriodOffset.values();

                for (int index = 0; index < periodOffsets.length; index++) {
                    final short id = (short) (index + 1);
                    final PeriodOffset periodOffset = periodOffsets[index];
                    final Interval<Long, Long> periodInterval = temporalService.getPeriodInterval(periodOffset, dateInterval);
                    final JsonObject periodJson = Json.object()
                            .put(QuotaFields.ID, id)
                            .put(QuotaFields.START_TIME, periodInterval.start())
                            .put(QuotaFields.END_TIME, periodInterval.end())
                            .put(QuotaFields.LABEL, temporalService.formatPeriodInterval(periodInterval, dateInterval, timezoneInfo, locale));

                    if (Objects.equals(dateInterval, MONTHS) && Objects.equals(periodOffset, PeriodOffset.CURRENT)) {
                        periodJson.put(QuotaFields.IS_DEFAULT, true);
                    }

                    periodArray.put(periodJson);
                }
            }
            optionContainer.addOption(Json.object().put(intervalLabel, periodArray)
                    .put(QuotaFields.NAME, getIntervalName(dateInterval)));
        }
        return optionContainer;
    }

    /**
     * Gets the interval name for the given {@link DateInterval}.
     *
     * @param dateInterval The {@link DateInterval}
     * @return The interval name
     */
    private String getIntervalName(final DateInterval dateInterval) {
        return switch (dateInterval) {
            case MONTHS -> QuotaFields.MONTH;
            case QUARTERS -> QuotaFields.QUARTER;
            default -> QuotaFields.YEAR;
        };
    }
}
