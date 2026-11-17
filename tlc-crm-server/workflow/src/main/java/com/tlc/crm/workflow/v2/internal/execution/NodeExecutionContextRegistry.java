package com.tlc.crm.workflow.v2.internal.execution;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of live {@link NodeExecutionContext} instances keyed by execution id.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class NodeExecutionContextRegistry {

    private final Map<Long, NodeExecutionContext> contexts;
    private final NodeExecutionContextService nodeExecutionContextService;

    private NodeExecutionContextRegistry() {
        this.contexts = new ConcurrentHashMap<>();
        this.nodeExecutionContextService = NodeExecutionContextService.getInstance();
    }

    private static final class Instance {
        private static final NodeExecutionContextRegistry INSTANCE = new NodeExecutionContextRegistry();
    }

    /**
     * Gets the instance of the {@link NodeExecutionContextRegistry}.
     *
     * @return The instance of the {@link NodeExecutionContextRegistry}.
     */
    public static NodeExecutionContextRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Puts the {@link NodeExecutionContext} into the registry.
     *
     * @param executionId Represents the execution id.
     * @param context     Represents the {@link NodeExecutionContext}.
     */
    public void put(final long executionId, final NodeExecutionContext context) {
        contexts.put(executionId, context);
        nodeExecutionContextService.put(executionId, context);
    }

    /**
     * Gets the {@link NodeExecutionContext} from the registry.
     *
     * @param orgId       Represents the organization id.
     * @param executionId Represents the execution id.
     * @return The {@link NodeExecutionContext}.
     */
    public NodeExecutionContext get(final long orgId, final long executionId) {
        final NodeExecutionContext nodeExecutionContext = contexts.get(executionId);

        if (Objects.isNull(nodeExecutionContext)) {
            return nodeExecutionContextService.get(orgId, executionId);
        }
        return nodeExecutionContext;
    }

    /**
     * Removes the {@link NodeExecutionContext} from the registry.
     *
     * @param executionId Represents the execution id.
     * @return The {@link NodeExecutionContext}.
     */
    public NodeExecutionContext remove(final long executionId) {
        return contexts.remove(executionId);
    }

    /**
     * Checks if the {@link NodeExecutionContext} is present in the registry.
     *
     * @param executionId Represents the execution id.
     * @return true if the {@link NodeExecutionContext} is present in the registry, false otherwise.
     */
    public boolean contains(final long executionId) {
        return contexts.containsKey(executionId);
    }
}
