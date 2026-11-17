package com.tlc.email.server.action;

import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

/**
 * <p>
 * Handles incoming webhook notifications from Gmail.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/webhook/google/notification", method = HttpMethod.POST)
public class GoogleWebhookAction extends EmailWebHookAction {

    public GoogleWebhookAction() {
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link EmailRequest}
     * @param response Represents the {@link EmailResponse}
     * @return true, if the request is valid;otherwise false
     */
    @Override
    protected boolean isValid(final EmailRequest request, final EmailResponse response) {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EmailProvider}
     */
    @Override
    protected EmailProvider getProvider() {
        return EmailProvider.GOOGLE;
    }
}