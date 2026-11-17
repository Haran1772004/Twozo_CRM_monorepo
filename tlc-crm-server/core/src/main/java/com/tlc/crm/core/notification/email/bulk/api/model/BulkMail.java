package com.tlc.crm.core.notification.email.bulk.api.model;

import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;

import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;

/**
 * Represents the {@link BulkMail} that implements the {@link TlcModel} and encapsulates
 * the information about bulk email messages and their associated data.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TlcModel
 */
public class BulkMail implements EmailNotificationModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.notifications.bulk.mail.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.bulk.mail.invalid.sender.id")
    private Long senderId;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notifications.mail.invalid")
    @Valid
    private Collection<MailInfo> mailInfo;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.notifications.bulk.mail.invalid.source")
    private Source source;
    private boolean hasDrafts;
    private Long updatedTime;
    private String subject;
    private String content;
    private Collection<MailInfo> subscribedMailInfo;
    private Long totalRecipientsCount;
    private EmailAddress fromAddress;
    private Metrics metrics;
    private boolean hasScheduledConversation;
    private Long threadId;

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

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
    }

    public Collection<MailInfo> getMailInfo() {
        return mailInfo;
    }

    public void setMailInfo(final Collection<MailInfo> mailInfo) {
        this.mailInfo = mailInfo;
    }

    public boolean hasDrafts() {
        return hasDrafts;
    }

    public void setHasDrafts(final boolean hasDrafts) {
        this.hasDrafts = hasDrafts;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public Collection<MailInfo> getSubscribedMailInfo() {
        return subscribedMailInfo;
    }

    public void setSubscribedMailInfo(final Collection<MailInfo> subscribedMailInfo) {
        this.subscribedMailInfo = subscribedMailInfo;
    }

    public Long getTotalRecipientsCount() {
        return totalRecipientsCount;
    }

    public void setTotalRecipientsCount(final Long totalRecipientsCount) {
        this.totalRecipientsCount = totalRecipientsCount;
    }

    public EmailAddress getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(final EmailAddress fromAddress) {
        this.fromAddress = fromAddress;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(final Metrics metrics) {
        this.metrics = metrics;
    }

    public boolean hasScheduledConversation() {
        return hasScheduledConversation;
    }

    public void setHasScheduledConversation(final boolean hasScheduledConversation) {
        this.hasScheduledConversation = hasScheduledConversation;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(final Long threadId) {
        this.threadId = threadId;
    }
}
