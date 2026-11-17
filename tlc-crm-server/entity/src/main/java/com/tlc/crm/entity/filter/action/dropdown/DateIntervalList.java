package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.filter.fields.DateIntervalAttributes;
import com.tlc.crm.entity.service.filter.DateIntervalService;

import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages date intervals related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "dateIntervalList")
public class DateIntervalList extends AbstractDropdownAction {

    private final DateIntervalService dateIntervalService;

    public DateIntervalList() {
        dateIntervalService = DateIntervalService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested date interval values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final JsonObject data = dropdownRequest.data();
        final String includeDefaultsKey = DateIntervalAttributes.INCLUDE_DEFAULTS.getName();
        final boolean includeDefaults = !data.containsKey(includeDefaultsKey) || data.getBoolean(includeDefaultsKey);
        final Map<DateIntervalGroup, Map<Long, DateInterval>> groupedDateIntervals = dateIntervalService.getSourceIntervals(
                includeDefaults, (Long) data.opt(FieldKeys.SOURCE_ID.getName()));
        final OptionContainer optionContainer = new OptionContainer();

        if (Objects.nonNull(groupedDateIntervals) && !groupedDateIntervals.isEmpty()) {
            final I18nResolver i18nResolver = dropdownRequest.i18nResolver();
            final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();

            if (includeDefaults) {
                groupedDateIntervals.forEach((dateIntervalGroup, dateIntervals) -> {
                    final Map<Long, DateInterval> matchedIntervals = filterDateIntervals(dateIntervals, dropdownCriteria, i18nResolver);

                    if (dateIntervalGroup.isRequired() && !matchedIntervals.isEmpty()) {
                        final JsonObject jsonObject = convertDateIntervalToJson(matchedIntervals, i18nResolver);

                        jsonObject.put(DateIntervalAttributes.CATEGORY.getName(), i18nResolver.get(dateIntervalGroup.getName()));
                        optionContainer.addOption(jsonObject);
                    }
                });
            } else {
                groupedDateIntervals.forEach((dateIntervalGroup, dateIntervals) -> {
                    final Map<Long, DateInterval> matchedIntervals = filterDateIntervals(dateIntervals, dropdownCriteria, i18nResolver);

                    if (!matchedIntervals.isEmpty()) {
                        optionContainer.addOption(convertDateIntervalToJson(matchedIntervals, i18nResolver));
                    }
                });
            }
        }

        return optionContainer;
    }

    /**
     * <p>
     * Filters the date intervals whose display name matches the given dropdown criteria.
     * </p>
     *
     * @param dateIntervals    Holds the date intervals to be filtered.
     * @param dropdownCriteria Represents the search criteria; all intervals are returned when it is null.
     * @param i18nResolver     Represents the resolver of the {@link I18nKey}.
     * @return Date intervals that match the criteria, in their original order.
     */
    private Map<Long, DateInterval> filterDateIntervals(final Map<Long, DateInterval> dateIntervals,
                                                        final DropdownCriteria dropdownCriteria, final I18nResolver i18nResolver) {
        if (Objects.isNull(dropdownCriteria)) {
            return dateIntervals;
        }
        final Collection<Map.Entry<Long, DateInterval>> matchedEntries = applyCriteria(dateIntervals.entrySet(), dropdownCriteria,
                List.of(entry -> i18nResolver.get(entry.getValue().getDisplayName())), Optional.empty());
        final Map<Long, DateInterval> matchedIntervals = new LinkedHashMap<>();

        matchedEntries.forEach(entry -> matchedIntervals.put(entry.getKey(), entry.getValue()));

        return matchedIntervals;
    }

    /**
     * <p>
     * Converts the {@link DateInterval} into {@link JsonObject}.
     * </p>
     *
     * @param dateIntervals Holds the details of the {@link DateInterval}.
     * @param i18nResolver  Represents the resolver of the {@link I18nKey}.
     * @return Converted date interval json.
     */
    private JsonObject convertDateIntervalToJson(final Map<Long, DateInterval> dateIntervals, final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final JsonArray jsonArray = Json.array();

        dateIntervals.forEach((id, dateInterval) -> {
            final JsonObject dateIntervalObj = Json.object();

            if (dateInterval.isRange()) {
                dateIntervalObj.put(DateIntervalAttributes.IS_RANGE.getName(), true);
            }
            dateIntervalObj.put(DateIntervalAttributes.ID.getName(), id);
            dateIntervalObj.put(DateIntervalAttributes.NAME.getName(), i18nResolver.get(dateInterval.getDisplayName()));

            jsonArray.put(dateIntervalObj);
        });
        jsonObject.put(DateIntervalAttributes.DAYS.getName(), jsonArray);
        jsonObject.put(DateIntervalAttributes.SIZE.getName(), jsonArray.size());

        return jsonObject;
    }
}
