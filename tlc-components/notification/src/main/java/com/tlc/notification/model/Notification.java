package com.tlc.notification.model;

import com.tlc.notification.model.identity.ReceiverIdentity;
import com.tlc.notification.model.identity.SenderIdentity;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * <p>
 * The services that represents all the notification related models.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Notification<S extends SenderIdentity, R extends ReceiverIdentity> {

    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.sender.invalid")
    protected S sender;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.recipients.invalid")
    protected List<R> recipients;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.action.time.invalid")
    protected Long actionTime;

    public S getSender() {
        return sender;
    }

    public void setSender(final S sender) {
        this.sender = sender;
    }

    public List<R> getRecipients() {
        return recipients;
    }

    public void setRecipients(final List<R> recipients) {
        this.recipients = recipients;
    }

    public Long getActionTime() {
        return actionTime;
    }

    public void setActionTime(final Long actionTime) {
        this.actionTime = actionTime;
    }
}
