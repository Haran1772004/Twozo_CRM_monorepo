package com.tlc.crm.entity.attachment.api.models;

import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.file.common.models.Attachment;
import com.tlc.l10n.temporal.model.DateTime;

/**
 * Represents an attachment that is associated with an entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class EntityAttachment extends Attachment {

    @SearchField(name = "id", readMethodName = "id")
    private Long id;

    @SearchField(name = "name")
    private String fileName;

    private Long orgId;
    private Long ownerId;
    private String extension;
    private String mediaType;
    private Long fileSize;
    private DateTime createdTime;
    private CrmUser owner;

    @Override
    public Long id() {
        return super.id();
    }

    public void setOwner(final CrmUser owner) {
        this.owner = owner;
    }

    public CrmUser getOwner() {
        return owner;
    }

    @Override
    public String getFileName() {
        return super.getFileName();
    }

    @Override
    public String getExtension() {
        return super.getExtension();
    }

    @Override
    public String getMediaType() {
        return super.getMediaType();
    }

    @Override
    public Long getFileSize() {
        return super.getFileSize();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public DateTime getCreatedTime() {
        return super.getCreatedTime();
    }

}
