package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.DateTimeRangeManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.DateTimeRange;
import com.tlc.crm.fields.models.StoreType;

/**
 * Provides different ways to convert the data from / into {@link DateTimeRange}
 *
 * @author Sundar
 * @version 1.0
 * @see RangeConverter
 */
public class DateTimeRangeConverter extends RangeConverter<DateTimeRange> {

    private final DateTimeRangeManager dateTimeRangeManager;

    private static class Instance {
        private static final DateTimeRangeConverter INSTANCE = new DateTimeRangeConverter();
    }

    private DateTimeRangeConverter() {
        super(StoreType.DATE_TIME_RANGE);
        dateTimeRangeManager = DateTimeRangeManager.getInstance();
    }

    /**
     * Gets the {@link DateTimeRangeConverter} instance
     *
     * @return {@link DateTimeRangeConverter}
     */
    public static DateTimeRangeConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the date and time range data
     * @return the {@link DateTimeRange}
     */
    @Override
    DateTimeRange buildRange(ConverterConfig converterConfig, JsonObject jsonObject) {
        final DateTimeRange range = new DateTimeRange();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            range.setId(jsonObject.getLong(idKey));
        }
        setDataToRange(converterConfig, range, jsonObject);

        return range;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the date or time range data
     * @return the {@link DateTimeRange}
     */
    @Override
    DateTimeRange buildRangeByPartialData(ConverterConfig converterConfig, JsonObject jsonObject) {
        final DateTimeRange range = dateTimeRangeManager.get(converterConfig.orgId(),
                jsonObject.getLong(FieldSubTypeKeys.ID.getName()));

        setDataToRange(converterConfig, range, jsonObject);

        return range;
    }

}
