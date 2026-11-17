package com.tlc.crm.workflow.v2.api.model;

import java.util.Collection;

/**
 * Support class for workflow nodes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowNodeSupport {

    private WorkflowNodeSupport() {
    }

    /**
     * Gets the trigger configuration for a workflow node.
     *
     * @param node the workflow node
     * @return the trigger configuration
     */
    public static TriggerConfiguration triggerConfiguration(final WorkflowNode node) {
        return configurationAs(node, TriggerConfiguration.class);
    }

    /**
     * Gets the condition configuration for a workflow node.
     *
     * @param node the workflow node
     * @return the condition configuration
     */
    public static ConditionConfiguration conditionConfiguration(final WorkflowNode node) {
        return configurationAs(node, ConditionConfiguration.class);
    }

    /**
     * Gets the if/else configuration for a workflow node.
     *
     * @param node the workflow node
     * @return the if/else configuration
     */
    public static IfElseConfiguration ifElseConfiguration(final WorkflowNode node) {
        return configurationAs(node, IfElseConfiguration.class);
    }

    /**
     * Gets the wait-for-condition configuration for a workflow node.
     *
     * @param node the workflow node
     * @return the wait-for-condition configuration
     */
    public static WaitForConditionConfiguration waitForConditionConfiguration(final WorkflowNode node) {
        return configurationAs(node, WaitForConditionConfiguration.class);
    }

    /**
     * Gets the action configuration for a workflow node.
     *
     * @param node the workflow node
     * @return the action configuration
     */
    public static ActionConfiguration actionConfiguration(final WorkflowNode node) {
        return configurationAs(node, ActionConfiguration.class);
    }

    /**
     * Gets the delay configuration for a workflow node.
     *
     * @param node the workflow node
     * @return the delay configuration
     */
    public static DelayConfiguration delayConfiguration(final WorkflowNode node) {
        return configurationAs(node, DelayConfiguration.class);
    }

    /**
     * Gets the configuration for a workflow node.
     *
     * @param node the workflow node
     * @param type the type of configuration
     * @return the configuration
     */
    private static <T extends NodeConfiguration> T configurationAs(final WorkflowNode node, final Class<T> type) {
        final NodeConfiguration configuration = node.getConfiguration();

        return type.isInstance(configuration) ? type.cast(configuration) : null;
    }

    /**
     * Gets the configuration for a collection of workflow nodes.
     *
     * @param nodes the collection of workflow nodes
     * @param type  the type of configuration
     * @return the configuration
     */
    public static <T extends NodeConfiguration> T configurationAs(final Collection<WorkflowNode> nodes, final Class<T> type) {
        for (final WorkflowNode node : nodes) {
            final NodeConfiguration configuration = node.getConfiguration();

            if (type.isInstance(configuration)) {
                return type.cast(configuration);
            }
        }
        return null;
    }
}
