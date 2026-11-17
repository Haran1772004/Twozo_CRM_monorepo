package com.tlc.crm.core.notification.email.alert.model;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * The {@link EmailAlertInfo} class is designed to store and manage email alert configurations
 * for a specific organization. It contains mappings of various email alert
 * template to their corresponding user ids and recipients
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailAlertInfo {

    private long orgId;
    private Map<EmailNotificationTemplate, Collection<Long>> templateUserIds;
    private TlcModel tlcModel;
    private Map<EmailNotificationTemplate, Map<String, Collection<String>>> templateRecipients;
    private Collection<EmailNotificationTemplate> userEnabledEmailTemplates;
    private Map<EmailNotificationTemplate, Map<String, Collection<Long>>> templateRecipientUserIds;

    public Collection<EmailNotificationTemplate> getUserEnabledEmailTemplates() {
        return userEnabledEmailTemplates;
    }

    public void setUserEnabledEmailTemplates(final Collection<EmailNotificationTemplate> userEnabledEmailTemplates) {
        this.userEnabledEmailTemplates = userEnabledEmailTemplates;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public TlcModel getTlcModel() {
        return tlcModel;
    }

    public void setTlcModel(final TlcModel tlcModel) {
        this.tlcModel = tlcModel;
    }

    public Map<String, Collection<String>> getRecipients(final EmailNotificationTemplate template) {
        return Objects.isNull(templateRecipients) ? Collections.emptyMap() : templateRecipients.get(template);
    }

    public void addRecipient(final EmailNotificationTemplate template, final String fieldName, final String recipient) {
        this.templateRecipients = Objects.isNull(this.templateRecipients) ? new HashMap<>() : this.templateRecipients;

        templateRecipients.computeIfAbsent(template, recipients -> new HashMap<>()).computeIfAbsent(fieldName, recipients -> new HashSet<>()).add(recipient);
    }

    public Map<EmailNotificationTemplate, Map<String, Collection<Long>>> getTemplateRecipientUserIds() {
        return templateRecipientUserIds;
    }

    public void addTemplateRecipientUserIds(final EmailNotificationTemplate template, final String fieldName, final Collection<Long> receiverIds) {
        this.templateRecipientUserIds = Objects.isNull(this.templateRecipientUserIds) ? new HashMap<>() : this.templateRecipientUserIds;

        templateRecipientUserIds.computeIfAbsent(template, userIds -> new HashMap<>()).computeIfAbsent(fieldName, fieldReceiverIds -> new HashSet<>()).addAll(receiverIds);
    }

    public void addTemplatesUserIds(final EmailNotificationTemplate template, final Collection<Long> userIds) {
        this.templateUserIds = Objects.isNull(this.templateUserIds) ? new HashMap<>() : this.templateUserIds;

        templateUserIds.computeIfAbsent(template, templatesUserIds -> new HashSet<>()).addAll(userIds);
    }

    public Map<EmailNotificationTemplate, Collection<Long>> getTemplateUserIds() {
        return templateUserIds;
    }
}
