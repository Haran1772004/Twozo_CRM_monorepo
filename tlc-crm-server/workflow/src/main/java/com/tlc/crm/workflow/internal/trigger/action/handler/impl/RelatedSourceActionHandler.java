package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.handler.ActionHandler;

import com.tlc.validator.TlcModel;

/**
 * Interface representing an action handler that processes actions related to mapping a source
 * to a corresponding entity model.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
interface RelatedSourceActionHandler extends ActionHandler {

    /**
     * Maps a related source to a corresponding {@link TlcModel} for the given organization and user.
     *
     * @param orgId                  The ID of the organization associated with the source.
     * @param userId                 The ID of the user requesting the mapping.
     * @param source                 The source object to be mapped.
     * @param workflowAction         Refers the workflow action.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return {@link TlcModel} representing the mapped data related to the provided source.
     */
    TlcModel mapRelatedSource(final Long orgId, final Long userId, final Source source,
                              final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext);
}
