package com.tlc.crm.common.iam.internal;

import com.tlc.crm.common.iam.UserDataTransferAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Holds the information about the registered user transfer action
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class DataMigrationContainer {

    private final Map<String, Map<String, UserDataTransferAction>> registeredActions;

    private DataMigrationContainer() {
        registeredActions = new HashMap<>();
    }

    private static class Instance {
        private static final DataMigrationContainer INSTANCE = new DataMigrationContainer();
    }

    /**
     * <p>
     * Gets the {@link DataMigrationContainer} Instance
     * </p>
     *
     * @return {@link DataMigrationContainer}
     */
    public static DataMigrationContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the user migrator action into container
     * </p>
     *
     * @param userMigratorName    Represents the user migrator name
     * @param userAction          Holds the instance of user data transfer action
     */
    public void addAction(final String groupId, final String userMigratorName, final UserDataTransferAction userAction) {
        registeredActions.computeIfAbsent(groupId, grpId -> new HashMap<>()).put(userMigratorName, userAction);
    }

    /**
     * <p>
     * Gets the user transfer instance
     * </p>
     *
     * @return The Collection of {@link UserDataTransferAction} instance
     */
    public Collection<UserDataTransferAction> getRegisteredActions() {
        return registeredActions.values().stream().flatMap(orgInitiatorActions -> orgInitiatorActions.values().stream()).toList();
    }

    /**
     * <p>
     * Removes the registered {@link UserDataTransferAction} of the given group ID.
     * </p>
     *
     * @param groupId Represents the group ID.
     */
    public void remove(final String groupId) {
        registeredActions.remove(groupId);
    }

    /**
     * Removes all registered actions across all groups.
     */
    public void clearAll() {
        registeredActions.clear();
    }
}
