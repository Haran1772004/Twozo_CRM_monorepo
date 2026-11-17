package com.tlc.social.common.whatsapp.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.converter.SocialEntityConverter;
import com.tlc.commons.language.Language;
import com.tlc.social.common.whatsapp.model.template.Body;
import com.tlc.social.common.whatsapp.model.template.Button;
import com.tlc.social.common.whatsapp.model.template.Component;
import com.tlc.social.common.whatsapp.model.template.Footer;
import com.tlc.social.common.whatsapp.model.template.Header;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.common.whatsapp.fields.WAFields;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A converter which converts the WhatsApp message template related information
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateConverter implements SocialEntityConverter<WATemplate> {

    private WATemplateConverter() {
    }

    /**
     * Holds the instance of {@link WATemplateConverter}
     */
    private static final class InstanceHolder {
        private static final WATemplateConverter WA_TEMPLATE_CONVERTER = new WATemplateConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WATemplateConverter}
     * </p>
     *
     * @return the instance of {@link WATemplateConverter}
     */
    public static WATemplateConverter getInstance() {
        return InstanceHolder.WA_TEMPLATE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Represents the json data to be converted
     * @return {@link WATemplate}
     */
    @Override
    public WATemplate convert(final JsonObject jsonObject) {
        final WATemplate waTemplate = new WATemplate();
        final long id = jsonObject.getLong(WAFields.ID);

        if (jsonObject.containsKey(WAFields.EXTERNAL_ID)) {
            waTemplate.setExternalTemplateId(jsonObject.getLong(WAFields.EXTERNAL_ID));
            waTemplate.setId(id);
        } else {
            waTemplate.setExternalTemplateId(id);
        }
        waTemplate.setName(jsonObject.getString(WAFields.NAME));
        waTemplate.setParameterFormat(WATemplate.ParameterFormat.valueOf(jsonObject.getString(WAFields.PARAMETER_FORMAT)));
        waTemplate.setLanguage(Language.getFromCode(jsonObject.getString(WAFields.LANGUAGE)));
        final WATemplate.Category category = WATemplate.Category.valueOf(jsonObject.getString(WAFields.CATEGORY));

        waTemplate.setCategory(category);
        waTemplate.setTemplateSubCategory(switch (category) {
            case UTILITY -> jsonObject.containsKey(WAFields.SUB_CATEGORY) ?
                    WATemplate.UtilitySubCategory.valueOf(jsonObject.getString(WAFields.SUB_CATEGORY))
                    : WATemplate.UtilitySubCategory.CUSTOM;
            case MARKETING -> jsonObject.containsKey(WAFields.SUB_CATEGORY) ?
                    WATemplate.MarketingSubCategory.valueOf(jsonObject.getString(WAFields.SUB_CATEGORY)) :
                    WATemplate.MarketingSubCategory.CUSTOM;
            default -> null;
        });
        waTemplate.setStatus(WATemplate.Status.valueOf(jsonObject.getString(WAFields.STATUS)));
        final JsonArray components = jsonObject.getJsonArray(WAFields.COMPONENTS);
        final int size = components.size();

        for (int index = 0; index < size; index++) {
            final JsonObject component = components.getJsonObject(index);
            final Component.Type type = Component.Type.valueOf(component.getString(WAFields.TYPE));

            switch (type) {
                case FOOTER -> {
                    final Footer footer = new Footer();

                    footer.setText(component.getString(WAFields.TEXT));
                    waTemplate.setFooter(footer);
                }
                case BODY -> {
                    final Body body = new Body();

                    if (component.containsKey(WAFields.EXAMPLE) || component.optBoolean(WAFields.HAS_PLACEHOLDER)) {
                        body.setHasPlaceholder(true);
                    }
                    body.setText(component.getString(WAFields.TEXT));
                    waTemplate.setBody(body);
                }
                case HEADER -> {
                    final Header.Format format = Header.Format.valueOf(component.getString(WAFields.FORMAT));
                    final Header header = new Header(format);

                    switch (format) {
                        case TEXT -> {
                            header.setText(component.getString(WAFields.TEXT));

                            if (component.containsKey(WAFields.EXAMPLE) || component.optBoolean(WAFields.HAS_PLACEHOLDER)) {
                                header.setHasPlaceholder(true);
                            }
                        }
                    }
                    waTemplate.setHeader(header);
                }
                case BUTTONS -> {
                    final JsonArray buttonsArray = component.getJsonArray(WAFields.BUTTONS);
                    final int buttonSize = buttonsArray.size();

                    for (int buttonIndex = 0; buttonIndex < buttonSize; buttonIndex++) {
                        final JsonObject buttonJson = buttonsArray.getJsonObject(buttonIndex);
                        final Button.ButtonType buttonType = Button.ButtonType.valueOf(buttonJson.getString(WAFields.TYPE));
                        final Button button = new Button(buttonType);

                        button.setText(buttonJson.getString(WAFields.TEXT));
                        button.setIndex(buttonIndex);
                        switch (buttonType) {
                            case URL -> {
                                button.setUrl(buttonJson.getString(WAFields.URL));

                                if (buttonJson.containsKey(WAFields.EXAMPLE) || buttonJson.optBoolean(WAFields.HAS_PLACEHOLDER)) {
                                    button.setHasPlaceholder(true);
                                }
                            }
                            case PHONE_NUMBER -> button.setPhoneNumber(buttonJson.getString(WAFields.PHONE_NUMBER));
                        }
                        waTemplate.addButton(button);
                    }
                }
            }
        }

        return waTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * @param waTemplate Represents the {@link WATemplate}
     * @return the converted json data
     */
    @Override
    public JsonObject convert(final WATemplate waTemplate) {
        final JsonObject templateJson = Json.object();
        final WATemplate.Category category = waTemplate.getCategory();
        final Header header = waTemplate.getHeader();
        final Body body = waTemplate.getBody();
        final Footer footer = waTemplate.getFooter();
        final Collection<Button> buttons = waTemplate.getButtons();
        final JsonArray components = Json.array();

        templateJson.put(WAFields.ID, waTemplate.id());
        templateJson.put(WAFields.EXTERNAL_ID, waTemplate.getExternalTemplateId());
        templateJson.put(WAFields.NAME, waTemplate.getName());
        templateJson.put(WAFields.PARAMETER_FORMAT, waTemplate.getParameterFormat());
        templateJson.put(WAFields.CATEGORY, category);
        templateJson.put(WAFields.LANGUAGE, waTemplate.getLanguage().getCode());
        templateJson.put(WAFields.SUB_CATEGORY, waTemplate.getTemplateSubCategory());
        templateJson.put(WAFields.STATUS, waTemplate.getStatus());

        if (Objects.nonNull(header)) {
            final JsonObject headerJson = Json.object();

            headerJson.put(WAFields.TYPE, header.getType());
            headerJson.put(WAFields.FORMAT, header.getFormat());
            headerJson.put(WAFields.TEXT, header.getText());
            headerJson.put(WAFields.HAS_PLACEHOLDER, header.hasPlaceholder());
            components.put(headerJson);
        }

        if (Objects.nonNull(body)) {
            final JsonObject bodyJson = Json.object();

            bodyJson.put(WAFields.TYPE, body.getType());
            bodyJson.put(WAFields.TEXT, body.getText());
            bodyJson.put(WAFields.HAS_PLACEHOLDER, body.hasPlaceholder());

            components.put(bodyJson);
        }

        if (Objects.nonNull(footer)) {
            final JsonObject footerJson = Json.object();

            footerJson.put(WAFields.TYPE, footer.getType());
            footerJson.put(WAFields.TEXT, footer.getText());
            components.put(footerJson);
        }

        if (Objects.nonNull(buttons)) {
            final JsonObject buttonsJson = Json.object();
            final JsonArray buttonArray = Json.array();

            for (final Button button : buttons) {
                final JsonObject buttonJson = Json.object();

                buttonJson.put(WAFields.TYPE, button.getButtonType());
                buttonJson.put(WAFields.TEXT, button.getText());
                buttonJson.put(WAFields.URL, button.getUrl());
                buttonJson.put(WAFields.PHONE_NUMBER, button.getPhoneNumber());
                buttonJson.put(WAFields.HAS_PLACEHOLDER, button.hasPlaceholder());

                buttonArray.put(buttonJson);
            }
            buttonsJson.put(WAFields.TYPE, Component.Type.BUTTONS);
            buttonsJson.put(WAFields.BUTTONS, buttonArray);
            components.put(buttonsJson);
        }
        templateJson.put(WAFields.COMPONENTS, components) ;

        return templateJson;
    }
}