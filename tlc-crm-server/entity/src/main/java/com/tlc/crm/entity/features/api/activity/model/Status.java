package com.tlc.crm.entity.features.api.activity.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.i18n.service.provider.I18nServiceProvider;

/**
 * <p>
 * Provides the constant values for the {@link Status}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Status implements OptionJsonConverter<Status> {

    OVERDUE((short) 1, "i18n.activity.status.overdue"),
    UPCOMING((short) 2, "i18n.activity.status.upcoming"),
    COMPLETED((short) 3, "i18n.activity.status.completed"),
    ;

    private final short id;
    private final String displayName;

    Status(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCompleted() {
        return COMPLETED == this;
    }

    public static Status get(final int id) {
        return switch (id) {
            case 1 -> OVERDUE;
            case 2 -> UPCOMING;
            case 3 -> COMPLETED;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_STATUS);
        };
    }

    /**
     * Gets the {@link Status} by its name.
     *
     * @param name Refers the status name.
     * @return The {@link Status}.
     */
    public static Status getByName(final String name) {
        return switch (name.toLowerCase()) {
            case "upcoming" -> UPCOMING;
            case "completed" -> COMPLETED;
            case "overdue" -> OVERDUE;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_STATUS);
        };
    }

    /**
     * Converts the specified {@code Status} object into a {@code JsonObject} using the provided{@code ConverterConfig}.
     *
     * @param status          Refer the status
     * @param converterConfig the {@code ConverterConfig} containing configuration details
     * @return a {@code JsonObject} representing the converted status with fields for value and name
     */
    @Override
    public JsonObject convertModelToOptionJson(final Status status, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), status.id);
        jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(status.displayName));

        return jsonObject;
    }
}
