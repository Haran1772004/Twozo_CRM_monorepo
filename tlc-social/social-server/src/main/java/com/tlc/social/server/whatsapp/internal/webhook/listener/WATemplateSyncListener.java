package com.tlc.social.server.whatsapp.internal.webhook.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.whatsapp.internal.model.WATemplateResponse;
import com.tlc.social.server.whatsapp.internal.service.WAService;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.validator.type.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A listener which listens to the WhatsApp template sync event. It fetches and stores the templates
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateSyncListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WATemplateSyncListener.class);

    private final WABusinessManager waBusinessManager;
    private final WAService waService;

    public WATemplateSyncListener() {
        waBusinessManager = WABusinessManager.getInstance();
        waService = WAService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject data = Json.object(streamMessage.data());
        final WABusinessAccount waBusinessAccount = new WABusinessAccount();
        final Long waBusinessAccountId = data.getLong(WAFields.BUSINESS_ACCOUNT_ID);
        final Long externalWABusinessAccountId = data.getLong(WAFields.EXTERNAL_WABA_ID);

        waBusinessAccount.setId(waBusinessAccountId);
        waBusinessAccount.setExternalWABAId(externalWABusinessAccountId);

        try {
            waBusinessManager.updateSyncStatus(waBusinessAccountId, WABusinessAccount.TemplateSyncStatus.IN_PROGRESS, null);
            getAndStoreApprovedTemplates(waBusinessAccount, waService.getWAApprovedTemplateRetrieveUrl(externalWABusinessAccountId));
            waBusinessManager.updateSyncStatus(waBusinessAccountId, WABusinessAccount.TemplateSyncStatus.SYNCED, Instant.now().toEpochMilli());
        } catch (Throwable throwable) {
            waBusinessManager.updateSyncStatus(waBusinessAccountId, WABusinessAccount.TemplateSyncStatus.FAILED, null);
            LOGGER.error("Whatsapp: Failed to fetch and store templates. Business account id:{}, Reason:{}, Stack:{}", waBusinessAccountId, throwable.getMessage(), throwable);
        } finally {
            streamMessage.clean();
        }
    }

    /**
     * <p>
     * Gets the approved templates and store them into the database
     * </p>
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     * @param url               Represents the url to retrieve templates
     */
    private void getAndStoreApprovedTemplates(final WABusinessAccount waBusinessAccount, final String url) {
        final Long waBusinessAccountId = waBusinessAccount.id();
        final WATemplateResponse waTemplateResponse = waService.getApprovedTemplates(waBusinessAccount, url);
        final String nextUrl = waTemplateResponse.nextUrl();
        final Collection<WATemplate> templates = waTemplateResponse.templates();

        if (Objects.nonNull(templates) && !templates.isEmpty()) {
            final Collection<String> errors = SocialEntityValidator.validate(templates, Group.Create.class);

            if (errors.isEmpty()) {
                waBusinessManager.storeTemplates(waBusinessAccount, templates);
                LOGGER.info("Whatsapp: Templates fetched and stored successfully. Business account id: {}, Total templates:{}", waBusinessAccountId, templates.size());
            } else {
                LOGGER.error("Whatsapp: Failed to store templates. Data validation failed. Business account id: {}, Reason: {}", waBusinessAccountId, errors.stream().findFirst().get());
            }
        }

        if (Objects.isNull(nextUrl)) {
            return;
        }
        getAndStoreApprovedTemplates(waBusinessAccount, nextUrl);
    }
}