package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.handler.ActionHandler;

import com.tlc.validator.TlcModel;

/**
 * The handler which is responsible for processing workflow actions across multiple entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
interface SourceActionHandler extends ActionHandler {

    /**
     * Administers the create event of an entity associated with the workflow action.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The created {@link TlcModel}.
     */
    TlcModel create(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext);

    /**
     * Administers the update event of an entity associated with the workflow action.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The updated {@link TlcModel}.
     */
    TlcModel update(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext);
}
