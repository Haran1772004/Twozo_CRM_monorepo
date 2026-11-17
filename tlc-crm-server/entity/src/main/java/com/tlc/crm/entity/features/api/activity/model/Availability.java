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
 * Provides the constant values for the {@link Availability}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Availability implements OptionJsonConverter<Availability> {

    BUSY(1, "i18n.activity.availability.busy"),
    FREE(2, "i18n.activity.availability.free"),
    ;

    private final int id;
    private final String displayName;

    Availability(final int id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Availability get(final int id) {
        return switch (id) {
            case 1 -> BUSY;
            case 2 -> FREE;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_AVAILABILITY);
        };
    }

    /**
     * Gets the {@link Availability} by its name.
     *
     * @param name Refers the availability name.
     * @return The {@link Availability}.
     */
    public static Availability getByName(final String name) {
        return switch (name.toLowerCase()) {
            case "busy" -> BUSY;
            case "free" -> FREE;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_AVAILABILITY);
        };
    }

    /**
     * Converts the specified {@code Availability} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
     *
     * @param availability the {@code Availability} object to be converted
     * @param converterConfig the {@code ConverterConfig} containing configuration details
     * @return a {@code JsonObject} representing the converted availability, with fields for value and name
     */
    @Override
    public JsonObject convertModelToOptionJson(final Availability availability, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), availability.id);
        jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(availability.displayName));

        return jsonObject;
    }
}
