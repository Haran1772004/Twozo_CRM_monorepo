package com.tlc.social.server.instagram.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.event.InstagramEvents;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.server.instagram.internal.service.InstagramPageTokenService;
import com.tlc.social.server.instagram.internal.service.InstagramService;
import com.tlc.social.server.instagram.internal.service.InstagramWebhookService;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
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
 * Handles instagram page data.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@EventListener(name = InstagramEvents.RETRIEVE_AND_PROCESS_INSTAGRAM_PAGES)
public class InstagramPageDataHandler implements EventConsumer<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramPageDataHandler.class);

    private final InstagramPageManager instagramPageManager;
    private final InstagramService instagramService;
    private final InstagramWebhookService instagramWebhookService;
    private final InstagramPageTokenService instagramPageTokenService;

    public InstagramPageDataHandler() {
        instagramPageManager = InstagramPageManager.getInstance();
        instagramService = InstagramService.getInstance();
        instagramWebhookService = InstagramWebhookService.getInstance();
        instagramPageTokenService = InstagramPageTokenService.getInstance();
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
        final String accessToken = data.getString(FBFields.ACCESS_TOKEN);
        final Map<InstagramPage, String> pageMap = instagramService.fetchPages(accessToken);
        final Collection<InstagramPage> pages = pageMap.keySet();

        if (pages.isEmpty()) {
            LOGGER.error("Instagram: No Instagram Business accounts found. The connected Facebook account must have at least one Business Page and one linked Instagram account");

            response.put(FBFields.ARE_PAGES_FOUND, false);
        } else {
            final Collection<String> errors = SocialEntityValidator.validate(pages, Group.Create.class);

            if (errors.isEmpty()) {
                instagramPageManager.storePages(pageMap);
                final Collection<Long> pageIds = new ArrayList<>();
                final long startedTime = System.currentTimeMillis();

                for (final InstagramPage instagramPage : pages) {
                    final Long pageId = instagramPage.id();

                    instagramPageTokenService.putLinkedPageToken(pageId, pageMap.get(instagramPage));
                    instagramWebhookService.subscribe(instagramPage);
                    pageIds.add(pageId);
                }
                LOGGER.info("Instagram: Instagram business accounts have been retrieved. Total accounts: {}, Total time taken to subscribe: {}", pages.size(), System.currentTimeMillis() - startedTime);
                response.put(FBFields.PAGE_IDS, pageIds);
            } else {
                response.put(FBFields.ERRORS, errors);
            }
        }

        return response.getBytes();
    }
}
