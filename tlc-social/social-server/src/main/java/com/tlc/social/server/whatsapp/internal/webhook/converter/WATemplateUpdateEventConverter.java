package com.tlc.social.server.whatsapp.internal.webhook.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.language.Language;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.internal.webhook.event.converter.WebhookMessageEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATemplateStatusUpdateEvent;

/**
 * A converter which converts the WhatsApp template status update event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateUpdateEventConverter implements WebhookMessageEventConverter<WATemplateStatusUpdateEvent> {

    private WATemplateUpdateEventConverter() {
    }

    /**
     * Holds the instance of {@link WATemplateUpdateEventConverter}
     */
    private static final class InstanceHolder {
        private static final WATemplateUpdateEventConverter WA_TEMPLATE_UPDATE_EVENT_CONVERTER = new WATemplateUpdateEventConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WATemplateUpdateEventConverter}
     * </p>
     *
     * @return the instance of {@link WATemplateUpdateEventConverter}
     */
    public static WATemplateUpdateEventConverter getInstance() {
        return InstanceHolder.WA_TEMPLATE_UPDATE_EVENT_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link WATemplateStatusUpdateEvent}
     */
    @Override
    public WATemplateStatusUpdateEvent convert(final JsonObject data) {
        final WATemplate.Status status = data.containsKey(WAFields.EVENT) ? WATemplate.Status.valueOf(data.getString(WAFields.EVENT)) : WATemplate.Status.PENDING;
        final WATemplateStatusUpdateEvent waTemplateStatusUpdateEvent = new WATemplateStatusUpdateEvent(status);

        waTemplateStatusUpdateEvent.setExternalTemplateId(data.getLong(WAFields.MESSAGE_TEMPLATE_ID));
        waTemplateStatusUpdateEvent.setName(data.getString(WAFields.MESSAGE_TEMPLATE_NAME));
        waTemplateStatusUpdateEvent.setLanguage(Language.getFromCode(data.getString(WAFields.MESSAGE_TEMPLATE_LANGUAGE)));

        return waTemplateStatusUpdateEvent;
    }
}