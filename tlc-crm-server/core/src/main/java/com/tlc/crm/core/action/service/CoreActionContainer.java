package com.tlc.crm.core.action.service;

import com.tlc.crm.core.action.service.annotation.CoreAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the registration and retrieval of core mail services
 *
 * @author Kishore J
 * @version 1.0
 */
public final class CoreActionContainer {

    private final Map<String, Map<CoreActionType, CoreActionService>> registeredActions;

    private CoreActionContainer() {
        this.registeredActions = new HashMap<>();
    }

    private static class Instance {
        private static final CoreActionContainer INSTANCE = new CoreActionContainer();
    }

    /**
     * <p>
     * Gets the {@link CoreActionContainer} instance
     * </p>
     *
     * @return The {@link CoreActionContainer}
     */
    public static CoreActionContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds a {@link CoreAction} and its corresponding {@link CoreActionService}
     * </p>
     *
     * @param groupId           Represents the group id.
     * @param coreAction        The core action service to be added
     * @param coreActionService Represents the {@link CoreActionService}
     */
    public void add(final String groupId, final CoreAction coreAction, final CoreActionService coreActionService) {
        registeredActions.computeIfAbsent(groupId, grpId -> new HashMap<>()).put(coreAction.name(), coreActionService);
    }

    /**
     * <p>
     * Fetches the core action service for given the specified name
     * </p>
     *
     * @param coreActionType Represents the {@link CoreActionType}
     * @return An {@link Optional} containing the core action service if found, otherwise an empty {@link Optional}
     */
    public <T> Optional<T> get(final CoreActionType coreActionType) {
        return (Optional<T>) registeredActions.values().stream().map(registerActions -> registerActions.get(coreActionType)).findAny();
    }

    /**
     * <p>
     * Removes the registered {@link CoreActionService} of the given group ID.
     * </p>
     *
     * @param groupId Represents the group ID.
     */
    public void remove(final String groupId) {
        registeredActions.remove(groupId);
    }

    /**
     * <p>
     * Removes all registered core action services.
     * </p>
     */
    public void clearAll() {
        registeredActions.clear();
    }
}
