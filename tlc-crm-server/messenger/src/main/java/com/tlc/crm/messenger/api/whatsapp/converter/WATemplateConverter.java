package com.tlc.crm.messenger.api.whatsapp.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.messenger.converter.SocialModelConverter;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.Body;
import com.tlc.social.common.whatsapp.model.template.Button;
import com.tlc.social.common.whatsapp.model.template.Component;
import com.tlc.social.common.whatsapp.model.template.Footer;
import com.tlc.social.common.whatsapp.model.template.Header;
import com.tlc.social.common.whatsapp.model.template.WATemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A converter which converts {@link WATemplate} into different forms
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateConverter implements SocialModelConverter<WATemplate> {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

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
     * @param waTemplate Represents the {@link WATemplate}
     * @return the converted json
     */
    @Override
    public JsonObject convert(final WATemplate waTemplate) {
        final JsonObject templateJson = Json.object();

        templateJson.put(WAFields.ID, waTemplate.id());
        templateJson.put(WAFields.NAME, waTemplate.getName());
        templateJson.put(WAFields.LANGUAGE, waTemplate.getLanguage().getDisplayLanguage());

        final JsonArray componentsArray = Json.array();
        final Header header = waTemplate.getHeader();
        final Body body = waTemplate.getBody();
        final Footer footer = waTemplate.getFooter();
        final Collection<Button> buttons = waTemplate.getButtons();

        if (Objects.nonNull(header)) {
            final Header.Format headerFormat = header.getFormat();
            final JsonObject headerJson = Json.object();
            final JsonObject contentJson = Json.object();
            final String headerText = header.getText();

            headerJson.put(WAFields.TYPE, header.getType());
            headerJson.put(WAFields.DISPLAY_NAME, WAFields.HEADER_DISPLAY_NAME);
            contentJson.put(WAFields.TYPE, headerFormat);
            contentJson.put(WAFields.TEXT, headerText);

            switch (headerFormat) {
                case TEXT -> {
                    if (header.hasPlaceholder()) {
                        contentJson.put(WAFields.PLACEHOLDERS, extractPlaceholder(headerText));
                    }
                }
            }
            headerJson.put(WAFields.CONTENT, contentJson);
            componentsArray.put(headerJson);
        }

        if (Objects.nonNull(body)) {
            final JsonObject bodyJson = Json.object();
            final JsonObject contentJson = Json.object();
            final String bodyText = body.getText();

            bodyJson.put(WAFields.TYPE, body.getType());
            bodyJson.put(WAFields.DISPLAY_NAME, WAFields.MESSAGE_DISPLAY_NAME);
            contentJson.put(WAFields.TEXT, bodyText);

            if (body.hasPlaceholder()) {
                contentJson.put(WAFields.PLACEHOLDERS, extractPlaceholder(bodyText));
            }
            bodyJson.put(WAFields.CONTENT, contentJson);
            componentsArray.put(bodyJson);
        }

        if (Objects.nonNull(footer)) {
            final JsonObject footerJson = Json.object();
            final JsonObject content = Json.object();

            footerJson.put(WAFields.TYPE, footer.getType());
            footerJson.put(WAFields.DISPLAY_NAME, WAFields.FOOTER_DISPLAY_NAME);
            content.put(WAFields.TEXT, footer.getText());
            footerJson.put(WAFields.CONTENT, content);

            componentsArray.put(footerJson);
        }

        if (Objects.nonNull(buttons) && !buttons.isEmpty()) {
            final JsonObject buttonsJson = Json.object();
            final JsonArray buttonArray = Json.array();

            for (final Button button : buttons) {
                final JsonObject buttonJson = Json.object();
                final Button.ButtonType buttonType = button.getButtonType();

                buttonJson.put(WAFields.TYPE, buttonType);
                buttonJson.put(WAFields.INDEX, button.getIndex());
                buttonJson.put(WAFields.DISPLAY_NAME, button.getText());

                switch (buttonType) {

                    case URL -> {
                        final String url = button.getUrl();

                        buttonJson.put(WAFields.IS_VISIBLE, true);
                        buttonJson.put(WAFields.TEXT, url);

                        if (button.hasPlaceholder()) {
                            buttonJson.put(WAFields.PLACEHOLDERS, extractPlaceholder(url));
                        }
                    }
                    case QUICK_REPLY, VOICE_CALL -> buttonJson.put(WAFields.IS_VISIBLE, false);
                    case COPY_CODE -> buttonJson.put(WAFields.IS_VISIBLE, true);
                    case PHONE_NUMBER -> {
                        buttonJson.put(WAFields.IS_VISIBLE, true);
                        buttonJson.put(WAFields.TEXT, button.getPhoneNumber());
                    }
                }
                buttonArray.put(buttonJson);
            }

            buttonsJson.put(WAFields.TYPE, Component.Type.BUTTONS);
            buttonsJson.put(WAFields.BUTTONS, buttonArray);
            componentsArray.put(buttonsJson);
        }
        templateJson.put(WAFields.COMPONENTS, componentsArray);

        return templateJson;
    }

    /**
     * <p>
     * Extract the placeholders from the provided content and returns them
     * </p>
     *
     * @param content Represents the content
     * @return the collection of placeholders
     */
    private Collection<String> extractPlaceholder(final String content) {
        final Set<String> placeholders = new HashSet<>();
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);

        while (matcher.find()) {
            placeholders.add(matcher.group());
        }

        return placeholders;
    }
}