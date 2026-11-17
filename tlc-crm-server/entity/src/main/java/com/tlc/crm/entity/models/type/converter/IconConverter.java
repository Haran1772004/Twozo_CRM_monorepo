package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.icon.model.Icon;

import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

/**
 * Provides different ways to convert the data from / into {@link Icon}
 *
 * @author kowsalya
 * @version 1.0
 */
public class IconConverter extends EntityConverter<Icon> {

    private final I18nService i18nAccess;

    private static class Instance {
        private static final IconConverter INSTANCE = new IconConverter();
    }

    private IconConverter() {
        i18nAccess = I18nServiceProvider.get();
    }

    /**
     * Gets the {@link IconConverter} instance
     *
     * @return {@link IconConverter}
     */
    public static IconConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Icon}
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the icon data
     * @return the {@link Icon}
     */
    @Override
    public Icon convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Icon icon = new Icon();
        final String idKey = FieldSubTypeKeys.ID.getName();
        final String nameKey = FieldSubTypeKeys.NAME.getName();

        if (jsonObject.containsKey(idKey)) {
            icon.setId(jsonObject.getLong(idKey));
        }
        icon.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey) : icon.getName());

        return icon;
    }

    /**
     * <p>
     * Converts {@link Icon} into {@link JsonObject}
     * </p>
     *
     * @param icon Holds the icon attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Icon icon) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldSubTypeKeys.ID.getName(), icon.id());
        jsonObject.put(FieldSubTypeKeys.NAME.getName(), icon.getName());
        jsonObject.put(FieldSubTypeKeys.DISPLAY_NAME.getName(), i18nAccess.getResolver(converterConfig.locale()).get(icon.getI18nName()));

        return jsonObject;
    }
}
