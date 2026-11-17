package com.tlc.crm.core.notification.email.api.model;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;

import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.email.common.model.Thread;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Represents the entity thread.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EntityThread extends Thread implements EmailNotificationModel {

    private Long orgId;
    private Long userId;
    private Long entityId;
    private Source source;
    private Collection<EntityEmail> entityEmails;
    private Long crmId;

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Collection<EntityEmail> getEntityEmails() {
        return entityEmails;
    }

    public void setEntityEmails(final Collection<EntityEmail> entityEmails) {
        this.entityEmails = entityEmails;
    }

    public Long getCrmId() {
        return crmId;
    }

    public void setCrmId(final Long crmId) {
        this.crmId = crmId;
    }

    /**
     * <p>
     * Fetches the mail of the thread
     * </p>
     *
     * @return {@link EntityEmail}
     */
    public final <T extends EntityEmail> T getComposedEmail() {
        final Collection<EntityEmail> mails = getEntityEmails();

        if (Objects.nonNull(mails) && !mails.isEmpty()) {
            return (T) mails.stream().filter(Objects::nonNull).findFirst().orElseGet(() -> null);
        }

        return null;
    }

    /**
     * <p>
     * Fetches the scheduled mail of the thread
     * </p>
     *
     * @return {@link EntityEmail}
     */
    public EntityScheduledEmail getScheduledMail() {
        final Collection<EntityEmail> mails = getEntityEmails();

        if (Objects.nonNull(mails) && !mails.isEmpty()) {

            for (final EntityEmail mail : mails) {

                if (mail instanceof EntityScheduledEmail scheduledMail) {
                    return scheduledMail;
                }
            }
        }

        return null;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
