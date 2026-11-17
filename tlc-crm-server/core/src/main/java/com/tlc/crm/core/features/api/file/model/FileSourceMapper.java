package com.tlc.crm.core.features.api.file.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.features.api.file.model.internal.CompanyFileHandler;
import com.tlc.crm.core.features.api.file.model.internal.ContactFileHandler;
import com.tlc.crm.core.features.api.file.model.internal.DealFileHandler;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

/**
 * The Mapper which maps the related source mapping of an {@link FileHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface FileSourceMapper {

    /**
     * Gets the instance of {@link FileSourceMapper} by the source provided.
     *
     * @param source Refers the source.
     * @return The {@link FileSourceMapper} instance of respective source.
     */
    static FileSourceMapper get(final Source source) {
        return switch (source) {
            case CONTACT -> ContactFileHandler.getInstance();
            case COMPANY -> CompanyFileHandler.getInstance();
            case DEAL -> DealFileHandler.getInstance();
            default -> throw ErrorCode.get(FileHolderErrorCodes.SOURCE_NOT_FOUND);
        };
    }

    /**
     * Maps the related entity id into {@link FileHolder}.
     *
     * @param fileHolder Refers the {@link FileHolder} which holds the attachment details.
     * @param orgId      Refers the organization id.
     * @param entityId   Refers the entity id.
     */
    void mapRelatedEntityId(final FileHolder fileHolder, final long orgId, final long entityId);
}