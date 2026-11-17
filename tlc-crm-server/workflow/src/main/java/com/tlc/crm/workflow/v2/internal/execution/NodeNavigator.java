package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeEdge;

import java.util.Objects;

/**
 * Resolves the next graph node from edges only (any {@code nodeType} order).
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class NodeNavigator {

    private NodeNavigator() {
    }

    /**
     * Follows {@code NEXT} when {@code takeNext} is true, otherwise {@code FALLBACK}.
     *
     * @param node     current node
     * @param takeNext true for NEXT, false for FALLBACK
     * @return destination node id or null if the edge is missing
     */
    public static Long nextNodeId(final WorkflowNode node, final boolean takeNext) {
        if (Objects.isNull(node)) {
            return null;
        }
        final WorkflowNodeEdge edge = takeNext ? node.getNextEdge() : node.getFallbackEdge();

        return Objects.isNull(edge) ? null : edge.getToNodeId();
    }
}
