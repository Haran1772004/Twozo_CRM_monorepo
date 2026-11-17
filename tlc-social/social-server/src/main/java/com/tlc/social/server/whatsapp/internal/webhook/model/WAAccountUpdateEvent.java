package com.tlc.social.server.whatsapp.internal.webhook.model;

import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;

/**
 * A model which represents the WhatsApp account update event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAAccountUpdateEvent extends WebhookMessageEvent {

    private Long externalWABusinessAccountId;
    private String displayPhoneNumber;
    private UpdateEvent updateEvent;

    public WAAccountUpdateEvent() {
        super(MessageEventType.WA_ACCOUNT_UPDATE);
    }

    public Long getExternalWABusinessAccountId() {
        return externalWABusinessAccountId;
    }

    public void setExternalWABusinessAccountId(final Long externalWABusinessAccountId) {
        this.externalWABusinessAccountId = externalWABusinessAccountId;
    }

    public String getDisplayPhoneNumber() {
        return displayPhoneNumber;
    }

    public void setDisplayPhoneNumber(final String displayPhoneNumber) {
        this.displayPhoneNumber = displayPhoneNumber;
    }

    public UpdateEvent getUpdateEvent() {
        return updateEvent;
    }

    public void setUpdateEvent(final UpdateEvent updateEvent) {
        this.updateEvent = updateEvent;
    }

    /**
     * An enum which represents the Whatsapp business account update events
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum UpdateEvent {
        PHONE_NUMBER_REMOVED,
    }
}