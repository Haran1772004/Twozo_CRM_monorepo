package com.tlc.crm.core.internal.event.onboard.consumer;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.OrgOnboardFields;
import com.tlc.crm.entity.cache.internal.OrgOnboardingStatusCache;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Listens to org onboard events and delegates to {@link OrgOnboardingPerformer}.
 * Performs an idempotency check via {@link OrgOnboardingStatusCache} before delegating,
 * ensuring that duplicate messages across nodes are discarded.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class OrgOnboardingEventListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgOnboardingEventListener.class);

    private final OrgOnboardingPerformer orgOnboardingPerformer;
    private final OrgOnboardingStatusCache orgOnboardingStatusCache;

    public OrgOnboardingEventListener(final OrgOnboardingPerformer orgOnboardingPerformer) {
        this.orgOnboardingPerformer = orgOnboardingPerformer;
        this.orgOnboardingStatusCache = OrgOnboardingStatusCache.getInstance();
    }

    /**
     * <p>
     * Processes the incoming org onboard stream message.
     * Skips processing if onboarding has already been initiated for this org
     * (idempotency guard against duplicate consumption across nodes).
     * </p>
     *
     * @param streamMessage Represents the {@link StreamMessage}.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject payload = Json.object(streamMessage.data());
        final Long crmOrgId = payload.getLong(OrgOnboardFields.ORG_ID);

        if (orgOnboardingStatusCache.isOnboardingCompleted(crmOrgId)) {
            LOGGER.warn("Org onboard action: Duplicate message detected, skipping. CRM Org ID: {}", crmOrgId);
            streamMessage.clean();

            return;
        }
        orgOnboardingPerformer.perform(streamMessage, payload);
    }
}