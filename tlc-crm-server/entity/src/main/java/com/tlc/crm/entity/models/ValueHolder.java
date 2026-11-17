package com.tlc.crm.entity.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.validator.TlcModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>
 * Holds the field values of an entity source
 * </p>
 *
 * @author Selvakumar G
 * @version 1.0
 */
public final class ValueHolder implements Serializable {

    private final Map<Long, Object> values;

    public ValueHolder() {
        values = new HashMap<>();
    }

    /**
     * <p>
     * Inserts the value of a field into the {@link ValueHolder}
     * </p>
     *
     * @param fieldId Primary key of the field
     * @param value   Value of the field
     */
    public void put(final Long fieldId, final Object value) {
        if (Objects.nonNull(value)) {

            if (value instanceof final Collection<?> list) {

                if (!list.isEmpty()) {
                    values.put(fieldId, value);
                }
            } else {
                values.put(fieldId, value);
            }
        }
    }

    /**
     * <p>
     *  Stores the specified field value, including {@code null} values.
     * </p>
     *
     * @param fieldId  Primary key of the field
     * @param value    Value of the field
     */
    public void putNullableValue(final Long fieldId, final Object value) {
        values.put(fieldId, value);
    }

    /**
     * <p>
     * Removes the value of a field from the {@link ValueHolder}
     * </p>
     *
     * <strong>Note</strong>: This is currently used only for data revert, please avoid using this method
     *
     * @param fieldId Primary key of the field
     */
    public void remove(final Long fieldId) {
        values.remove(fieldId);
    }

    /**
     * <p>
     * Fetches the value of a field by its id
     * </p>
     *
     * @param fieldId Primary key of the field
     * @return Field value
     */
    public Object get(final Long fieldId) {
        return values.get(fieldId);
    }

    /**
     * <p>
     * Fetches the values of a field
     * </p>
     *
     * @param fieldId Primary key of a field
     * @return List of available values
     */
    public List<Object> getValues(final Long fieldId) {
        return (List<Object>) get(fieldId);
    }

    /**
     * <p>
     * Maps the field values into the json object
     * </p>
     *
     * @param jsonObject A target to which the field values will be mapped
     */
    public void mapToJson(final JsonObject jsonObject) {
        this.values.forEach((key, value) -> {

            if (value instanceof final TlcModel model) {
                jsonObject.put(key.toString(), convertModelValue(key, model));
            } else if (value instanceof final Collection<?> models) {
                final JsonArray jsonArray = Json.array();

                for (final Object model : models) {
                    jsonObject.put(key.toString(), model instanceof TlcModel tlcModel
                            ? jsonArray.put(convertModelValue(key, tlcModel)) : value);
                }
            } else if (value instanceof final Temporal temporal) {
                jsonObject.put(key.toString(), temporal.getTimeStamp());
            } else {
                jsonObject.put(key.toString(), value);
            }
        });
    }

    /**
     * Converts the {@link TlcModel} into json object\
     *
     * @param fieldId represents the field ID
     * @param model   represents the {@link TlcModel} which contains the data
     * @return {@link TlcModel} which contains the data
     */
    private Object convertModelValue(final Long fieldId, final TlcModel model) {
        final FieldInfo fieldInfo = FieldService.getSecureInstance().getField(model.orgId(), fieldId);

        if (Objects.nonNull(fieldInfo)) {
            final StoreType storeType = fieldInfo.storeType();

            return switch (storeType) {
                case DROPDOWN, MULTI_SELECT, OWNER, COMPANY, CONTACT -> model.id();
                default -> {
                    final EntityConverter<TlcModel> converter = (EntityConverter<TlcModel>) EntityConverterHolder.getInstance().getConverter(storeType);

                    yield Objects.isNull(converter) ? null : converter.convert(model);
                }
            };
        }
        return null;
    }

    public String getText(final Long fieldId) {
        return (String) get(fieldId);
    }

    public String optText(final Long fieldId, final String defaultValue) {
        final String value = (String) get(fieldId);

        return Objects.isNull(value) || value.isBlank() ? defaultValue : value;
    }

    public Collection<String> getTexts(final Long fieldId) {
        return (List<String>) get(fieldId);
    }

    public int getInt(final Long fieldId) {
        final Object value = get(fieldId);

        return Objects.isNull(value) ? 0 : (Integer) value;
    }

    public Integer optInt(final Long fieldId, final Integer defaultValue) {
        final Integer value = (Integer) get(fieldId);

        return Objects.isNull(value) ? defaultValue : value;
    }

    public long getLong(final Long fieldId) {
        final Object value = get(fieldId);

        return Objects.isNull(value) ? 0 : (Long) value;
    }

    public Long optLong(final Long fieldId, final Long defaultValue) {
        final Long value = (Long) get(fieldId);

        return Objects.isNull(value) ? defaultValue : value;
    }

    public Collection<Long> getLongList(final Long fieldId) {
        return (List<Long>) get(fieldId);
    }


    public Collection<Integer> getIntList(final Long fieldId) {
        return (List<Integer>) get(fieldId);
    }

    public Double getDouble(final Long fieldId) {
        final Object value = get(fieldId);

        return Objects.isNull(value) ? 0 : (Double) value;
    }

