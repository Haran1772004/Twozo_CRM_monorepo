package com.tlc.crm.core.features.api.file.model.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.api.file.model.FileSourceMapper;
import com.tlc.crm.core.features.field.file.FileHolderFields;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Map;

/**
 * The handler which is responsible for processing {@link com.tlc.crm.core.api.deal.model.Deal}
 * related data in {@link FileHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class DealFileHandler implements FileSourceMapper {

    private final DealManager dealManager;

    private DealFileHandler() {
        this.dealManager = DealManager.getInstance();
    }

    public static DealFileHandler getInstance() {
        return Instance.INSTANCE;
    }

    private static class Instance {
        private static final DealFileHandler INSTANCE = new DealFileHandler();
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

        if (dealManager.exists(orgId, entityId)) {
            final Map<String, Long> contactAndCompanyIdsMap = dealManager.getDealContactAndCompanyIds(orgId, entityId);

            fileHolder.addRelatedEntityId(Source.DEAL.getName(), entityId);
            fileHolder.addRelatedEntityId(Source.CONTACT.getName(), contactAndCompanyIdsMap.get(FileHolderFields.CONTACT_ID.name()));
            fileHolder.addRelatedEntityId(Source.COMPANY.getName(), contactAndCompanyIdsMap.get(FileHolderFields.COMPANY_ID.name()));
        } else {
            throw ErrorCode.get(FileHolderErrorCodes.DEAL_NOT_FOUND);
        }
    }
}
