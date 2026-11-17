package com.tlc.crm.core.features.action.call.model;

import com.tlc.crm.core.conversation.api.model.EntityConversation;

/**
 * Model which represents the {@link CallLogEntityConversation}
 *
 * @author kishore J
 * @version 1.0
 */
public class CallLogEntityConversation extends EntityConversation<CallLog> {

    private final CallLog callLog;

    public CallLogEntityConversation(final CallLog callLog){
        this.callLog = callLog;
    }

    @Override
    public CallLog getConversableModel() {
        return callLog;
    }
}
