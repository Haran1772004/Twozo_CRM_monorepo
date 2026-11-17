package com.tlc.notification.model.sync;

import com.tlc.commons.code.ErrorCode;
import com.tlc.i18n.I18nKey;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * Represents the status for synchronization tasks.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum SyncStatus {

    /**
     * Indicates that the synchronization task is new.
     */
    NEW((short) 1),

    /**
     * Indicates that the synchronization task is currently in progress.
     */
    IN_PROGRESS((short) 2),

    /**
     * Indicates that the synchronization task has been successfully completed.
     */
    SYNCED((short) 3),

    /**
     * Indicates that the synchronization task has encountered a failure.
     */
    FAILED((short) 4),

    /**
     * Indicates that the synchronization task has added in the queue.
     */
    IN_QUEUE((short) 5),

    /**
     * Indicates that the synchronization task has been stopped.
     */
    STOPPED((short) 6),
    ;

    private final short id;

    SyncStatus(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the id of the {@link SyncStatus}.
     * </p>
     *
     * @return the id
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the status of {@link SyncStatus}.
     * </p>
     *
     * @param id Represents the syncStatus id
     * @return {@link SyncStatus}
     */
    public static SyncStatus getStatus(final short id) {
        return switch (id) {
            case 1 -> NEW;
            case 2 -> IN_PROGRESS;
            case 3 -> SYNCED;
            case 4 -> FAILED;
            case 5 -> IN_QUEUE;
            case 6 -> STOPPED;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_SYNC_STATUS,
                    "i18n.sync.status.invalid");
        };
    }

    /**
     * <p>
     * Provides a mapping between synchronization statuses and their respective state values.
     * When the synchronization status is set to {@code NEW}, {@code FAILED} or {@code STOPPED}, returns <b>Authorized</b>
     * If the status is {@code IN_PROGRESS}, returns <b>Syncing</b>
     * For {@code SYNCED} or {@code IN_QUEUE} status, returns <b>Active</b>
     * </p>
     *
     * @param syncStatus Represents the {@link SyncStatus}
     * @return the {@link I18nKey} state value
     */
    public static I18nKey getStateValue(final SyncStatus syncStatus) {
        return switch (syncStatus) {
            case NEW, FAILED, STOPPED -> new I18nKey("i18n.sync.state.authorized");
            case IN_PROGRESS -> new I18nKey("i18n.sync.state.in.progress");
            case SYNCED, IN_QUEUE -> new I18nKey("i18n.sync.state.active");
        };
    }

    /**
     * <p>
     * Checks whether the synchronization status is synced.
     * </p>
     *
     * @return true if the status is {@code SYNCED}, false otherwise
     */
    public boolean isSynced() {
        return this == SYNCED;
    }

    /**
     * <p>
     * Checks whether the synchronization status is queued.
     * </p>
     *
     * @return true if the status is {@code QUEUED}, false otherwise
     */
    public boolean isQueued() {
        return this == IN_QUEUE;
    }

    /**
     * <p>
     * Checks whether the synchronization status is active.
     * </p>
     *
     * @return true if the status is active, false otherwise
     */
    public boolean isActive() {
        return isSynced() || isQueued();
    }

    /**
     * <p>
     * Checks whether the synchronization status is in progress.
     * </p>
     *
     * @return true if the status is {@code IN_PROGRESS}, false otherwise
     */
    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    /**
     * <p>
     * Checks whether the synchronization status is stopped.
     * </p>
     *
     * @return true if the status is {@code STOPPED}, false otherwise
     */
    public boolean isStopped() {
        return this == STOPPED;
    }
}