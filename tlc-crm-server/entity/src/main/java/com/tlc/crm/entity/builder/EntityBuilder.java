package com.tlc.crm.entity.builder;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.EntityConverterHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.Time;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Builds entity based on the given inputs
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class EntityBuilder {

    private final FieldService fieldService;
    private final FieldSourceService fieldSourceService;
    private final EntityConverterHolder entityConverterHolder;

    private static class Instance {
        private static final EntityBuilder INSTANCE = new EntityBuilder();
    }

    private EntityBuilder() {
        fieldService = FieldService.getSecureInstance();
        fieldSourceService = FieldSourceService.getInstance();
        entityConverterHolder = EntityConverterHolder.getInstance();
    }

    public static EntityBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Maps json field values to crm entity
     * </p>
     *
     * @param entity          An entity that holds the values
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the field values of entity
     */
    public void buildValueHolder(final Entity entity, final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ValueHolder oldValues = Objects.isNull(entity.getValueHolder()) ? new ValueHolder() : entity.getValueHolder();
        final ValueHolder newValues = new ValueHolder();
        final FieldSource fieldSource = fieldSourceService.getSource(entity.getEntityType());
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());

        fields.stream().filter(FieldInfo::isPersistent).forEach(crmField -> {
            final String fieldIdAsString = crmField.idAsString();
            final StoreType storeType = crmField.storeType();

            if (jsonObject.containsKey(fieldIdAsString)) {
                final Long fieldId = crmField.id();
                final Object value = jsonObject.getNullableValue(fieldId.toString());

                if (Objects.nonNull(value)) {
                    newValues.putNullableValue(fieldId, switch (storeType) {
                        case DROPDOWN -> {
                            if (jsonObject.containsKey(fieldIdAsString)) {
                                final Long optionId = fetchOptionId(jsonObject.get(fieldIdAsString));

                                if (Objects.nonNull(optionId)) {
                                    yield buildOption(entity.orgId(), fieldId, optionId);
                                }
                            }
                            yield null;
                        }
                        case MULTI_SELECT -> {
                            final JsonArray jsonArray = jsonObject.getJsonArray(fieldIdAsString);
                            final Collection<Option> optionsArray = new ArrayList<>();

                            for (int index = 0; index < jsonArray.size(); index++) {
                                final Long optionId = fetchOptionId(jsonArray.get(index));

                                if (Objects.nonNull(optionId)) {
                                    optionsArray.add(buildOption(entity.orgId(), fieldId, optionId));
                                }
                            }
                            yield optionsArray;
                        }
                        case TIME -> new Time(jsonObject.getLong(fieldIdAsString));
                        case DATE -> new Date(jsonObject.getLong(fieldIdAsString));
                        case DATE_TIME -> new DateTime(jsonObject.getLong(fieldIdAsString));
                        default -> {
                            if (StoreType.isModelStore(storeType) && !storeType.isEntityType() && StoreType.OWNER != storeType) {
                                final TlcModel model = (TlcModel) oldValues.get(fieldId);
                                final Map<String, Object> fieldJson = (Map<String, Object>) jsonObject.get(fieldIdAsString);

                                if (Objects.nonNull(model)) {
                                    fieldJson.put(FieldKeys.ID.getName(), model.id());

                                    yield convertObjectToModel(storeType, converterConfig, fieldJson);
                                } else {
                                    final boolean isPartial = converterConfig.isPartialData();

                                    converterConfig.setPartialData(false);
                                    final TlcModel tlcModel = convertObjectToModel(storeType, converterConfig, fieldJson);

                                    converterConfig.setPartialData(isPartial);
                                    yield tlcModel;
                                }
                            } else {
                                yield jsonObject.get(fieldIdAsString);
                            }
                        }
                    });
                } else {
                    newValues.putNullableValue(fieldId, null);
                }
            } else {
                newValues.put(crmField.id(), oldValues.get(crmField.id()));
            }
        });

        entity.setEntitySource(fieldSource);
        entity.setExistingValueHolder(oldValues);
        entity.setValueHolder(newValues);
    }

    /**
     * Builds the option
     *
     * @param orgId   Represents the organisation ID
     * @param fieldId Represents the field ID
     * @param id      Represents the option ID
     * @return {@link Option} built by provided details
     */
    private Option buildOption(final Long orgId, final Long fieldId, final Long id) {
        return new Option.Builder().id(id).orgId(orgId).fieldId(fieldId).build();
    }

    /**
     * Fetches the option ID from the given value
     *
     * @param value Represents the value from which the option ID will be obtained
     * @return the option ID
     */
    private Long fetchOptionId(final Object value) {

        if (value instanceof final Number number) {
            return number.longValue();
        } else if (value instanceof final Map<?, ?> mapValue) {
            return ((Number) mapValue.get(FieldKeys.VALUE.getName())).longValue();
        } else if (value instanceof final JsonObject jsonValue) {
            return jsonValue.getLong(FieldKeys.VALUE.getName());
        }
        return null;
    }

    /**
     * Converts the json object to corresponding model
     *
     * @param storeType       represents the field store type
     * @param converterConfig Converter configuration settings
     * @param value           which contains the isModelStore data to be converted
     * @return {@link TlcModel} which contains the data
     */
    private TlcModel convertObjectToModel(final StoreType storeType, final ConverterConfig converterConfig,
                                          final Object value) {
        return Objects.isNull(value) ? null
                : convertJsonToModel(storeType, converterConfig, (value instanceof final Map<?, ?> mapValue)
                ? Json.object((Map<String, ?>) mapValue) : null);
    }

    /**
     * Converts the {@link JsonObject} into {@link TlcModel}
     *
     * @param storeType       represents the field store type
     * @param converterConfig Converter configuration settings
     * @param jsonObject      which contains the data to be converted
     * @return {@link TlcModel} which contains the data
     */
    private TlcModel convertJsonToModel(final StoreType storeType, final ConverterConfig converterConfig,
                                        final JsonObject jsonObject) {
        if (Objects.nonNull(jsonObject)) {
            return switch (storeType) {
                case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE, MONETARY, EMAIL, PHONE, ADDRESS -> {
                    final EntityConverter<? extends TlcModel> converter = entityConverterHolder.getConverter(storeType);

                    yield Objects.nonNull(converter) ? (converter.convert(storeType.isEntityType()
                            ? (new ConverterConfig(converterConfig.orgId(), converterConfig.ownerId(),
                            converterConfig.locale(), true, converterConfig.dataToBeConverted()))
                            : converterConfig, jsonObject)) : null;
                }
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
            };
        }
        return null;
    }
}