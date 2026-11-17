package com.tlc.crm.workflow.v2.action.history;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;
import com.tlc.crm.workflow.v2.api.model.converter.history.WorkflowV2ExecutionHistoryConverter;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import com.tlc.web.WebAction;

/**
 * V2 execution history get with the same JSON field names as V1.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
@WebAction(path = "/v2/workflow/execution/history/mgmt")
public final class WorkflowV2ExecutionHistoryManagement extends AuthorizationController {

    private final WorkflowV2ExecutionManager executionManager;
    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowV2ExecutionHistoryConverter historyConverter;

    public WorkflowV2ExecutionHistoryManagement() {
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.historyConverter = WorkflowV2ExecutionHistoryConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The CRM request.
     * @param crmResponse The CRM response.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(WorkflowAttributes.DATA);

        switch (requestJson.getString(WorkflowAttributes.TYPE)) {
            case "get" -> processHistory(orgId, data.getLong(WorkflowAttributes.ID), request, crmResponse);
        }
    }

    /**
     * Processes the history.
     *
     * @param orgId       The organization ID.
     * @param id          The ID.
     * @param crmRequest  The CRM request.
     * @param crmResponse The CRM response.
     */
    private void processHistory(final long orgId, final Long id, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        if (!executionManager.exists(orgId, id)) {
            throw ErrorCode.get(WorkflowErrorCodes.HISTORY_NOT_FOUND, "i18n.workflow.history.not.found");
        }
        final WorkflowV2Execution execution = executionManager.get(orgId, id);
        final WorkflowV2 workflow = workflowV2Manager.getByVersionId(orgId, execution.getVersionId());

        crmResponse.put(WorkflowAttributesV2.HISTORY, historyConverter.convert(execution, workflow, crmRequest.locale(), crmRequest.i18nResolver()));
    }
}
