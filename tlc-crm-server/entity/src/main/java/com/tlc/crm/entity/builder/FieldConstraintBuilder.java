package com.tlc.crm.entity.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.PresetConfig.Configuration;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;

import java.util.Objects;

/**
 * <p>
 * Builds field validation constraint JSON by resolving i18n messages from the preset configuration.
 * Constructs validation rules with their corresponding localized messages and values.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class FieldConstraintBuilder {

    private static class Instance {
        private static final FieldConstraintBuilder INSTANCE = new FieldConstraintBuilder();
    }

    private FieldConstraintBuilder() {
    }

    /**
     * <p>
     * Returns the singleton instance of {@link FieldConstraintBuilder}.
     * </p>
     *
     * @return The singleton {@link FieldConstraintBuilder} instance.
     */
    public static FieldConstraintBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds a validation JSON object for the given field by resolving i18n messages
     * from the preset configuration. If validations are already present in the preset config JSON,
     * they are returned as-is. Otherwise, individual constraints (required, minLength, maxLength,
     * min, max, maxLimit, maxDate, pattern) are extracted, resolved with localized messages,
     * and assembled into a new validation JSON object.
     * </p>
     *
     * <p>After building, raw message keys are removed from the preset config JSON.</p>
     *
     * @param fieldInfo             The {@link FieldInfo} containing the field's preset configuration.
     * @param presetConfigJson      The {@link JsonObject} representing the field's preset configuration.
     * @param i18NResolutionAdapter The {@link I18nResolutionAdapter} used to resolve localized messages.
     * @return A {@link JsonObject} containing the constructed validation constraints with resolved messages.
     */
    public JsonObject build(final FieldInfo fieldInfo, final JsonObject presetConfigJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject validationJson;

        if (presetConfigJson.containsKey(FieldKeys.VALIDATIONS.getName())) {
            validationJson = presetConfigJson.getJsonObject(FieldKeys.VALIDATIONS.getName());
        } else {
            validationJson = Json.object();
            final PresetConfig presetConfig = fieldInfo.presetConfig();
            final JsonObject patternJson = presetConfig.getPatternJson();
            final String messageKey = FieldKeys.MESSAGE.getName();
            final String requiredKey = PresetConfig.Configuration.REQUIRED.getName();
            final String maxLimitKey = PresetConfig.Configuration.MAX_LIMIT.getName();
            final String minLengthKey = PresetConfig.Configuration.MIN_LENGTH.getName();
            final String maxLengthKey = PresetConfig.Configuration.MAX_LENGTH.getName();
            final String minKey = FieldKeys.MIN.getName();
            final String valueKey = FieldKeys.VALUE.getName();
            final String maxKey = FieldKeys.MAX.getName();
            final String maxDateKey = FieldKeys.MAX_DATE.getName();

            if (presetConfigJson.containsKey(requiredKey)) {
                final JsonObject requiredJson = Json.object();

                requiredJson.put(messageKey, i18NResolutionAdapter.get(presetConfig.getRequiredMessage()));
                requiredJson.put(valueKey, presetConfig.hasOverrideRequired() ? presetConfig.overrideRequired() : presetConfig.required());
                validationJson.put(requiredKey, requiredJson);
            }

            if (presetConfigJson.containsKey(maxLimitKey)) {
                final JsonObject maxLimitJson = Json.object();

                maxLimitJson.put(messageKey, String.format(i18NResolutionAdapter.get(Objects.requireNonNullElse(presetConfig.getMaxLimitMessage(), StringUtil.EMPTY_STRING)), presetConfig.maxLimit()));
                maxLimitJson.put(valueKey, presetConfig.maxLimit());
                validationJson.put(maxLimitKey, maxLimitJson);
            }

            if (presetConfigJson.containsKey(minLengthKey)) {
                final JsonObject minLengthJson = Json.object();

                minLengthJson.put(messageKey, String.format(i18NResolutionAdapter.get(Objects.requireNonNullElse(presetConfig.getMinLengthMessage(), StringUtil.EMPTY_STRING)), presetConfig.minLength()));
                minLengthJson.put(valueKey, presetConfig.minLength());
                validationJson.put(minLengthKey, minLengthJson);
            }

            if (presetConfigJson.containsKey(maxLengthKey)) {
                final JsonObject maxLengthJson = Json.object();

                maxLengthJson.put(messageKey, String.format(i18NResolutionAdapter.get(Objects.requireNonNullElse(presetConfig.getMaxLengthMessage(), StringUtil.EMPTY_STRING)), presetConfig.maxLength()));
                maxLengthJson.put(valueKey, presetConfig.maxLength());
                validationJson.put(maxLengthKey, maxLengthJson);
            }

            if (presetConfigJson.containsKey(minKey)) {
                final JsonObject minJson = Json.object();

                minJson.put(messageKey, i18NResolutionAdapter.get(Objects.requireNonNullElse(presetConfig.getMinMessage(), StringUtil.EMPTY_STRING)));
                minJson.put(valueKey, presetConfig.min());
                validationJson.put(minKey, minJson);
            }

            if (presetConfigJson.containsKey(maxKey)) {
                final JsonObject maxJson = Json.object();

                Object maxValue = presetConfig.max();

                if (fieldInfo.storeType() == StoreType.SINGLE_NUMBER || fieldInfo.storeType() == StoreType.MONETARY || fieldInfo.storeType() == StoreType.DEAL_VALUE) {
                    maxValue = String.valueOf(maxValue).length();
                }

                maxJson.put(messageKey, String.format(i18NResolutionAdapter.get(Objects.requireNonNullElse(presetConfig.getMaxMessage(), StringUtil.EMPTY_STRING)), maxValue));
                maxJson.put(valueKey, presetConfig.max());
                validationJson.put(maxKey, maxJson);
            }

            if (presetConfigJson.containsKey(maxDateKey)) {
                final JsonObject maxDateJson = Json.object();

                maxDateJson.put(messageKey, i18NResolutionAdapter.get(Objects.requireNonNullElse(presetConfig.getMaxDateMessage(), StringUtil.EMPTY_STRING)));
                maxDateJson.put(valueKey, presetConfigJson.get(maxDateKey));
                validationJson.put(maxDateKey, maxDateJson);
            }

            if (Objects.nonNull(patternJson)) {
                final String patternKey = FieldKeys.PATTERN.getName();

                patternJson.put(messageKey, i18NResolutionAdapter.get(patternJson.getString(messageKey)));
                validationJson.put(patternKey, patternJson);
            }

        }
        presetConfigJson.remove(Configuration.REQUIRED_MESSAGE.getName());
        presetConfigJson.remove(Configuration.MAX_LIMIT_MESSAGE.getName());
        presetConfigJson.remove(Configuration.MIN_LENGTH_MESSAGE.getName());
        presetConfigJson.remove(Configuration.MAX_LENGTH_MESSAGE.getName());
        presetConfigJson.remove(Configuration.MIN_MESSAGE.getName());
        presetConfigJson.remove(Configuration.MAX_MESSAGE.getName());
        presetConfigJson.remove(Configuration.MAX_DATE_MESSAGE.getName());
        presetConfigJson.remove(Configuration.PATTERN.getName());

        return validationJson;
    }

    /**
     * <p>
     * Builds the subfield configuration for a preset configuration JSON by resolving i18n messages for subfield constraints.
     * </p>
     *
     * @param presetConfigJson Represents the preset configuration json which may contain the subfield configuration and message keys for the subfields
     * @param i18nResolver     Represents the i18n resolver to resolve the message keys in the preset configuration json
     * @return The preset configuration json with the subfield configuration built and message keys resolved to messages
     */
    public JsonObject buildSubFields(final JsonObject presetConfigJson, final I18nResolver i18nResolver) {
        final JsonObject validationJson = Json.object();
        final String messageKey = FieldKeys.MESSAGE.getName();
        final String maxLengthKey = PresetConfig.Configuration.MAX_LENGTH.getName();
        final String maxLengthMessageKey = Configuration.MAX_LENGTH_MESSAGE.getName();
        final String valueKey = FieldKeys.VALUE.getName();


        if (presetConfigJson.containsKey(maxLengthKey)) {
            final JsonObject maxLengthJson = Json.object();

            maxLengthJson.put(messageKey, String.format(i18nResolver.get(presetConfigJson.optString(maxLengthMessageKey, StringUtil.EMPTY_STRING)), presetConfigJson.get(maxLengthKey)));
            maxLengthJson.put(valueKey, presetConfigJson.get(maxLengthKey));
            validationJson.put(maxLengthKey, maxLengthJson);
            presetConfigJson.remove(maxLengthMessageKey);
        }

        if (validationJson.size() > 0) {
            presetConfigJson.put(FieldKeys.VALIDATIONS.getName(), validationJson);
        }

        return presetConfigJson;
    }
}
