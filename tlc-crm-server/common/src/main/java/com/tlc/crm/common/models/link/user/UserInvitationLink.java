package com.tlc.crm.common.models.link.user;

import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;

/**
 * <p>
 * Handles the expiration details related to user invitations.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TemporaryUserLink
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.USER_INVITATION})
public final class UserInvitationLink extends TemporaryUserLink {

    @EmailAlertField(
            name = "creatorId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            placeholderTemplates = {EmailNotificationTemplate.USER_INVITATION}
    )
    private long creatorId;

    public long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(final long creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * <p>
     * Gets the {@link Category}.
     * </p>
     *
     * @return {@link Category}
     */
    public Category getCategory() {
        return Category.INVITE;
    }
}
