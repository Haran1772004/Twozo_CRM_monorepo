package com.tlc.crm.workflow.action.history;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.workflow.resolver.internal.WorkflowCriteriaResolver;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.manager.history.WorkflowExecutionHistoryManager;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionHistory;
import com.tlc.crm.workflow.api.model.converter.history.WorkflowExecutionHistoryConverter;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

import com.tlc.i18n.I18nResolver;
import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the workflow history related web actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@WebAction(path = "/workflow/execution/history/mgmt")
public final class WorkflowExecutionHistoryManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowExecutionHistoryManagement.class);
    private static final String HISTORY = "history";

    private final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;
    private final WorkflowExecutionHistoryConverter workflowExecutionHistoryConverter;
    private final UserService userService;

    public WorkflowExecutionHistoryManagement() {
        this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        this.workflowExecutionHistoryConverter = WorkflowExecutionHistoryConverter.getInstance();
        this.userService = UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Handles the received request
     * @param crmResponse Generates the CRM response
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(WorkflowAttributes.DATA);

        switch (requestJson.getString(WorkflowAttributes.TYPE)) {
            case "get" ->
                    processHistory(orgId, data.getLong(WorkflowAttributes.ID), request.i18nResolver(), crmResponse);
        }
    }

    /**
     * <p>
     * Gets the history details for the given id
     * </p>
     *
     * @param orgId Represents an  organization id
     * @param id    Represents the history id
     * @return {@link JsonObject} contains of converted {@link WorkflowExecutionHistory} details
     */
    private void processHistory(final long orgId, final Long id, final I18nResolver i18nResolver, final CrmResponse crmResponse) {
        if (workflowExecutionHistoryManager.exists(orgId, id)) {
            final WorkflowExecutionHistory workflowExecutionHistory = workflowExecutionHistoryManager.get(orgId, id);

            if (Objects.nonNull(workflowExecutionHistory)) {
                final JsonObject jsonObject = workflowExecutionHistory.getWorkflowState();

                if (jsonObject.containsKey(WorkflowAttributes.CONDITIONS) && jsonObject.containsKey(WorkflowAttributes.USER_IDS)) {
                    final Collection<Long> userIds = new HashSet<>();

                    jsonObject.getJsonArray(WorkflowAttributes.USER_IDS).forEach(userId -> userIds.add(NumberUtils.parseNumber(userId).longValue()));

                    if (userIds.isEmpty()) {
                        crmResponse.put(HISTORY, workflowExecutionHistoryConverter.convert(workflowExecutionHistory, i18nResolver));
                    } else {
                        if (jsonObject.containsKey(WorkflowAttributes.CONDITIONS)) {
                            final Map<Long, CrmUser> userMap = userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));
                            final JsonArray conditionArray = jsonObject.getJsonArray(WorkflowAttributes.CONDITIONS);
                            final JsonObject triggerJson = jsonObject.getJsonObject(WorkflowAttributes.TRIGGER);

                            WorkflowCriteriaResolver.getInstance().resolveUserConditions(orgId,
                                    TriggerSource.get(triggerJson.getShort(WorkflowAttributes.SOURCE)), conditionArray, userMap);
                        }
                        crmResponse.put(HISTORY, workflowExecutionHistoryConverter.convert(workflowExecutionHistory, i18nResolver));
                    }
                } else {
                    crmResponse.put(HISTORY, workflowExecutionHistoryConverter.convert(workflowExecutionHistory, i18nResolver));
                }
            }
        } else {
            throw ErrorCode.get(WorkflowErrorCodes.HISTORY_NOT_FOUND, "i18n.workflow.history.not.found");
        }
    }
}
