package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.DateRangeManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.DateRange;

/**
 * Provides different ways to convert the data from / into {@link DateRange}
 *
 * @author Sundar
 * @version 1.0
 * @see RangeConverter
 */
public class DateRangeConverter extends RangeConverter<DateRange> {

    private final DateRangeManager dateRangeManager;

    private static class Instance {
        private static final DateRangeConverter INSTANCE = new DateRangeConverter();
    }

    private DateRangeConverter() {
        super(StoreType.DATE_RANGE);
        dateRangeManager = DateRangeManager.getInstance();
    }

    /**
     * Gets the {@link DateRangeConverter} instance
     *
     * @return {@link DateRangeConverter}
     */
    public static DateRangeConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link DateRange}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the date range data
     * @return the {@link DateRange}
     */
    @Override
    DateRange buildRange(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final DateRange dateRange = new DateRange();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            dateRange.setId(jsonObject.getLong(idKey));
        }
        setDataToRange(converterConfig, dateRange, jsonObject);

        return dateRange;
    }

    /**
     * Converts the partial data present in  {@link JsonObject} into {@link DateRange}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the date range data
     * @return the {@link DateRange}
     */
    @Override
    DateRange buildRangeByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final DateRange dateRange = dateRangeManager.get(converterConfig.orgId(),
                jsonObject.getLong(FieldSubTypeKeys.ID.getName()));

        setDataToRange(converterConfig, dateRange, jsonObject);

        return dateRange;
    }

}
