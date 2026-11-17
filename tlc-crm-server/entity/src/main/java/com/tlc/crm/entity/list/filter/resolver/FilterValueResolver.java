package com.tlc.crm.entity.list.filter.resolver;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;

import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.filter.internal.DefaultFilter;
import com.tlc.crm.entity.service.filter.DateIntervalService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.l10n.temporal.interval.Interval;

import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Resolves the values of the filter based on the current user.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterValueResolver {

    private final Long orgId;
    private final Long userId;
    private final DefaultFilter defaultFilter;

    private final DateIntervalService dateIntervalService;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;

    public FilterValueResolver(final Long orgId, final Long userId, final DefaultFilter defaultFilter) {
        this.orgId = orgId;
        this.userId = userId;
        this.defaultFilter = defaultFilter;

        dateIntervalService = DateIntervalService.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
    }

    /**
     * <p>
     * Resolves the values of the given filter criteria based on the current user.
     * </p>
     *
     * @param criteria Holds the criteria to retrieve the data.
     */
    public void resolve(final FilterCriteria criteria) {
        final FilterOptions filterOptions = FilterOptions.get(criteria.getCriteriaField().getAliasName());

        switch (defaultFilter) {
            case MY_DEALS, MY_COMPANIES, MY_CONTACTS, MY_PRODUCTS, MY_ACTIVITIES, TODAY_ACTIVITIES, OVER_DUE_ACTIVITIES,
                 PRIORITY_ACTIVITIES -> loadValue(userId, filterOptions, criteria);
            case THIS_MONTH_SALES -> loadValue(dateIntervalService.get(DateIntervals.THIS_MONTH).getId(), filterOptions, criteria);
            case CLOSING_THIS_WEEK -> loadValue(dateIntervalService.get(DateIntervals.THIS_WEEK).getId(), filterOptions, criteria);
            case CONTACTS_WITH_ME_IN_TEAM -> {
                final Collection<Long> teamMateIds = teamManager.getTeamMateIds(orgId, userId);

                if (!teamMateIds.isEmpty()) {
                    loadValue(teamMateIds, filterOptions, criteria);
                }
            }
            case MY_TERRITORY_DEALS, MY_TERRITORY_COMPANIES, MY_TERRITORY_CONTACTS -> {
                final Collection<Long> territories = territoryManager.getUserTerritoryIds(orgId, userId);

                loadValue(territories.isEmpty() ? null : territories, filterOptions, criteria);
            }
            case NEW_CONTACTS, RECENTLY_MODIFIED, RECENTLY_IMPORTED, RECENT_DEALS -> resolveDateTimeValue(criteria);
            case COLD_DEALS -> {
                if (FilterOptions.CREATED_TIME == filterOptions) {
                    final Optional<CriteriaValue> optionalCriteriaValue = criteria.getCriteriaValues().stream().findFirst();

                    optionalCriteriaValue.ifPresent(criteriaValue -> {
                        final Optional<Object> optionalValue = optionalCriteriaValue.map(CriteriaValue::getValue);

                        optionalValue.ifPresent(value -> {
                            final Long end = ZonedDateTime.now().minus(buildTemporalAmount(value.toString())).toInstant().toEpochMilli();

                            criteriaValue.setValue(new Interval<>(null, end));
                        });
                    });
                }
            }
            case MY_OVERDUE_DEALS -> {

                if (FilterOptions.SALES_OWNER == filterOptions) {
                    loadValue(userId, filterOptions, criteria);
                } else if (FilterOptions.EXPECTED_CLOSE_DATE == filterOptions) {
                    loadValue(dateIntervalService.get(DateIntervals.BEFORE_TODAY).getId(), filterOptions, criteria);
                }
            }
            case MY_DEALS_CLOSING_THIS_WEEK -> {

                if (FilterOptions.SALES_OWNER == filterOptions) {
                    loadValue(userId, filterOptions, criteria);
                } else if (FilterOptions.EXPECTED_CLOSE_DATE == filterOptions) {
                    loadValue(dateIntervalService.get(DateIntervals.THIS_WEEK).getId(), filterOptions, criteria);
                }
            }
            case MY_OPEN_DEALS, MY_OVERDUE_ACTIVITIES -> {

              if (FilterOptions.SALES_OWNER == filterOptions || FilterOptions.ASSIGNEE_ID == filterOptions) {
                  loadValue(userId, filterOptions, criteria);
              }
           }
           case MY_NEW_CONTACTS, MY_RECENT_CONTACTS -> {

             if (FilterOptions.SALES_OWNER == filterOptions) {
                loadValue(userId, filterOptions, criteria);
             } else if (FilterOptions.CREATED_TIME == filterOptions || FilterOptions.UPDATED_TIME == filterOptions) {
                 resolveDateTimeValue(criteria);
             }
          }
        }
    }

    /**
     * <p>
     * Resolves the filter date time value
     * </p>
     *
     * @param criteria Represents the {@link FilterCriteria}
     */
    private void resolveDateTimeValue(final FilterCriteria criteria) {
        final Optional<CriteriaValue> optionalCriteriaValue = criteria.getCriteriaValues().stream().findFirst();

        optionalCriteriaValue.ifPresent(criteriaValue -> {
            final Optional<Object> optionalValue = optionalCriteriaValue.map(CriteriaValue::getValue);

            optionalValue.ifPresent(value -> {
                final ZonedDateTime zonedDateTime = ZonedDateTime.now();
                final Long start = zonedDateTime.minus(buildTemporalAmount(value.toString())).toInstant().toEpochMilli();
                final Long end = zonedDateTime.toInstant().toEpochMilli();

                criteriaValue.setValue(new Interval<>(start, end));
            });
        });
    }

    /**
     * <p>
     * Builds the {@link Duration} from the value.
     * </p>
     *
     * @param value Represents the duration value.
     * @return {@link Duration}.
     */
    private TemporalAmount buildTemporalAmount(final String value) {
        final Pattern pattern = Pattern.compile("(\\d+)(\\D+)");
        final Matcher matcher = pattern.matcher(value);

        if (matcher.matches()) {
            final Number durationValue = NumberUtils.parseNumber(matcher.group(1));

            return switch (matcher.group(2)) {
                case "min", "Min", "MIN" -> Duration.ofMinutes(durationValue.longValue());
                case "h", "H" -> Duration.ofHours(durationValue.longValue());
                case "d", "D" -> Period.ofDays(durationValue.intValue());
                case "w", "W" -> Period.ofWeeks(durationValue.intValue());
                case "mon", "Mon", "MON" -> Period.ofMonths(durationValue.intValue());
                case "y", "Y" -> Period.ofYears(durationValue.intValue());
                default -> Duration.ofSeconds(durationValue.longValue());
            };
        }
        return Duration.ZERO;
    }

    /**
     * <p>
     * Loads the value into the {@link FilterCriteria} based on {@link CriteriaField}.
     * </p>
     *
     * @param value         Represents the retrieval value.
     * @param filterOptions Represents the options of the filter.
     * @param criteria      Holds the criteria to retrieve the data.
     */
    private void loadValue(final Object value, final FilterOptions filterOptions, final FilterCriteria criteria) {
        switch (filterOptions) {
            case SALES_OWNER, DEAL_CLOSED_ON, EXPECTED_CLOSE_DATE, ASSIGNEE_ID -> {
                final CriteriaValue criteriaValue = new CriteriaValue();

                criteriaValue.setValueEntityId(Long.parseLong(value.toString()));
                criteria.addCriteriaValue(criteriaValue);
            }
            case TEAM, TERRITORY -> {
                final CriteriaValue criteriaValue = new CriteriaValue();

                criteriaValue.setValue(value);
                criteria.addCriteriaValue(criteriaValue);
            }
        }
    }
}
