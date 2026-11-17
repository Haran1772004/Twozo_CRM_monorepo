package com.tlc.social.client.service.facebook.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.client.service.facebook.FBPageService;
import com.tlc.social.common.facebook.event.FBEvents;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.model.FBPage;

import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the {@link FBPageService} which provides client services related to facebook page
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBPageServiceImpl implements FBPageService {

    private FBPageServiceImpl() {
    }

    private static class InstanceHolder {
        private static final FBPageServiceImpl INSTANCE = new FBPageServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FBPageServiceImpl}
     * </p>
     *
     * @return the instance of {@link FBPageServiceImpl}
     */
    public static FBPageServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param userAccessToken   Represents the user access token
     * @param pageStoreCallback Represents the callback function
     */
    @Override
    public void fetchAndStorePages(final String userAccessToken, final EventCallback<JsonObject> pageStoreCallback) {
        final Event<byte[]> pageStoreEvent = new Event<>(FBEvents.RETRIEVE_AND_PROCESS_FB_PAGE, Json.object().put(FBFields.ACCESS_TOKEN, userAccessToken).getBytes());
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
     * @param pageIds           Represents the collection of page ids
     * @param pageFetchCallback Represents the page fetch callback function
     */
    @Override
    public void fetchPages(final Collection<Long> pageIds, final EventCallback<Map<Long, FBPage>> pageFetchCallback) {
        final Event<byte[]> pageFetchEvent = new Event<>(FBEvents.FETCH_FB_PAGES, Json.object().put(FBFields.PAGE_IDS, pageIds).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Map<Long, FBPage> pageMap = new HashMap<>();
                final JsonObject data = Json.object(bytes);
                final JsonArray pageArray = data.getJsonArray(FBFields.PAGES);
                final int pageSize = pageArray.size();

                for (int index = 0; index < pageSize; index++) {
                    final JsonObject pageJson = pageArray.getJsonObject(index);
                    final FBPage fbPage = new FBPage();

                    fbPage.setId(pageJson.getLong(FBFields.ID));
                    fbPage.setName(pageJson.getString(FBFields.NAME));

                    pageMap.put(fbPage.id(), fbPage);
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
        final Event<byte[]> pageUnsubscribeEvent = new Event<>(FBEvents.UNSUBSCRIBE_FB_PAGES, Json.object().put(FBFields.PAGE_ID, pageId).getBytes());
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
