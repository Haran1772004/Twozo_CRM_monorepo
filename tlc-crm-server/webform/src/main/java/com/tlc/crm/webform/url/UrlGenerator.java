package com.tlc.crm.webform.url;

import com.tlc.crm.webform.url.impl.WebFormLinkGenerator;

/**
 * The generator that generates the url for {@link com.tlc.crm.webform.api.model.WebForm}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface UrlGenerator {

    /**
     * Gets the instance of {@link UrlGenerator}.
     *
     * @return {@link UrlGenerator} instance.
     */
    static UrlGenerator get() {
        return WebFormLinkGenerator.getInstance();
    }

    /**
     * Gets the absolute url by constructing with provided form id.
     *
     * @param orgId   Represents an org id
     * @param ownerId Represents an user id
     * @param formId  Refers the id of the webForm.
     * @return The url of the webForm.
     */
    String getUrl(final long orgId, final long ownerId, final Long formId);

    /**
     * <p>
     * Generates the iframe URL for embedding the form
     * </p>
     *
     * @param orgId   Represents an org id
     * @param ownerId Represents an user id
     * @param formId  Represents and form id
     * @return Encoded URL suitable for iframe embedding
     */
    String getIframeUrl(final long orgId, final long ownerId, final Long formId);
}
