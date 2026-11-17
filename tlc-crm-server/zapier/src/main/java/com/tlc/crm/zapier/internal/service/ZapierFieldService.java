package com.tlc.crm.zapier.internal.service;

import com.tlc.crm.zapier.api.models.ZapierField;

import java.util.Collection;

/**
 * Service interface for handling operations related to ZapierField
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ZapierFieldService {

    /**
     * Gets the {@link ZapierFieldService} instance.
     *
     * @return Instance of {@link ZapierFieldService}.
     */
    static ZapierFieldService getInstance() {
        return ZapierFieldServiceImpl.getInstance();
    }

    /**
     * Retrieves a collection of ZapierField objects associated with a specific action ID.
     *
     * @param id The action ID for which the fields are to be retrieved.
     * @return A collection of ZapierField objects for the specified action ID.
     */
    Collection<ZapierField> getFieldsByActionId(final Short id);

}
