package com.tlc.social.server.facebook.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.facebook.event.FBEvents;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.server.facebook.internal.service.FBPageTokenService;
import com.tlc.social.server.facebook.internal.service.FBService;
import com.tlc.social.server.facebook.internal.service.FBWebhookService;
import com.tlc.social.server.facebook.manager.FBPageManager;

import com.tlc.validator.type.Group;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A handler which listens to the facebook page retrieve and process event
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = FBEvents.RETRIEVE_AND_PROCESS_FB_PAGE)
public class FBPageDataHandler implements EventConsumer<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBPageDataHandler.class);

    private final FBPageManager fbPageManager;
    private final FBService fbService;
    private final FBWebhookService fbWebhookService;

    public FBPageDataHandler() {
        fbPageManager = FBPageManager.getInstance();
        fbService = FBService.getInstance();
        fbWebhookService = FBWebhookService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response in byte array
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final FBPageTokenService fbPageTokenService = FBPageTokenService.getInstance();
        final String accessToken = data.getString(FBFields.ACCESS_TOKEN);
        final Map<FBPage, String> pageMap = fbService.fetchPages(accessToken);
        final Collection<FBPage> pages = pageMap.keySet();

        if (pages.isEmpty()) {
            LOGGER.error("Facebook: No pages found. A Facebook account must have at least one Business page");

            response.put(FBFields.ARE_PAGES_FOUND, false);
        } else {
            final Collection<String> errors = SocialEntityValidator.validate(pages, Group.Create.class);

            if (errors.isEmpty()) {
                fbPageManager.storePages(pageMap);
                final Collection<Long> pageIds = new ArrayList<>();
                final long startedTime = System.currentTimeMillis();

                for (final FBPage fbPage : pages) {
                    final Long pageId = fbPage.id();

                    fbPageTokenService.put(pageId, pageMap.get(fbPage));
                    fbWebhookService.subscribe(fbPage);
                    pageIds.add(pageId);
                }
                LOGGER.info("Facebook: Business pages have been retrieved. Total pages: {}, Total time taken to subscribe: {}", pages.size(), System.currentTimeMillis() - startedTime);
                response.put(FBFields.PAGE_IDS, pageIds);
            } else {
                response.put(FBFields.ERRORS, errors);
            }
        }

        return response.getBytes();
    }
}
