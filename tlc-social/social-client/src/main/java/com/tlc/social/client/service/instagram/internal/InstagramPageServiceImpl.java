package com.tlc.social.client.service.instagram.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.client.service.instagram.InstagramPageService;
import com.tlc.social.common.instagram.event.InstagramEvents;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Facilitates services related to Instagram page.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class InstagramPageServiceImpl implements InstagramPageService {

    private InstagramPageServiceImpl() {
    }

    private static class InstanceHolder {
        private static final InstagramPageServiceImpl INSTANCE = new InstagramPageServiceImpl();
    }

    /**
     * <p>
     * Gets {@link InstagramPageServiceImpl}.
     * </p>
     *
     * @return {@link InstagramPageServiceImpl}.
     */
    public static InstagramPageServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param userAccessToken   Represents the user access token.
     * @param pageStoreCallback Represents the callback function.
     */
    @Override
    public void fetchAndStorePages(final String userAccessToken, final EventCallback<JsonObject> pageStoreCallback) {
        final Event<byte[]> pageStoreEvent = new Event<>(InstagramEvents.RETRIEVE_AND_PROCESS_INSTAGRAM_PAGES,
                Json.object().put(FBFields.ACCESS_TOKEN, userAccessToken).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                pageStoreCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                pageStoreCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(pageStoreEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param pageIds           Represents the collection of page ids.
     * @param pageFetchCallback Represents the page fetch callback function.
     */
    @Override
    public void fetchPages(final Collection<Long> pageIds, final EventCallback<Map<Long, InstagramPage>> pageFetchCallback) {
        final Event<byte[]> pageFetchEvent = new Event<>(InstagramEvents.FETCH_INSTAGRAM_PAGES,
                Json.object().put(FBFields.PAGE_IDS, pageIds).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Map<Long, InstagramPage> pageMap = new HashMap<>();
                final JsonObject data = Json.object(bytes);
                final JsonArray pageArray = data.getJsonArray(FBFields.PAGES);
                final int pageSize = pageArray.size();

                for (int index = NumericalConstant.ZERO; index < pageSize; index++) {
                    final JsonObject pageJson = pageArray.getJsonObject(index);
                    final InstagramPage instagramPage = new InstagramPage();

                    instagramPage.setId(pageJson.getLong(FBFields.ID));
                    instagramPage.setName(pageJson.getString(FBFields.NAME));

                    pageMap.put(instagramPage.id(), instagramPage);
                }
                pageFetchCallback.onSuccess(pageMap);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                pageFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(pageFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId                  Represents the page ids
     * @param pageUnsubscribeCallback Represents the page unsubscribe callback
     */
    @Override
    public void unsubscribePage(final Long pageId, final EventCallback<JsonObject> pageUnsubscribeCallback) {
        final Event<byte[]> pageUnsubscribeEvent = new Event<>(InstagramEvents.UNSUBSCRIBE_INSTAGRAM_PAGES,
                Json.object().put(FBFields.PAGE_ID, pageId).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                pageUnsubscribeCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(Throwable throwable) {
                pageUnsubscribeCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(pageUnsubscribeEvent, eventCallback);
    }
}