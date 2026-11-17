package com.tlc.sync.service.oauth.model;

/**
 * <p>
 * Provides the url config for sync action
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class SyncUrlConfig {

    private String redirectUrl;

    private String consentUrl;

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(final String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getConsentUrl() {
        return consentUrl;
    }

    public void setConsentUrl(final String consentUrl) {
        this.consentUrl = consentUrl;
    }
}