package com.tlc.social.server.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.server.handler.SocialEventHandler;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.model.MessageSendRequest;

/**
 * Defines an abstract representation of the outbound services
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class SocialOutboundService {

    protected final ChatManager chatManager;
    protected final SocialEventHandler socialEventHandler;

    protected SocialOutboundService() {
        chatManager = ChatManager.getInstance();
        socialEventHandler = SocialEventRequestService.get().getRequestHandler();
    }

    /**
     * <p>
     * Sends the message to the recipient
     * </p>
     *
     * @param messageSendRequest Represents the {@link MessageSendRequest}
     * @return the response in json
     */
    public abstract JsonObject send(final MessageSendRequest messageSendRequest);
}
