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
 * Provides the constant values for the {@link Reminder}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Reminder implements OptionJsonConverter<Reminder> {

    NO_REMINDER(1, "i18n.activity.reminder.noReminder"),
    FIVE_MINUTES(2, "i18n.activity.reminder.fiveMinutes"),
    TEN_MINUTES(3, "i18n.activity.reminder.tenMinutes"),
    FIFTEEN_MINUTES(4, "i18n.activity.reminder.fifteenMinutes"),
    THIRTY_MINUTES(5, "i18n.activity.reminder.thirtyMinutes"),
    ;

    private final int id;
    private final String displayName;

    Reminder(final int id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Reminder get(final int id) {
        return switch (id) {
            case 1 -> NO_REMINDER;
            case 2 -> FIVE_MINUTES;
            case 3 -> TEN_MINUTES;
            case 4 -> FIFTEEN_MINUTES;
            case 5 -> THIRTY_MINUTES;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_REMINDER);
        };
    }

    /**
     * Gets the {@link Reminder} by its name.
     *
     * @param name Refers the remainder name.
     * @return The {@link Reminder}.
     */
    public static Reminder getByName(final String name) {
        return switch (name.toLowerCase()) {
            case "no_reminder" -> NO_REMINDER;
            case "five_minutes" -> FIVE_MINUTES;
            case "ten_minutes" -> TEN_MINUTES;
            case "fifteen_minutes" -> FIFTEEN_MINUTES;
            case "thirty_minutes" -> THIRTY_MINUTES;
            default -> throw ErrorCode.get(ActivityErrorCodes.UNSUPPORTED_REMINDER);
        };
    }

    /**
     * <p>
     * Whether reminder is required.
     * </p>
     *
     * @return reminder is required.
     */
    public boolean isRequired() {
        return this != Reminder.NO_REMINDER;
    }

    /**
     * Converts the specified {@code Reminder} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
     *
     * @param reminder        the {@code Reminder} object to be converted
     * @param converterConfig the {@code ConverterConfig} containing configuration details
     * @return a {@code JsonObject} representing the converted reminder with fields for value and name
     */
    @Override
    public JsonObject convertModelToOptionJson(final Reminder reminder, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), reminder.id);
        jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(reminder.displayName));

        return jsonObject;
    }
}