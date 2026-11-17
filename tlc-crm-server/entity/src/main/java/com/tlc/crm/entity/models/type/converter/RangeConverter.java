package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.DateRange;
import com.tlc.crm.entity.models.type.DateTimeRange;
import com.tlc.crm.entity.models.type.Range;
import com.tlc.crm.entity.models.type.TimeRange;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.l10n.temporal.model.Time;

import java.util.Map;
import java.util.Objects;

/**
 * Provides an abstraction way of implementation to convert {@link Range}
 *
 * @param <T> Represents the date and time range related types
 * @author sundar
 * @version 1.0
 * @see Range
 */
public abstract class RangeConverter<T extends Range> extends EntityConverter<T> {

    private final FieldTypeService fieldTypeService;
    private final StoreType storeType;

    RangeConverter(final StoreType storeType) {
        this.fieldTypeService = FieldTypeService.getInstance();
        this.storeType = storeType;
    }

    /**
     * Converts {@link JsonObject} into {@link DateTimeRange}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the date or time range data
     * @return the {@link DateTimeRange}
     */
    @Override
    public T convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildRangeByPartialData(converterConfig, jsonObject)
                : buildRange(converterConfig, jsonObject);
    }

    /**
     * Converts {@link JsonObject} into {@link Range}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the range data
     * @return the {@link Range}
     */
    abstract T buildRange(final ConverterConfig converterConfig, final JsonObject jsonObject);

    /**
     * Converts {@link JsonObject} into {@link DateTimeRange}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the date or time range data
     * @return the {@link DateTimeRange}
     */
    abstract T buildRangeByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject);


    /**
     * Sets the json data into {@link DateRange}
     *
     * @param converterConfig converter configuration
     * @param range           which contains the date or time range data
     * @param jsonObject      which contains the date or time range fields data
     */
    void setDataToRange(final ConverterConfig converterConfig, final T range, final JsonObject jsonObject) {
        final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
        range.setOrgId(converterConfig.orgId());

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            final FieldSubType from = groupedSubtypes.get(StoreSubType.FROM);
            final FieldSubType to = groupedSubtypes.get(StoreSubType.TO);

            if (Objects.nonNull(from)) {
                final String fromKey = from.idAsString();

                range.setFrom(jsonObject.containsKey(fromKey) ? getTemporal(range, (Long) jsonObject.opt(fromKey)) : range.getFrom());
            }

            if (Objects.nonNull(to)) {
                final String toKey = to.idAsString();

                range.setTo(jsonObject.containsKey(toKey) ? getTemporal(range, (Long) jsonObject.opt(toKey)) : range.getTo());
            }
        }
    }

    /**
     * <p>
     * Gets the {@link Temporal} with the given timestamp
     * </p>
     *
     * @param range       Represents the {@link Range}
     * @param timeStamp   Represents the timestamp value
     * @return  The instance of {@link Temporal}
     */
    private Temporal getTemporal(final T range, final Long timeStamp) {

        if (range instanceof DateRange) {
            return new Date(timeStamp);
        } else if (range instanceof TimeRange) {
            return new Time(timeStamp);
        } else if (range instanceof DateTimeRange) {
            return new DateTime(timeStamp);
        } else {
            return null;
        }
    }

    /**
     * Converts {@link Range} into {@link JsonObject}
     *
     * @param range A model that holds the attributes of date or time range
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final T range) {
        final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            jsonObject.put(FieldSubTypeKeys.ID.getName(), range.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), range.orgId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.FROM).idAsString(), range.getFrom().getTimeStamp());
            jsonObject.put(groupedSubtypes.get(StoreSubType.TO).idAsString(), range.getTo().getTimeStamp());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Range} into {@link JsonObject}
     *
     * @param range           A model that holds the attributes of date or time range
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final T range) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(range);
        } else {
            jsonObject.put(FieldSubTypeKeys.ID.getName(), range.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), range.orgId());
            jsonObject.put(StoreSubType.FROM.getAliasName(), range.getFrom().getTimeStamp());
            jsonObject.put(StoreSubType.TO.getAliasName(), range.getTo().getTimeStamp());
        }

        return jsonObject;
    }
}
