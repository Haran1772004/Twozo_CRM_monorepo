package com.tlc.crm.notifications.api.call.model;

import com.tlc.crm.core.conversation.api.model.EntityConversation;

/**
 * Model which represents the {@link CallDetailsEntityConversation}
 *
 * @author kishore J
 * @version 1.0
 */
public class CallDetailsEntityConversation extends EntityConversation<CallDetails> {

    private final CallDetails callDetails;

    public CallDetailsEntityConversation(final CallDetails callDetails) {
        this.callDetails = callDetails;
    }

    @Override
    public CallDetails getConversableModel() {
        return callDetails;
    }
}
