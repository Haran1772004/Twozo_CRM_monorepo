package com.tlc.email.server.action;

import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;

import com.tlc.email.server.fields.WebhookReceiverFields;
import com.tlc.notification.key.NotificationKeys;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Handles incoming webhook notifications from Outlook.
 * Provides Outlook-specific processing logic for email-related webhook events.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EmailWebHookAction
 */
@WebAction(path = "/webhook/outlook/notification")
public class OutlookWebhookAction extends EmailWebHookAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OutlookWebhookAction.class);

    public OutlookWebhookAction() {
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link EmailRequest}
     * @param response Represents the {@link EmailResponse}
     * @return true, if the validationToken returns 200 status code;otherwise false
     */
    @Override
    protected boolean isValid(final EmailRequest request, final EmailResponse response) {
        LOGGER.debug("Webhook Receiver: Incoming email webhook request received and validation initiated.");

        final String token = request.getParameter(WebhookReceiverFields.VALIDATION_TOKEN);

        if (Objects.nonNull(token)) {
            response.setStatus(HttpStatus.OK);
            response.setHeader(NotificationKeys.CONTENT_TYPE.getKey(), WebExchange.ContentType.PLAIN_TEXT.getName());
            response.setBody(token);

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EmailProvider}
     */
    @Override
    protected EmailProvider getProvider() {
        return EmailProvider.OUTLOOK;
    }
}
