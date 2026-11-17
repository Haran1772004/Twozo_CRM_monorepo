package com.tlc.crm.entity.features.api.activity.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.i18n.service.provider.I18nServiceProvider;

/**
 * <p>
 * Provides the constant values for the {@link Priority}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Priority implements OptionJsonConverter<Priority> {

    HIGH(1, "High"),
    MEDIUM(2, "Medium"),
    LOW(3, "Low"),
    ;

    private final int id;
    private final String displayName;

    Priority(final int id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Priority get(final int id) {
        return switch (id) {
            case 1 -> HIGH;
            case 2 -> MEDIUM;
            case 3 -> LOW;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_PRIORITY);
        };
    }

    /**
     * Gets the {@link Priority} by its name.
     *
     * @param name Refers the priority name.
     * @return The {@link Priority}.
     */
    public static Priority getByName(final String name) {
        return switch (name.toLowerCase()) {
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            case "low" -> LOW;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_PRIORITY);
        };
    }

    /**
     * Converts the specified {@code Priority} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
     *
     * @param priority        the {@code Priority} object to be converted
     * @param converterConfig the {@code ConverterConfig} containing configuration details.
     * @return a {@code JsonObject} representing the converted priority, with fields for value and name
     */
    @Override
    public JsonObject convertModelToOptionJson(final Priority priority, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), priority.id);
        jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(priority.displayName));

        return jsonObject;
    }
}