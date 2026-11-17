package com.tlc.crm.messenger.api.chat.model;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.messenger.validator.group.MessengerValidatorGroup;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.social.common.model.Chat;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * <p>
 * Adds crm related fields to {@link Chat}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
@NoArgsConstructor
@Notifiable(types = {AppNotificationEventType.CHAT_ASSIGNED, AppNotificationEventType.CHAT_INITIATED}, modelName = CrmModelType.CHAT, needHandling = true)
public class CrmChat extends Chat implements ConversableModel {

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {Group.Create.class, MessengerValidatorGroup.ChatUserAssignment.class, MessengerValidatorGroup.ChatUserUnAssignment.class}, message = "i18n.messenger.chat.org.id.is.required")
    private Long orgId;
    @NotNull(groups = {MessengerValidatorGroup.ChatUserAssignment.class, MessengerValidatorGroup.ChatUserUnAssignment.class}, message = "i18n.messenger.chat.id.is.null")
    private Long crmChatId;
    @NotNull(groups = {MessengerValidatorGroup.ChatUserAssignment.class}, message = "i18n.messenger.user.not.found")
    private CrmUser owner;
    private boolean isNewChat;
    /* Required for entity conversation */
    private Contact contact;

    public void setOwnerId(final Long ownerId) {
        this.owner = Objects.isNull(owner) ? new CrmUserEntity() : owner;

        owner.setId(ownerId);
    }

    public Long getOwnerId() {
        return Objects.isNull(owner) ? null : owner.id();
    }

    /**
     * {@inheritDoc}
     *
     * @return the org id
     */
    @Override
    public Long orgId() {
        return orgId;
    }
}