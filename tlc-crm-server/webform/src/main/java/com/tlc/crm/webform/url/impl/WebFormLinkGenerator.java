package com.tlc.crm.webform.url.impl;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.webform.url.UrlGenerator;
import com.tlc.web.HttpUtil;

import java.util.Base64;
import java.util.Map;

/**
 * Handles the url generation of {@link com.tlc.crm.webform.api.model.WebForm}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class WebFormLinkGenerator implements UrlGenerator {

    private String baseUrl;

    private WebFormLinkGenerator() {
    }

    private static class Instance {
        private static final WebFormLinkGenerator INSTANCE = new WebFormLinkGenerator();
    }

    /**
     * Gets instance of {@link WebFormLinkGenerator}.
     *
     * @return {@link Instance}.
     */
    public static WebFormLinkGenerator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Sets the base url for the webForm generation.
     *
     * @param configurations Refers the {@link Map} which has the base url details.
     */
    public void setBaseUrl(final Configurations configurations) {
        this.baseUrl = configurations.getConfig("redirectUrl.render");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an org id
     * @param ownerId Represents an user id
     * @param formId  Refers the id of the webForm.
     * @return The url of the webForm.
     */
    @Override
    public String getUrl(final long orgId, final long ownerId, final Long formId) {
        return buildEncodedUrl(orgId, ownerId, formId, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an org id
     * @param ownerId Represents an user id
     * @param formId  Represents and form id
     * @return Encoded URL suitable for iframe embedding
     */
    @Override
    public String getIframeUrl(final long orgId, final long ownerId, final Long formId) {
        return buildEncodedUrl(orgId, ownerId, formId, true);
    }

    /**
     * <p>
     * Builds a fully encoded and absolute URL by serializing the input parameters into a
     * JSON structure, Base64 encoding it, and appending it to the base URL.
     * </p>
     *
     * @param orgId    Represents an org id
     * @param ownerId  Represents an user id
     * @param formId   Represents and form id
     * @param isIframe Flag indicating whether the URL is for iframe usage
     * @return A Base64-encoded absolute URL string containing the serialized form data
     */
    private String buildEncodedUrl(final long orgId, final long ownerId, final Long formId, final boolean isIframe) {
        return HttpUtil.buildAbsoluteUrl(String.format("%s%s", baseUrl,
                Base64.getEncoder().encodeToString(String.format("{\"orgId\":\"%s\",\"ownerId\":\"%s\",\"id\":\"%s\",\"isIframe\":\"%b\"}", orgId, ownerId, formId, isIframe).getBytes())));
    }
}