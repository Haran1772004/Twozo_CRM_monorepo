package com.tlc.email.common.model;

import java.util.Objects;

/**
 * <p>
 * Enum representing different types of email background jobs for publishing requests.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.1
 */
public enum SyncJobType {

    /**
     * Represents a sync job that retrieves email data from a source.
     */
    SYNC,

    /**
     * Represents a team inbox job that retrieves email data from a source.
     */
    TEAM_INBOX,

    /**
     * Represents a smart BCC job, used for email synchronization or processing for BCC recipients.
     */
    SMART_BCC,

    /**
     * Represents an auto-sync job type.
     */
    AUTO_SYNC,
    ;

    /**
     * <p>
     * Checks if the current job type is {@link #AUTO_SYNC}.
     * </p>
     *
     * @return {@code true} if this job type is auto sync, {@code false} otherwise
     */
    public boolean isAutoSync() {
        return Objects.equals(this, AUTO_SYNC);
    }

    /**
     * <p>
     * Checks if the current job type is {@link #SYNC}.
     * </p>
     *
     * @return {@code true} if this job type is sync-based, {@code false} otherwise
     */
    public boolean isSyncType() {
        return Objects.equals(this, SYNC);
    }
}