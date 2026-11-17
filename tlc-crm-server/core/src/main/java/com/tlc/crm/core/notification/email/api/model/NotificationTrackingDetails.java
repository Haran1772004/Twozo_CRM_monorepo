package com.tlc.crm.core.notification.email.api.model;

import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail.Type;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * A model that represents the {@link NotificationTrackingDetails}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@Notifiable(types = {AppNotificationEventType.EMAIL_CLICKED, AppNotificationEventType.EMAIL_OPENED}, modelName = CrmModelType.TRACKING, needHandling = true)
public class NotificationTrackingDetails implements TlcModel {

    private Long id;
    private long orgId;
    private long userId;
    private String subject;
    private Type type;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public long getUserId() {
        return userId;
    }

    public String getSubject() {
        return subject;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}