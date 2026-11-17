package com.tlc.crm.notifications.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.notification.model.Notification;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents the app notification
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class AppNotification extends Notification<NotificationUser, NotificationUser> implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Update.class}, message = "i18n.app.notification.org.invalid.id")
    private long orgId;

    @NotNull(groups = {Group.Update.class}, message = "i18n.app.notification.user.invalid.id")
    private long userId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.app.notification.invalid")
    private String notification;

    private Boolean isRead;

    @NotNull(groups = {Group.Create.class}, message = "i18n.app.notification.type.invalid")
    private AppNotificationEventType type;

    @NotNull(groups = {Group.Update.class}, message = "i18n.app.notification.invalid.state")
    private String state;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setNotification(final String notification) {
        this.notification = notification;
    }

    public Boolean isRead() {
        return isRead;
    }

    public void setRead(final boolean read) {
        isRead = read;
    }

    public AppNotificationEventType getType() {
        return type;
    }

    public void setType(final AppNotificationEventType type) {
        this.type = type;
    }

    public JsonObject getNotificationAsJson() {
        return Objects.isNull(notification) ? Json.object() : Json.object(notification);
    }

    public String getNotification() {
        return notification;
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

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public long getUserId() {
        return userId;
    }

    public String getState() {
        return state;
    }
}
