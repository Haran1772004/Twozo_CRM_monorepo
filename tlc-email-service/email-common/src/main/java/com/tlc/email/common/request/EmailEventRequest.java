package com.tlc.email.common.request;

import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.AbstractEmailEvent;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.exchange.SyncEmailData;

/**
 * <p>
 * Represents a request to process an email webhook event.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailEventRequest extends SyncEmailData implements RequestCodec {

    private EmailReceiverConfiguration receiverConfiguration;
    private AbstractEmailEvent createEvent;
    private AbstractEmailEvent updateEvent;
    private EmailDeleteEvent deleteEvent;

    public EmailEventRequest() {
    }

    public EmailReceiverConfiguration getReceiverConfiguration() {
        return receiverConfiguration;
    }

    public AbstractEmailEvent getCreateEvent() {
        return createEvent;
    }

    public AbstractEmailEvent getUpdateEvent() {
        return updateEvent;
    }

    public EmailDeleteEvent getDeleteEvent() {
        return deleteEvent;
    }

    public void setReceiverConfiguration(final EmailReceiverConfiguration receiverConfiguration) {
        this.receiverConfiguration = receiverConfiguration;
    }

    public void setCreateEvent(final AbstractEmailEvent createEvent) {
        this.createEvent = createEvent;
    }

    public void setUpdateEvent(final AbstractEmailEvent updateEvent) {
        this.updateEvent = updateEvent;
    }

    public void setDeleteEvent(final EmailDeleteEvent deleteEvent) {
        this.deleteEvent = deleteEvent;
    }
}