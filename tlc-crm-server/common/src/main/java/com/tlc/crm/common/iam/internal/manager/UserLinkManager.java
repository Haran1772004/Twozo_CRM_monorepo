package com.tlc.crm.common.iam.internal.manager;

import com.tlc.validator.TlcModel;

/**
 * Provides the service for managing user linking and unlinking operations
 *
 * @author KarthickV
 * @version 1.0
 */
public interface UserLinkManager<T extends TlcModel> {

    /**
     * <p>
     * Links users to the respective entity
     * </p>
     *
     * @param model Holds the details of the model.
     */
    void linkUsers(final T model);

    /**
     * <p>
     * Unlink users to the respective entity
     * </p>
     *
     * @param model Holds the details of the model.
     */
    void unlinkUsers(final T model);
}
