package com.tlc.crm.entity.models.settings;

import com.tlc.notification.model.email.EmailAddress;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Provides the services for various sync settings
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface SyncSettings extends TlcModel {

    /**
     * <p>
     * Gets the synced address
     * </p>
     *
     * @return the synced {@link EmailAddress}
     */
    EmailAddress getAddress();

    /**
     * <p>
     * Sets the synced address
     * </p>
     *
     * @param address Represents the synced {@link EmailAddress}
     */
    void setAddress(final EmailAddress address);

    /**
     * <p>
     * Fetches the user id
     * </p>
     *
     * @return the user id
     */
    Long getUserId();

    /**
     * <p>
     * Sets the user id given
     * </p>
     *
     * @param userId Represents the user id
     */
    void setUserId(final Long userId);

    /**
     * <p>
     * Sets the org id given
     * </p>
     *
     * @param orgId Represents the org id
     */
    void setOrgId(final Long orgId);

    /**
     * <p>
     * Sets the sync settings id
     * </p>
     *
     * @param id Represents the sync settings id
     */
    void setId(final Long id);

    /**
     * <p>
     * Gets provider.
     * </p>
     *
     * @return provider.
     */
    String getProvider();
}
