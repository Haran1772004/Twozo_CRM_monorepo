package com.tlc.crm.common.iam.internal;

import com.tlc.crm.common.iam.OrgInitiatorAction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * Holds the information about the registered org initiator actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public final class OrgInitiatorContainer {

    private final Map<String, Map<String, OrgInitiatorAction>> registeredActions;

    private OrgInitiatorContainer() {
        registeredActions = new LinkedHashMap<>();
    }

    private static class Instance {
        private static final OrgInitiatorContainer INSTANCE = new OrgInitiatorContainer();
    }

    /**
     * <p>
     * Gets the {@link OrgInitiatorContainer} Instance
     * </p>
     *
     * @return {@link OrgInitiatorContainer}
     */
    public static OrgInitiatorContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the crm org action into container
     * </p>
     *
     * @param orgInitiatorName   Represents the crm org initiator
     * @param orgInitiatorAction Holds org initiator instance
     */
    public void addAction(final String groupId, final String orgInitiatorName, final OrgInitiatorAction orgInitiatorAction) {
        registeredActions.computeIfAbsent(groupId, grpId -> new LinkedHashMap<>()).put(orgInitiatorName, orgInitiatorAction);
    }

    /**
     * <p>
     * Gets the crm org initiator instance
     * </p>
     *
     * @return The Collection of {@link OrgInitiatorAction}
     */
    public Collection<OrgInitiatorAction> getRegisteredActions() {
        return registeredActions.values().stream().flatMap(orgInitiatorActions -> orgInitiatorActions.values().stream()).toList();
    }

    /**
     * <p>
     * Removes the registered {@link OrgInitiatorAction} of the given group ID.
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
