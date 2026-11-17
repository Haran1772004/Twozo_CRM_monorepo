package com.tlc.email.server.sync;

import java.util.Objects;

/**
 * <p>
 * A model that represents the email sync token
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public record EmailSyncTokenKey(Long orgId, String emailAddress, String folderId) {

    /**
     * {@inheritDoc}
     *
     * @param object the reference object with which to compare.
     * @return
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (Objects.isNull(object) || getClass() != object.getClass()) {
            return false;
        }
        final EmailSyncTokenKey that = (EmailSyncTokenKey) object;

        return orgId.equals(that.orgId) && emailAddress.equals(that.emailAddress) && folderId.equals(that.folderId);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(orgId, emailAddress, folderId);
    }
}