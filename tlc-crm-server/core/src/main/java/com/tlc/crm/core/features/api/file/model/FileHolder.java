package com.tlc.crm.core.features.api.file.model;

import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.file.common.models.Attachable;
import com.tlc.file.common.validator.Group.Upload;

import com.tlc.crm.common.action.search.annotation.SearchField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A Model that represents the {@link FileHolder} which holds the entity attachment details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Searchable(index = SearchIndex.FILE)
public class FileHolder implements Attachable {

    private Long id;

    @NotNull(groups = {Upload.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    private Long userId;

    @SearchField(name = "attachments")
    @NotEmpty(groups = {Upload.class}, message = "i18n.fileHolder.invalid.attachment")
    @Valid
    private Collection<EntityAttachment> attachments;

    @SearchField(name = "entityId")
    @NotNull(groups = {Upload.class}, message = "i18n.fileHolder.invalid.entity.id")
    private Long entityId;

    @SearchField(name = "entitySource")
    @NotNull(groups = {Upload.class}, message = "i18n.fileHolder.invalid.entity.source")
    private String source;

    @SearchField(name = "relatedEntityIds")
    private Map<String, Long> relatedEntityIds;

    public Collection<EntityAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(final Collection<EntityAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(final EntityAttachment entityAttachment) {
        this.attachments = Objects.isNull(this.attachments) ? new ArrayList<>() : this.attachments;

        this.attachments.add(entityAttachment);
    }

    public Map<String, Long> getRelatedEntityIds() {
        return relatedEntityIds;
    }

    public void setRelatedEntityIds(final Map<String, Long> relatedEntityIds) {
        this.relatedEntityIds = relatedEntityIds;
    }

    public void addRelatedEntityId(final String source, final Long id) {
        this.relatedEntityIds = Objects.isNull(this.relatedEntityIds) ? new HashMap<>() : this.relatedEntityIds;

        this.relatedEntityIds.put(source, id);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }
}