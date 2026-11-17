package com.tlc.crm.common.iam.api.manager;

import com.tlc.crm.common.models.link.user.TemporaryUserLink;
import com.tlc.crm.common.models.link.user.TemporaryUserLink.Category;
import com.tlc.crm.common.iam.internal.TemporaryUserLinkManagerImpl;
import com.tlc.sql.common.api.DataContainer;

import java.util.Optional;

/**
 * <p>
 * Services that provides the user link related operations
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public interface TemporaryUserLinkManager {

    /**
     * <p>
     * Gets the {@link TemporaryUserLinkManager} instance
     * </p>
     *
     * @return The {@link TemporaryUserLinkManager} instance
     */
    static TemporaryUserLinkManager getInstance() {
        return TemporaryUserLinkManagerImpl.getInstance();
    }

    /**
     * <p>
     * Saves the temporary user link details into database
     * </p>
     *
     * @param temporaryUserLink Represents the {@link TemporaryUserLink} details
     */
    void save(final TemporaryUserLink temporaryUserLink);

    /**
     * <p>
     * Loads the temporary user link details into container
     * </p>
     *
     * @param temporaryUserLink Represents the {@link TemporaryUserLink} details
     * @param targetContainer  Represents the target container to load the details
     */
    void loadTemporaryLinkIntoContainer(final TemporaryUserLink temporaryUserLink,
                                        final DataContainer targetContainer);

    /**
     * <p>
     * Gets the {@link TemporaryUserLink} by link expiry id
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param expiryLinkId Represents the expiry link id
     * @return The {@link TemporaryUserLink} details
     */
    Optional<TemporaryUserLink> get(final long orgId, final long expiryLinkId);

    /**
     * <p>
     * Gets the {@link TemporaryUserLink} based on {@link Category}.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param category Represents the {@link Category}
     * @return The {@link  TemporaryUserLink} details
     */
    Optional<TemporaryUserLink> get(final long orgId, final long userId, final Category category);

    /**
     * <p>
     * Deletes the expiry link log.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param expiryLinkId Represents the expiry link id
     */
    void delete(final long orgId, final long expiryLinkId);
}
