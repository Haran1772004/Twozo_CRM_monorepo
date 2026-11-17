package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.StaticOption;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Locale;

/**
 * Provides different ways to convert the data from / into {@link Option}
 *
 * @author Sundar
 * @version 1.0
 */
public class OptionConverter extends EntityConverter<Option> implements OptionJsonConverter<Option> {

    private final OptionService optionService;
    private final I18nService i18nService;

    private static class Instance {
        private static final OptionConverter INSTANCE = new OptionConverter();
    }

    private OptionConverter() {
        optionService = OptionService.getInstance();
        i18nService = I18nServiceProvider.get();
    }

    /**
     * Gets the {@link EmailConverter} instance
     *
     * @return {@link EmailConverter}
     */
    public static OptionConverter getInstance() {
        return OptionConverter.Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Option}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the option data
     * @return the {@link Option}
     */
    @Override
    public Option convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildOptionByPartialData(converterConfig, jsonObject)
                : buildOption(converterConfig, jsonObject);
    }

    /**
     * Builds the option by {@link JsonObject}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the option data
     * @return the {@link Option}
     */
    private Option buildOption(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Option option = new Option();
        final String idKey = FieldKeys.VALUE.getName();

        if (jsonObject.containsKey(idKey)) {
            option.setId(jsonObject.getLong(idKey));
        }
        setDataToOption(converterConfig, option, jsonObject);

        return option;
    }

    /**
     * Sets the json data into {@link Option}
     *
     * @param converterConfig converter configuration
     * @param option          which contains the option data
     * @param jsonObject      which contains option fields data
     */
    private void setDataToOption(final ConverterConfig converterConfig, final Option option, final JsonObject jsonObject) {
        final String nameKey = FieldKeys.NAME.getName();
        final String staticOptionKey = FieldKeys.STATIC_OPTION.getName();
        final String fieldIdKey = FieldKeys.FIELD_ID.getName();
        final boolean isStaticOption = jsonObject.containsKey(staticOptionKey) ? jsonObject.getBoolean(staticOptionKey)
                : option.isStaticOption();

        option.setOption(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey) : option.getOption());
        option.setStaticOption(isStaticOption);
        option.setFieldId(jsonObject.containsKey(fieldIdKey) ? jsonObject.getLong(fieldIdKey) : option.getFieldId());

        if (isStaticOption) {
            option.setOrgId(converterConfig.orgId());
        }
    }

    /**
     * Builds the option using the {@link JsonObject}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the option data
     * @return the {@link Option}
     */
    private Option buildOptionByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Long optionId = jsonObject.getLong(FieldSubTypeKeys.ID.getName());
        final Option option = optionService.get(converterConfig.orgId(), jsonObject.getLong(FieldKeys.FIELD_ID.getName()), optionId);

        setDataToOption(converterConfig, option, jsonObject);

        return option;
    }

    /**
     * Converts {@link Option} into {@link JsonObject}
     *
     * @param option A model that holds the attributes of option
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Option option) {
        final OptionContainer optionContainer = new OptionContainer();

        optionContainer.addOption(option.id(), option.getOption(), option.getOrder(), option.isEditable(), option.isDefault());

        return optionContainer.getOptions().getJsonObject(0);
    }

    /**
     * Converts {@link Option} into {@link JsonObject}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param option          A model that holds the attributes of option
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Option option) {
        final OptionContainer optionContainer = new OptionContainer();
        final boolean editable = option.isEditable();

        if (!editable) {
            option.setOption(((StaticOption) option).getOptionName(i18nService.getResolver(converterConfig.locale())));
        }
        optionContainer.addOption(option.id(), option.getOption(), option.getOrder(), editable, option.isDefault());

        return optionContainer.getOptions().getJsonObject(0);
    }

    /**
     * <p>
     * Converts {@link Option} for the given org id, field id, option id
     * </p>
     *
     * @param orgId    Represents an org id
     * @param fieldId  Represents field id
     * @param optionId Represents the option id
     * @return The {@link Option}
     */
    public Option convert(final long orgId, final long fieldId, final Long optionId) {
        final Option option = new Option();

        option.setOrgId(orgId);
        option.setFieldId(fieldId);
        option.setId(optionId);

        return option;
    }

    /**
     * <p>
     * Converts the {@link StaticOption} data into {@link JsonObject}.
     * </p>
     *
     * @param staticOption Represents the {@link StaticOption}.
     * @return {@link JsonObject} of {@link StaticOption}.
     */
    public final JsonObject convertStaticOptionToJson(final Option staticOption, final Locale locale) {
        final JsonObject jsonObject = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(locale);

        jsonObject.put(FieldKeys.ID.getName(), staticOption.id());
        jsonObject.put(FieldKeys.NAME.getName(), i18nResolver.get(staticOption.getOption()));

        return jsonObject;
    }

    /**
     * <p>
     * Converts an {@link Option} into a partial {@link JsonObject}.
     * </p>
     *
     * @param option          An entity of type {@link Option} that holds the attributes of option details.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return An {@link Object} representing the partial JSON structure of the {@link Option}.
     */
    @Override
    public JsonObject convertModelToOptionJson(final Option option, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), option.id());
        jsonObject.put(FieldKeys.NAME.getName(), converterConfig.getI18nPlaceholderResolver().get(option.getOption()));

        return jsonObject;
    }
}
