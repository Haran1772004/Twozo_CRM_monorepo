package com.tlc.crm.zapier.internal.event.processor;

import com.tlc.crm.zapier.api.manager.ZapEventWebhookManager;
import com.tlc.crm.zapier.api.models.ZapEventWebhook;
import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.crm.zapier.internal.event.ZapEventRequest;
import com.tlc.crm.zapier.internal.event.ZapEventRequestSender;

import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Abstract base implementation of {@link ZapEventProcessor} that provides a common structure
 * for handling Zapier event processing for various model types.
 *
 * @param <T> the type of model being processed (e.g., Contact, Deal, etc.)
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractZapEventProcessor<T> implements ZapEventProcessor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractZapEventProcessor.class);

    protected final I18nService i18nService;
    private final ZapEventWebhookManager zapEventWebhookManager;
    private final ZapEventRequestSender zapEventRequestSender;

    protected AbstractZapEventProcessor() {
        this.i18nService = I18nServiceProvider.get();
        this.zapEventWebhookManager = ZapEventWebhookManager.getInstance();
        this.zapEventRequestSender = ZapEventRequestSender.getInstance();
    }

    /**
     * Handles the given {@link ZapEventData} by processing and dispatching the
     * event and serves as the main entry point for processing a Zapier-triggered event.
     *
     * @param zapEventData The event data containing context and model information to be processed.
     */
    protected final void handle(final ZapEventData zapEventData) {
        final Collection<ZapEventWebhook> zapEventWebhooks = zapEventWebhookManager.getAllSubscribedHookUrl(zapEventData.getOrgId(),
                zapEventData.getUserId(), zapEventData.getZapEvent());

        zapEventRequestSender.send(new ZapEventRequest(zapEventWebhooks, zapEventData.getEventPayloads()));
    }
}
