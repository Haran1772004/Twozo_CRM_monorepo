package com.tlc.crm.common.models.link.user;

import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;

/**
 * <p>
 * Manages the forgot password functionality, including the management of password reset link expiration.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TemporaryUserLink
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.FORGOT_PASSWORD})
public final class ForgotPasswordLink extends TemporaryUserLink {

    @EmailAlertField(
            name = "creatorId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            placeholderTemplates = {EmailNotificationTemplate.FORGOT_PASSWORD}
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
        return Category.FORGOT_PASSWORD;
    }
}
