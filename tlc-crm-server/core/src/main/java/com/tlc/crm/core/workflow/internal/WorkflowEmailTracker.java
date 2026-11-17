package com.tlc.crm.core.workflow.internal;

import java.util.Collection;

/**
 * The {@code WorkflowEmailTracker} is responsible for tracking email actions.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 * @since 1.0.0
 */
public final class WorkflowEmailTracker {

    private Long id;
    private Long orgId;
    private Long userId;
    private Long executionId;
    private Long nodeId;
    private Long actionId;
    private boolean initialMessage;
    private boolean clicked;
    private short nodeType;
    private Long conversationId;
    private Long threadId;
    private Collection<Long> trackerIds;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(final Long executionId) {
        this.executionId = executionId;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public short getNodeType() {
        return nodeType;
    }

    public void setNodeType(final short nodeType) {
        this.nodeType = nodeType;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(final Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(final Long threadId) {
        this.threadId = threadId;
    }

    public Collection<Long> getTrackerIds() {
        return trackerIds;
    }

    public void setTrackerIds(final Collection<Long> trackerIds) {
        this.trackerIds = trackerIds;
    }

    public boolean isInitialMessage() {
        return initialMessage;
    }

    public void setInitialMessage(final boolean initialMessage) {
        this.initialMessage = initialMessage;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(final boolean clicked) {
        this.clicked = clicked;
    }
}
