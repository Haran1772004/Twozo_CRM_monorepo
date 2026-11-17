package com.tlc.crm.messenger.api.conversation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

/**
 * <p>
 * Pertains to conversation contents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    private Long orgId;
    private Collection<CrmMessage> crmMessages;
    private Collection<ConversationActivity> conversationActivities;
}