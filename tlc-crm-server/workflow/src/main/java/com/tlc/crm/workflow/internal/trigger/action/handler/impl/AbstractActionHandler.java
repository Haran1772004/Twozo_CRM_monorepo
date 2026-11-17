package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.ActionHandler;

import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;
import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * An abstract class designed to handle workflow actions and supports indexing entity data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractActionHandler implements ActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractActionHandler.class);
    private final IndexDataPublisher indexDataPublisher;
    private final WorkflowManager workflowManager;

    protected final TriggerModelHandler triggerModelHandler;
    private final WorkflowV2ExecutionManager executionManager;

    protected AbstractActionHandler() {
        this.indexDataPublisher = IndexDataPublisher.getInstance();
        this.workflowManager = WorkflowManager.getInstance();
        this.triggerModelHandler = TriggerModelHandler.getInstance();
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
    }

    /**
     * Publishes index data for the specified organization ID, model, and action.
     *
     * @param orgId    The ID of the organization for which the data is indexed.
     * @param tlcModel The model data that needs to be indexed.
     * @param action   The action triggering this indexing operation.
     */
    protected final void index(final long orgId, final TlcModel tlcModel, final Action action) {
        try {
            indexDataPublisher.publishIndexData(orgId, List.of(tlcModel), action);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Workflow Execution: Failed to index model. Id: {}, Org Id: {}", tlcModel.id(), orgId);
        }
    }

    /** Loads the field audit info for the given entity based on the workflow action.
     *
     * @param orgId            The ID of the organization.
     * @param workflowActionId The ID of the workflow action being executed.
     * @param entity           The entity for which the field edit context is to be loaded.
     */
    protected void loadFieldAuditInfo(final Long orgId, final Long workflowActionId, final Entity entity) {
        final Workflow workflow = workflowManager.getByActionId(orgId, workflowActionId);

        entity.setFieldAuditInfo(new FieldAuditInfo(FieldAuditSource.WORKFLOW, workflow.id(), Json.object()
                .put(ActionAttributes.NAME, workflow.getName())
                .put(ActionAttributes.IS_ACTIVE, true)
        ));
    }

    /**
     * Loads the field audit info for the given entity based on the workflow action.
     *
     * @param orgId        The ID of the organization.
     * @param workflowId   The ID of the workflow.
     * @param workflowName Refers the workflow name.
     * @param entity       The entity for which the field edit context is to be loaded.
     */
    protected final void loadFieldAuditInfo(final Long orgId, final Long workflowId, final String workflowName, final Entity entity) {
        entity.setFieldAuditInfo(new FieldAuditInfo(FieldAuditSource.WORKFLOW, workflowId, Json.object()
                .put(ActionAttributes.NAME, workflowName)
                .put(ActionAttributes.IS_ACTIVE, true)
        ));
    }

    /**
     * Inserts the source entity for the given node execution context, model, and action configuration.
     *
     * @param nodeExecutionContext Represents the {@link NodeExecutionContext}.
     * @param tlcModel             Represents the {@link TlcModel}.
     * @param actionConfiguration  Represents the {@link ActionConfiguration}.
     */
    protected final void insertSourceEntity(final NodeExecutionContext nodeExecutionContext, final TlcModel tlcModel, final ActionConfiguration actionConfiguration) {
        final String source = actionConfiguration.getTriggerActionSource().getAliasName();
        final ExecutionEntity.ExecutionSource executionSource = ExecutionEntity.ExecutionSource.get(source);
        final JsonObject entityJson;

        if (Objects.nonNull(executionSource) && ExecutionEntity.ExecutionSource.EMAIL == executionSource) {
            entityJson = Json.object();
        } else {
            entityJson = triggerModelHandler.getPriorStateJson(Source.get(source), tlcModel);
        }
        executionManager.upsertSourceEntity(new ExecutionEntity(nodeExecutionContext.orgId(), nodeExecutionContext.executionId(), nodeExecutionContext.nodeId(), tlcModel.id(), entityJson, executionSource));
    }

}
