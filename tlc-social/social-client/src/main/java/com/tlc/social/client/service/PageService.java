package com.tlc.social.client.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.model.SocialPage;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Facilitates services related to pages.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface PageService<T extends SocialPage> {

    /**
     * <p>
     * Fetches the pages info from the facebook and stores it into the database
     * </p>
     *
     * @param userAccessToken   Represents the user access token
     * @param pageStoreCallback Represents the callback function
     */
    void fetchAndStorePages(final String userAccessToken, final EventCallback<JsonObject> pageStoreCallback);

    /**
     * <p>
     * Fetches the fb pages by the provided page ids
     * </p>
     *
     * @param pageIds           Represents the collection of page ids
     * @param pageFetchCallback Represents page fetch callback function
     */
    void fetchPages(final Collection<Long> pageIds, final EventCallback<Map<Long, T>> pageFetchCallback);

    /**
     * <p>
     * Unsubscribes the pages from the webhook
     * </p>
     *
     * @param pageId                  Represents the page id.
     * @param pageUnsubscribeCallback Represents the page unsubscribe callback
     */
    void unsubscribePage(final Long pageId, final EventCallback<JsonObject> pageUnsubscribeCallback);
}
