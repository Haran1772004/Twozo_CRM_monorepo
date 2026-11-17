package com.tlc.cdc.core.event;

import com.tlc.cdc.core.internal.status.CdcErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry that maintains and provides access to CDC aggregated handlers.
 *
 * <p>The registry organizes aggregated handlers by type and key, allowing the appropriate {@link CdcEventHandler} to be resolved and
 * executed for a given CDC processing flow (e.g., analytics, event, notification).</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcEventHandlerRegistry {

    private final Map<String, Map<String, CdcEventHandler>> aggregatedHandlers;

    private CdcEventHandlerRegistry() {
        this.aggregatedHandlers = new LinkedHashMap<>();
    }

    private static class Instance {
        private static final CdcEventHandlerRegistry INSTANCE = new CdcEventHandlerRegistry();
    }

    /**
     * Gets the {@link CdcEventHandlerRegistry} instance.
     *
     * @return {@link CdcEventHandlerRegistry}.
     */
    public static CdcEventHandlerRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Adds the CDC aggregated action into the registry.
     *
     * @param groupId         Represents the module name.
     * @param eventName       Refers the event name.
     * @param cdcEventHandler Refers the {@code CdcEventHandler}.
     */
    public void addAction(final String groupId, final String eventName, final CdcEventHandler cdcEventHandler) {
        final Map<String, CdcEventHandler> groupHandlers = aggregatedHandlers.computeIfAbsent(groupId, grpId -> new LinkedHashMap<>());

        if (groupHandlers.containsKey(eventName)) {
            throw ErrorCode.get(CdcErrorCodes.CDC_HANDLER_ALREADY_EXISTS,
                    "A CDC handler for event '" + eventName + "' is already registered under module '" + groupId + "'");
        }
        groupHandlers.put(eventName, cdcEventHandler);
    }

    /**
     * Gets the collection of {@code CdcEventHandler}s across all registered modules.
     *
     * @return The collection of {@code CdcEventHandler}.
     */
    public Collection<CdcEventHandler> getAll() {
        return aggregatedHandlers.values().stream().flatMap(actions -> actions.values().stream()).toList();
    }

    /**
     * Removes the registered {@link CdcEventHandler} of the given group ID.
     *
     * @param groupId Represents the group ID.
     */
    public void remove(final String groupId) {
        aggregatedHandlers.remove(groupId);
    }
}
