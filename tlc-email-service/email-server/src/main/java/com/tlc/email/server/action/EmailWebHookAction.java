package com.tlc.email.server.action;

import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.action.PublicAction;

import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;
import com.tlc.email.server.fields.WebhookReceiverFields;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import com.tlc.web.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles email webhook callbacks from different providers.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see PublicAction
 */
public abstract class EmailWebHookAction extends PublicAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(EmailWebHookAction.class);

    private final EmailEventPublisher emailEventPublisher;

    public EmailWebHookAction() {
        this.emailEventPublisher = EmailEventPublisher.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception
     */
    @Override
    public void pProcess(final EmailRequest request, final EmailResponse response) {
        final JsonObject requestJson = request.getRequestJson();

        LOGGER.debug("Webhook Receiver: Incoming email webhook request received.");

        if (!isValid(request, response)) {
            LOGGER.info("Webhook Receiver: Received email webhook request. Beginning payload processing.");

            requestJson.put(WebhookReceiverFields.PROVIDER_ID, getProvider().getId());

            emailEventPublisher.publishWebhookNotification(requestJson.toString());
        }

        response.setStatus(HttpStatus.OK);
        response.send();
    }

    /**
     * Cheks whether the token is valid or not.
     *
     * @param request  Represents the {@link EmailRequest}
     * @param response Represents the {@link EmailResponse}
     * @return true, if the request is valid;otherwise false
     */
    protected abstract boolean isValid(final EmailRequest request, final EmailResponse response);

    /**
     * Gets the {@link EmailProvider}.
     *
     * @return {@link EmailProvider}
     */
    protected abstract EmailProvider getProvider();
}
