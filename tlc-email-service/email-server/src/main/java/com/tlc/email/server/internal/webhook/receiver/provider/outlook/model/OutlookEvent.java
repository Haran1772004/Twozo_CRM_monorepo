package com.tlc.email.server.internal.webhook.receiver.provider.outlook.model;

import java.util.Collection;

/**
 * Represents the webhook notification.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class OutlookEvent {

    private Collection<OutlookEventNotification> value;
    private Collection<String> validationTokens;

    public Collection<OutlookEventNotification> getValue() {
        return value;
    }

    public void setValue(final Collection<OutlookEventNotification> value) {
        this.value = value;
    }

    public Collection<String> getValidationTokens() {
        return validationTokens;
    }

    public void setValidationTokens(final Collection<String> validationTokens) {
        this.validationTokens = validationTokens;
    }
}