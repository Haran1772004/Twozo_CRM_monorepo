package com.tlc.crm.workflow.v2.action.filter.history;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.SortType;

import com.tlc.crm.workflow.action.filter.CrmWorkflowListAction;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.manager.history.WorkflowExecutionHistoryManager;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.api.model.history.filter.DateIntervalType;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilter;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilterType;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import com.tlc.web.client.event.EventCallback;
import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * V2 execution history list with the same JSON field names as V1.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
@WebAction(path = "/v2/workflow/execution/history/list")
public final class WorkflowV2ExecutionHistoryList extends CrmWorkflowListAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2ExecutionHistoryList.class);

    private static final String ALL_WORKFLOWS = "All Workflows";
    private static final String FILTER = "filter";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String WORKFLOW_ID = "workflowId";

    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowFilterManager workflowFilterManager;
    private final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;
    private final WorkflowV2ExecutionManager executionManager;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final CrmUserConverter userConverter;
    private final DateTimeUtil dateTimeUtil;

    public WorkflowV2ExecutionHistoryList() {
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.userService = UserService.getInstance();
        this.userConverter = CrmUserConverter.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The type of the workflow history list.
     */
    @Override
    protected Type getType() {
        return Type.WORKFLOW_HISTORY_V2;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The organization ID.
     * @param userId      The user ID.
     * @param type        The type of the workflow history list.
     * @param crmResponse The CRM response.
     */
    @Override
    protected void loadFilters(final long orgId, final long userId, final Type type, final CrmResponse crmResponse) {
        final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter = workflowExecutionHistoryManager.getWorkflowExecutionHistoryFilter(orgId, userId, type);
        final JsonObject jsonObject = Json.object();

        if (Objects.isNull(workflowExecutionHistoryFilter)) {
            jsonObject.put(WorkflowAttributes.ID, DEFAULT_FILTER_VALUE);
            jsonObject.put(WorkflowAttributes.NAME, ALL_WORKFLOWS);
        } else {
            final Long value = workflowExecutionHistoryFilter.getFilterValue(WorkflowExecutionHistoryFilterType.WORKFLOWS);

            jsonObject.put(WorkflowAttributes.ID, value);
            jsonObject.put(WorkflowAttributes.NAME, Objects.equals(value.intValue(), DEFAULT_FILTER_VALUE) ? ALL_WORKFLOWS : workflowV2Manager.getWorkflowName(orgId, value));
        }
        crmResponse.put(FILTER, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The organization ID.
     * @param userId      The user ID.
     * @param jsonObject  The JSON object.
     * @param crmResponse The CRM response.
     */
    @Override
    protected void loadList(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final WorkflowExecutionHistoryFilter filter;
        final Type type = getType();
        final int start = Math.max(jsonObject.optInt(WorkflowAttributes.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(WorkflowAttributes.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);

        if (jsonObject.containsKey(WorkflowAttributes.CRITERIA)) {
            filter = buildHistoryFilter(orgId, userId, jsonObject, type);

            workflowFilterManager.loadExecutionHistoryFilterRowIntoContainer(filter);
        } else {
            filter = workflowExecutionHistoryManager.getWorkflowExecutionHistoryFilter(orgId, userId, type);
        }
        final long totalRecords = executionManager.getTotalCount(orgId, filter);
        final boolean hasMore = (start + limit) <= totalRecords;

        if (totalRecords > 0 && start <= totalRecords) {
            final JsonObject sortJson = jsonObject.optJsonObject(WorkflowAttributes.SORT);
            final String sortField;
            final String sortType;

            if (Objects.isNull(sortJson)) {
                sortField = WorkflowAttributes.EXECUTED_TIME;
                sortType = WorkflowAttributes.DESC;
            } else {
                sortField = sortJson.getString(WorkflowAttributes.FIELD);
                sortType = sortJson.getString(WorkflowAttributes.TYPE);
            }
            final Collection<WorkflowV2Execution> executions = executionManager.getExecutionList(orgId, start, limit, sortField, sortType, filter);

            if (!executions.isEmpty()) {
                fetchUserAndDispatch(orgId, totalRecords, hasMore, executions, crmResponse, sortField, SortType.getSortType(sortType));
            }
        } else {
            crmResponse.put(WorkflowAttributes.HAS_MORE, hasMore);
            crmResponse.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
            crmResponse.put(WorkflowAttributes.LIST, Json.array());
        }
    }

    /**
     * Builds the history filter.
     *
     * @param orgId      The organization ID.
     * @param userId     The user ID.
     * @param jsonObject The JSON object.
     * @param type       The type of the workflow history list.
     * @return The history filter.
     */
    private WorkflowExecutionHistoryFilter buildHistoryFilter(final long orgId, final long userId, final JsonObject jsonObject, final Type type) {
        final JsonArray criteriaArray = jsonObject.getJsonArray((WorkflowAttributes.CRITERIA));
        final Map<WorkflowExecutionHistoryFilterType, Long> criteriaList = new HashMap<>();
        final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter = new WorkflowExecutionHistoryFilter();
        final int criteriaSize = criteriaArray.size();
        final String valueKey = WorkflowAttributes.VALUE;

        workflowExecutionHistoryFilter.setUserId(userId);
        workflowExecutionHistoryFilter.setOrgId(orgId);
        workflowExecutionHistoryFilter.setType(type);

        for (int criteriaIndex = 0; criteriaIndex < criteriaSize; criteriaIndex++) {
            final JsonObject criteriaJson = criteriaArray.getJsonObject(criteriaIndex);
            final WorkflowExecutionHistoryFilterType workflowExecutionHistoryFilterType = WorkflowExecutionHistoryFilterType.get(criteriaJson.getShort(FILTER_GROUP));
            final Long value = criteriaJson.getLong(valueKey);

            if (Objects.equals(WorkflowExecutionHistoryFilterType.DUE_DATE, workflowExecutionHistoryFilterType) &&
                    Objects.equals(DateIntervalType.BETWEEN, DateIntervalType.get(value.shortValue()))) {
                workflowExecutionHistoryFilter.setInterval(new Interval<>(criteriaJson.getLong(FROM), criteriaJson.getLong(TO)));
            }
            criteriaList.put(workflowExecutionHistoryFilterType, value);
        }
        workflowExecutionHistoryFilter.setCriteria(criteriaList);

        return workflowExecutionHistoryFilter;
    }

    /**
     * Fetches the users and dispatches the executions.
     *
     * @param orgId        The organization ID.
     * @param totalRecords The total number of records.
     * @param hasMore      Whether there are more records.
     * @param executions   The executions.
     * @param crmResponse  The CRM response.
     * @param sortField    The sort field.
     * @param sortType     The sort type.
     */
    private void fetchUserAndDispatch(final long orgId, final long totalRecords, final boolean hasMore,
                                      final Collection<WorkflowV2Execution> executions, final CrmResponse crmResponse,
                                      final String sortField, final SortType sortType) {
        final Collection<Long> userIds = executions.stream().map(WorkflowV2Execution::getCrmUser).filter(Objects::nonNull)
                .map(CrmUser::getIamId).collect(Collectors.toUnmodifiableSet());

        if (userIds.isEmpty()) {
            setResponse(hasMore, totalRecords, executions, crmResponse);
            return;
        }
        crmResponse.convertIntoAsync();

        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {
            @Override
            public void onSuccess(final Collection<User> users) {
                final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                try {
                    if (WorkflowAttributes.SALES_OWNER.equalsIgnoreCase(sortField)) {
                        final Collection<WorkflowV2Execution> sorted = new ArrayList<>();
                        final Comparator<User> nameComparator = Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER);

                        setUser(executions, userMap);
                        final Map<Long, List<WorkflowV2Execution>> grouped = executions.stream()
                                .collect(Collectors.groupingBy(history -> history.getCrmUser().getIamId()));

                        users.stream().sorted(Objects.equals(SortType.ASCENDING, sortType) ? nameComparator : nameComparator.reversed())
                                .forEach(user -> sorted.addAll(grouped.get(user.id())));
                        setResponse(hasMore, totalRecords, sorted, crmResponse);
                    } else {
                        setUser(executions, userMap);
                        setResponse(hasMore, totalRecords, executions, crmResponse);
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Workflow V2 history list failed. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    sendError(crmResponse, null, ErrorCode.get(WorkflowErrorCodes.FAILED_TO_RETRIEVE_WORKFLOW_HISTORY_LIST, "i18n.workflow.execution.history.retrieval.failed"));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(WorkflowErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
            }
        };
        userService.fetchUsers(organizationService.getIamOrgId(orgId), userIds, usersFetchCallBack);
    }

    /**
     * Sets the user for the executions.
     *
     * @param executions The executions.
     * @param userMap    The user map.
     */
    private void setUser(final Collection<WorkflowV2Execution> executions, final Map<Long, User> userMap) {
        for (final WorkflowV2Execution execution : executions) {
            final CrmUser crmUser = execution.getCrmUser();

            if (Objects.nonNull(crmUser)) {
                crmUser.setUser(userMap.get(crmUser.getIamId()));
            }
        }
    }

    /**
     * Converts the execution to a JSON object.
     *
     * @param execution   The execution.
     * @param crmResponse The CRM response.
     * @return The JSON object.
     */
    private JsonObject convert(final WorkflowV2Execution execution, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final CrmUser crmUser = execution.getCrmUser();

        jsonObject.put(WORKFLOW_ID, execution.getWorkflowId())
                .put(WorkflowAttributes.NAME, execution.getWorkflowName())
                .put(WorkflowAttributes.TRIGGER_SOURCE, execution.getTriggerSource().getId())
                .put(WorkflowAttributes.ACTIONS, execution.getTotalActions())
                .put(WorkflowAttributes.STATUS, crmResponse.resolveI18nString(execution.getStatus().getDisplayName()))
                .put(WorkflowAttributes.TIMESTAMP, dateTimeUtil.getTimeStamp(execution.getStartedTime()))
                .put(WorkflowAttributes.ID, execution.id());

        if (Objects.nonNull(crmUser)) {
            jsonObject.put(WorkflowAttributes.OWNER, userConverter.convert(crmUser, true, true));
        }
        return jsonObject;
    }

    /**
     * Sets the response.
     *
     * @param hasMore      Whether there are more records.
     * @param totalRecords The total number of records.
     * @param histories    The histories.
     * @param crmResponse  The CRM response.
     */
    private void setResponse(final boolean hasMore, final long totalRecords, final Collection<WorkflowV2Execution> histories,
                             final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();

        crmResponse.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
        crmResponse.put(WorkflowAttributes.HAS_MORE, hasMore);
        histories.forEach(history -> jsonArray.put(convert(history, crmResponse)));
        crmResponse.put(WorkflowAttributes.LIST, jsonArray);
    }
}
