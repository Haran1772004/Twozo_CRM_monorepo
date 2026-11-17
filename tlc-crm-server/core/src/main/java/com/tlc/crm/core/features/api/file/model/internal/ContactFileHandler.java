package com.tlc.crm.core.features.api.file.model.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.file.model.FileSourceMapper;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

/**
 * The handler which is responsible for processing {@link Contact} related data in {@link FileHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ContactFileHandler implements FileSourceMapper {

    private final ContactManager contactManager;

    private ContactFileHandler() {
        this.contactManager = ContactManager.getInstance();
    }

    public static ContactFileHandler getInstance() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final ContactFileHandler INSTANCE = new ContactFileHandler();
    }

    /**
     * {@inheritDoc}
     *
     * @param fileHolder Refers the {@link FileHolder} which holds the attachment details.
     * @param orgId      Refers the organization id.
     * @param entityId   Refers the entity id.
     */
    @Override
    public void mapRelatedEntityId(final FileHolder fileHolder, final long orgId, final long entityId) {

        if (contactManager.exists(orgId, entityId)) {
            fileHolder.addRelatedEntityId(Source.CONTACT.getName(), entityId);
            fileHolder.addRelatedEntityId(Source.COMPANY.getName(), contactManager.getContactCompanyId(entityId, orgId));
        } else {
            throw ErrorCode.get(FileHolderErrorCodes.CONTACT_NOT_FOUND);
        }
    }
}