    public Double optDouble(final Long fieldId, final Double defaultValue) {
        final Object value = get(fieldId);

        return Objects.isNull(value) ? defaultValue : (Double) value;
    }

    public Collection<Double> getDoubleList(final Long fieldId) {
        return (List<Double>) get(fieldId);
    }

    /**
     * <p>
     * Gets the user values of a field
     * </p>
     *
     * @return The Collection of {@link CrmUser} details
     */
    public Collection<CrmUser> getUserValues() {
        final Collection<CrmUser> crmUsers = new HashSet<>();

        for (final Object value : values.values()) {

            if (value instanceof CrmUser crmUser) {
                crmUsers.add(crmUser);
            }
        }
        return crmUsers;
    }

    /**
     * <p>
     * Fetched the value holder in key & value pairs.
     * </p>
     *
     * @return value holder as {@link Map}.
     */
    public Map<Long, Object> toMap() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * <p>
     * Maps the field values from the current object to the provided {@link JsonObject}.
     * </p>
     *
     * @param jsonObject      The target {@link JsonObject} to which field values will be mapped.
     * @param converterConfig Configuration settings that may influence the conversion process.
     */
    public void mapToJson(final JsonObject jsonObject, final ConverterConfig converterConfig) {
        mapToJson(jsonObject, converterConfig, false);
    }

    /**
     * <p>
     * Maps the field values from the current object to the provided {@link JsonObject}.
     * </p>
     *
     * @param jsonObject                  The target {@link JsonObject} to which field values will be mapped.
     * @param converterConfig             Configuration settings that may influence the conversion process.
     * @param isExternalNameRequiredOnKey Refers whether the external name is required on the json key.
     */
    public void mapToJson(final JsonObject jsonObject, final ConverterConfig converterConfig, final boolean isExternalNameRequiredOnKey) {
        final Map<Long, FieldInfo> fieldsById = isExternalNameRequiredOnKey
                ? FieldService.getInstance().getFieldsById(converterConfig.orgId(), values.keySet()) : Collections.emptyMap();

        this.values.forEach((key, value) -> {
            final String jsonKey = isExternalNameRequiredOnKey ? Objects.requireNonNullElse(fieldsById.get(key).getInternalName(), key.toString()) : key.toString();

            if (value instanceof final TlcModel model) {
                jsonObject.put(jsonKey, convertModelValue(key, model, converterConfig, isExternalNameRequiredOnKey));
            } else if (value instanceof final Collection<?> models) {
                final JsonArray jsonArray = Json.array();

                for (final Object model : models) {
                    jsonObject.put(jsonKey, model instanceof TlcModel tlcModel
                            ? jsonArray.put(convertModelValue(key, tlcModel, converterConfig, isExternalNameRequiredOnKey)) : value);
                }
            } else if (value instanceof final Temporal temporal) {
                jsonObject.put(jsonKey, temporal.getTimeStamp());
            } else {
                jsonObject.put(jsonKey, value);
            }
        });
    }

    /**
     * <p>
     * Converts the specified {@link TlcModel} into a JSON representation based on its field ID.
     * </p>
     *
     * @param fieldId         Represents the field ID associated with the model.
     * @param model           The {@link TlcModel} containing the data to be converted.
     * @param converterConfig Represents the converter config.
     * @return An {@link Object} representing the JSON-converted data for the model, or
     * null if the field information is not found or the converter is not applicable.
     */
    private Object convertModelValue(final Long fieldId, final TlcModel model, final ConverterConfig converterConfig, final boolean isIdRequiredOnKey) {
        final FieldInfo fieldInfo = FieldService.getSecureInstance().getField(model.orgId(), fieldId);

        if (Objects.nonNull(fieldInfo)) {
            final StoreType storeType = fieldInfo.storeType();
            final Supplier<EntityConverter<TlcModel>> converterSupplier =
                    () -> (EntityConverter<TlcModel>) EntityConverterHolder.getInstance().getConverter(storeType);

            return switch (storeType) {
                case OWNER -> CrmUserConverter.getInstance().convertModelToOptionJson((CrmUser) model);
                case DROPDOWN, MULTI_SELECT -> ((OptionJsonConverter) converterSupplier.get()).convertModelToOptionJson(model, converterConfig);
                case COMPANY, CONTACT -> ((OptionJsonConverter) converterSupplier.get()).convertModelToOptionJson(model);
                case ADDRESS, MONETARY, EMAIL, PHONE -> {
                    if (isIdRequiredOnKey) {
                        yield converterSupplier.get().convertToRestJson(model, converterConfig);
                    } else
                        yield converterSupplier.get().convertModelToJson(model, converterConfig);
                }
                default -> {
                    final EntityConverter<TlcModel> entityConverter = converterSupplier.get();

                    yield Objects.isNull(entityConverter) ? null : entityConverter.convert(model);
                }
            };
        }
        return null;
    }

    /**
     * <p>
     * Checks whether the key present in the values are not
     * </p>
     *
     * @param fieldId  Represents the field ID
     * @return true is key is present otherwise false.
     */
    public boolean contains(final Long fieldId) {
        return this.values.containsKey(fieldId);
    }

    /**
     * <p>
     * Checks whether the values are empty are not.
     * </p>
     *
     * @return true if empty otherwise false.
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }
}
