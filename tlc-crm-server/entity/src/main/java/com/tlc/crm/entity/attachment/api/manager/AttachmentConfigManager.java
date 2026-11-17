package com.tlc.crm.entity.attachment.api.manager;

import com.tlc.file.common.models.Attachable;
import com.tlc.file.common.models.Attachment;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Map;

/**
 * Manages the attachment details save action.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AttachmentConfigManager<T extends Attachable> {

    /**
     * Saves the attachment details into the database.
     *
     * @param model Which represents the respective Tlc model.
     * @return The response of an respective Tlc model with identifier.
     */
    public abstract Map<String, Object> save(final T model);

    /**
     * Returns the {@link OrgDataStore}.
     *
     * @param orgId Organization id.
     * @return {@link OrgDataStore}.
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Deletes the entity by orgId.
     *
     * @param orgId       Organization id.
     * @param attachments Collection of attachment.
     */
    public abstract void delete(final Long orgId, final Collection<Attachment> attachments);
}