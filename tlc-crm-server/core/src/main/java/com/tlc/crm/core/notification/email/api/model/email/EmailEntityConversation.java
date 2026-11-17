package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.email.notification.email.EmailNotificationModel;

/**
 * Model which represents the {@link EmailEntityConversation}
 *
 * @author kishore J
 * @version 1.0
 */
public class EmailEntityConversation extends EntityConversation<EntityEmail> implements EmailNotificationModel {

    private final EntityEmail entityEmail;

    public EmailEntityConversation(final EntityEmail entityEmail) {
        this.entityEmail = entityEmail;
    }

    @Override
    public EntityEmail getConversableModel() {
        return entityEmail;
    }
}
