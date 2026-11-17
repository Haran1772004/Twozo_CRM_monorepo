package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.TimeRangeManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.TimeRange;

/**
 * Provides different ways to convert the data from / into {@link TimeRange}
 *
 * @author Sundar
 * @version 1.0
 * @see RangeConverter
 */
public class TimeRangeConverter extends RangeConverter<TimeRange> {

    private final TimeRangeManager timeRangeManager;

    private static class Instance {
        private static final TimeRangeConverter INSTANCE = new TimeRangeConverter();
    }

    private TimeRangeConverter() {
        super(StoreType.TIME_RANGE);
        timeRangeManager = TimeRangeManager.getInstance();
    }

    /**
     * Gets the {@link TimeRangeConverter} instance
     *
     * @return {@link TimeRangeConverter}
     */
    public static TimeRangeConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link TimeRange}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the time range data
     * @return the {@link TimeRange}
     */
    @Override
    TimeRange buildRange(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TimeRange timeRange = new TimeRange();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            timeRange.setId(jsonObject.getLong(idKey));
        }
        setDataToRange(converterConfig, timeRange, jsonObject);

        return timeRange;
    }

    /**
     * Converts the partial data present in  {@link JsonObject} into {@link TimeRange}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the time range data
     * @return the {@link TimeRange}
     */
    @Override
    TimeRange buildRangeByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TimeRange timeRange = timeRangeManager.get(converterConfig.orgId(),
                jsonObject.getLong(FieldSubTypeKeys.ID.getName()));

        setDataToRange(converterConfig, timeRange, jsonObject);

        return timeRange;
    }
}
