package com.tlc.email.server.model;

import com.tlc.email.common.model.config.EmailConfiguration;

/**
 * Handles email data retrieved from Outlook. Extends the base functionality to accommodate
 * Outlook-specific attributes and processing.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailProviderDataStore
 */
public class OutlookDataStore extends EmailProviderDataStore {

    private String url;

    public OutlookDataStore(final String accessToken, final EmailConfiguration emailReceiverConfiguration) {
        super(accessToken, emailReceiverConfiguration);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
