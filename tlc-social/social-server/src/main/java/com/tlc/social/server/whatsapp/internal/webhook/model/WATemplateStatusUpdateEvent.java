package com.tlc.social.server.whatsapp.internal.webhook.model;

import com.tlc.commons.language.Language;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;

/**
 * A model which represents the WhatsApp template status update event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateStatusUpdateEvent extends WebhookMessageEvent {

    private final WATemplate.Status status;
    private Long externalTemplateId;
    private Long externalWABusinessAccountId;
    private String name;
    private Language language;

    public WATemplateStatusUpdateEvent(final WATemplate.Status status) {
        super(MessageEventType.TEMPLATE_UPDATE);
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getExternalTemplateId() {
        return externalTemplateId;
    }

    public void setExternalTemplateId(final Long externalTemplateId) {
        this.externalTemplateId = externalTemplateId;
    }

    public Long getExternalWABusinessAccountId() {
        return externalWABusinessAccountId;
    }

    public void setExternalWABusinessAccountId(final Long externalWABusinessAccountId) {
        this.externalWABusinessAccountId = externalWABusinessAccountId;
    }

    public WATemplate.Status getStatus() {
        return status;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(final Language language) {
        this.language = language;
    }
}