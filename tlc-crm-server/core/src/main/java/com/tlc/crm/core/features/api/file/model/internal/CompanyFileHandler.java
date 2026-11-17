package com.tlc.crm.core.features.api.file.model.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.file.model.FileSourceMapper;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

/**
 * The handler which is responsible for processing {@link com.tlc.crm.core.api.contact.model.Company}
 * related data in {@link FileHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class CompanyFileHandler implements FileSourceMapper {

    private final CompanyManager companyManager;

    private CompanyFileHandler() {
        this.companyManager = CompanyManager.getInstance();
    }

    public static CompanyFileHandler getInstance() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final CompanyFileHandler INSTANCE = new CompanyFileHandler();
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

        if (companyManager.exists(orgId, entityId)) {
            fileHolder.addRelatedEntityId(Source.COMPANY.getName(), entityId);
        } else {
            throw ErrorCode.get(FileHolderErrorCodes.COMPANY_NOT_FOUND);
        }
    }
}
