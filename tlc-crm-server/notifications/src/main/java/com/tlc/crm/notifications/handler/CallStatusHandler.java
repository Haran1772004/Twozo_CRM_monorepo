package com.tlc.crm.notifications.handler;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.notification.webhook.model.CallStatusWebhookData;

/**
 * Defines operations for handling call status webhook events from telephony providers.
 *
 * @author Sathish
 * @version 1.0
 */
public interface CallStatusHandler {

    /**
     * Returns the default instance of {@link CallStatusHandler}.
     *
     * @return the {@link CallStatusHandler} instance
     */
    static CallStatusHandler get(final TelephoneProvider telephoneProvider) {
        return switch (telephoneProvider) {
            case TELE_CMI -> TeleCMICallStatusHandler.getInstance();
            case M_CUBE -> MCubeCallStatusHandler.getInstance();
            default -> DefaultCallStatusHandler.getInstance();
        };
    }

    /**
     * Processes web-based call status updates (non click-to-call).
     * Handles child call events by looking up the parent call and updating
     * or creating child call records accordingly.
     *
     * @param webhookData the {@link CallStatusWebhookData} containing call event details
     * @param crmResponse the {@link CrmResponse} for error reporting
     */
    void handleWebCall(final CallStatusWebhookData webhookData, final CrmResponse crmResponse);

    /**
     * Handles the click-to-call action based on the provided call status and webhook data.
     *
     * @param webhookData the {@link CallStatusWebhookData} containing call event details
     * @param crmResponse the {@link CrmResponse} for error reporting
     */
    void handleClickToCall(final CallStatusWebhookData webhookData, final CrmResponse crmResponse);
}

