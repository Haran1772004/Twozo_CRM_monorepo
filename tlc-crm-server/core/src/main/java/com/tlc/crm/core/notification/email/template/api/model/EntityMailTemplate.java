package com.tlc.crm.core.notification.email.template.api.model;

import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.validator.annotation.EntityMailTemplateValidator;
import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.email.client.template.MailTemplate;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * A model that represents a template of email in crm
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@EntityMailTemplateValidator(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.email.template.validation.failed")
public class EntityMailTemplate extends MailTemplate implements EmailNotificationModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotEmpty(groups = Group.Create.class, message = "i18n.email.template.invalid.name")
    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.email.template.name.max.length")
    private String name;

    @NotEmpty(groups = Group.Create.class, message = "i18n.email.template.invalid.content")
    private String content;

    private Long createdTime;

    @Size(max = 200, groups = {Group.Create.class, Group.Update.class}, message = "i18n.email.template.subject.max.length")
    private String subject;

    private Source source;

    private Long entityId;

    private Collection<Attachment> attachments;

    private TemplateMetrics templateMetrics;

    @Size(max = 5, groups = {Group.Create.class, Group.Update.class}, message = "i18n.template.tag.max.size")
    private Collection<TemplateTag> templateTags;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.template.folder.invalid.id")
    private Long folderId;

    private Set<Long> attachmentIds;

    private final MailPlaceHolderExtractor placeHolderExtractor;

    public EntityMailTemplate() {
        placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;

        setSubjectHasPlaceHolders(placeHolderExtractor.hasPlaceHolders(subject));
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;

        setContentHasPlaceHolders(placeHolderExtractor.hasPlaceHolders(content));
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public void setAttachments(final Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    public void addAttachment(final Attachment attachment) {
        this.attachments = Objects.isNull(this.attachments) ? new ArrayList<>() : this.attachments;

        this.attachments.add(attachment);
    }

    public void setTemplateTags(final Collection<TemplateTag> templateTags) {
        this.templateTags = templateTags;
    }

    public Collection<TemplateTag> getTemplateTags() {
        return templateTags;
    }

    public void setFolderId(final Long folderId) {
        this.folderId = folderId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public TemplateMetrics getTemplateMetrics() {
        return templateMetrics;
    }

    public void setTemplateMetrics(final TemplateMetrics templateMetrics) {
        this.templateMetrics = templateMetrics;
    }

    public Set<Long> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(final Set<Long> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }

    @Override
    public Object identity() {
        return null;
    }
}