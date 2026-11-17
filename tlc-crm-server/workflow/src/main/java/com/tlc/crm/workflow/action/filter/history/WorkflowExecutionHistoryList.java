package com.tlc.crm.workflow.action.filter.history;

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
import com.tlc.crm.workflow.api.manager.history.WorkflowExecutionHistoryManager;
import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionHistory;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.api.model.history.filter.DateIntervalType;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilterType;
import com.tlc.crm.workflow.api.model.history.filter.WorkflowExecutionHistoryFilter;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles the HTTP requests that are relevant to history list action
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@WebAction(path = "/workflow/execution/history/list")
public final class WorkflowExecutionHistoryList extends CrmWorkflowListAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowExecutionHistoryList.class);
    private static final String ALL_WORKFLOWS = "All Workflows";
    private static final String FILTER = "filter";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String WORKFLOW_ID = "workflowId";

    private final WorkflowManager workflowManager;
    private final WorkflowFilterManager workflowFilterManager;
    private final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final CrmUserConverter userConverter;
    private final DateTimeUtil dateTimeUtil;

    public WorkflowExecutionHistoryList() {
        this.workflowManager = WorkflowManager.getInstance();
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.userService = UserService.getInstance();
        this.userConverter = CrmUserConverter.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Type} associated with the implementing class
     */
    @Override
    protected Type getType() {
        return Type.WORKFLOW_HISTORY;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param type        Represents the {@link Type} of the filter
     * @param crmResponse Represents the {@link CrmResponse}
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
            jsonObject.put(WorkflowAttributes.NAME, Objects.equals(value.intValue(), DEFAULT_FILTER_VALUE) ? ALL_WORKFLOWS : workflowManager.getWorkflowName(orgId, value));
        }
        crmResponse.put(FILTER, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param jsonObject  The {@link JsonObject} containing filter criteria, pagination details, and sorting information
     * @param crmResponse Represents the {@link CrmResponse}.
     */
    @Override
    protected void loadList(final long orgId, long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final WorkflowExecutionHistoryFilter workflowExecutionHistoryFilter;
        final Type type = getType();
        final int start = Math.max(jsonObject.optInt(WorkflowAttributes.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(WorkflowAttributes.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);

        if (jsonObject.containsKey(WorkflowAttributes.CRITERIA)) {
            workflowExecutionHistoryFilter = buildHistoryFilter(orgId, userId, jsonObject, type);

            workflowFilterManager.loadExecutionHistoryFilterRowIntoContainer(workflowExecutionHistoryFilter);
        } else {
            workflowExecutionHistoryFilter = workflowExecutionHistoryManager.getWorkflowExecutionHistoryFilter(orgId, userId, type);
        }
        final long totalRecords = workflowExecutionHistoryManager.getTotalCount(orgId, workflowExecutionHistoryFilter);
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
            final Collection<WorkflowExecutionHistory> workflowExecutionHistories = workflowExecutionHistoryManager.getWorkflowExecutionHistoryList(orgId,
                    start, limit, sortField, sortType, workflowExecutionHistoryFilter);

            if (!workflowExecutionHistories.isEmpty()) {
                fetchUserAndDispatchHistories(orgId, totalRecords, hasMore, workflowExecutionHistories, crmResponse, sortField, SortType.getSortType(sortType));
            }
        } else {
            crmResponse.put(WorkflowAttributes.HAS_MORE, hasMore);
            crmResponse.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
            crmResponse.put(WorkflowAttributes.LIST, Json.array());
        }
    }

    /**
     * <p>
     * Fetches user data associated with {@link Collection} of {@link WorkflowExecutionHistory} and dispatches the updated histories records
     * </p>
     *
     * @param orgId                      Represents an organization id
     * @param totalRecords               Represents the total number of histories which is matches for the given filter
     * @param hasMore                    A flag which indicate whether there are additional records to be in the histories
     * @param workflowExecutionHistories Represents the {@link Collection} of {@link WorkflowExecutionHistory}
     * @param sortField                  Represents the field used to sort the data
     * @param sortType                   Represents the {@link SortType}
     * @param crmResponse                Represents the {@link CrmResponse}
     */
    private void fetchUserAndDispatchHistories(final long orgId, final long totalRecords, final boolean hasMore,
                                               final Collection<WorkflowExecutionHistory> workflowExecutionHistories,
                                               final CrmResponse crmResponse, final String sortField, final SortType sortType) {
        final Collection<Long> userIds = workflowExecutionHistories.stream().map(WorkflowExecutionHistory::getCrmUser).filter(Objects::nonNull).
                map(CrmUser::getIamId).collect(Collectors.toUnmodifiableSet());

        if (userIds.isEmpty()) {
            setResponse(hasMore, totalRecords, workflowExecutionHistories, crmResponse);
        } else {
            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                /**
                 * {@inheritDoc}
                 *
                 * @param users {@link Collection} of {@link User}
                 */
                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    try {
                        if (WorkflowAttributes.SALES_OWNER.equalsIgnoreCase(sortField)) {
                            final Collection<WorkflowExecutionHistory> sortedHistories = new ArrayList<>();
                            final Comparator<User> nameComparator = Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER);

                            setUser(workflowExecutionHistories, userMap);
                            final Map<Long, List<WorkflowExecutionHistory>> historiesGroupedByUserId = workflowExecutionHistories
                                    .stream().collect(Collectors.groupingBy(history -> history.getCrmUser().getIamId()));

                            users.stream().sorted(Objects.equals(SortType.ASCENDING, sortType)
                                    ? nameComparator : nameComparator.reversed())
                                    .forEach(user -> sortedHistories.addAll(historiesGroupedByUserId.get(user.id())));
                            setResponse(hasMore, totalRecords, sortedHistories, crmResponse);
                        } else {
                            setUser(workflowExecutionHistories, userMap);
                            setResponse(hasMore, totalRecords, workflowExecutionHistories, crmResponse);
                        }
                    } catch (Throwable throwable) {
                        LOGGER.error("Workflow history: Failed to retrieve workflow execution history list. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        sendError(crmResponse, null, ErrorCode.get(WorkflowErrorCodes.FAILED_TO_RETRIEVE_WORKFLOW_HISTORY_LIST, "i18n.workflow.execution.history.retrieval.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                /**
                 * {@inheritDoc}
                 *
                 * @param throwable Cause of the failure
                 */
                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(WorkflowErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), userIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Sets {@link User} to the collection of {@link WorkflowExecutionHistory}
     * </p>
     *
     * @param workflowExecutionHistories Represents the collection of {@link WorkflowExecutionHistory}
     * @param userMap                    Represents the user map
     */
    private void setUser(final Collection<WorkflowExecutionHistory> workflowExecutionHistories, final Map<Long, User> userMap) {
        for (final WorkflowExecutionHistory workflowExecutionHistory : workflowExecutionHistories) {
            final CrmUser crmUser = workflowExecutionHistory.getCrmUser();

            if (Objects.nonNull(crmUser)) {
                crmUser.setUser(userMap.get(crmUser.getIamId()));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject The {@link JsonObject} containing filter criteria
     * @param type       Represents the {@link Type}
     */
    public WorkflowExecutionHistoryFilter buildHistoryFilter(final long orgId, final long userId, final JsonObject jsonObject, final Type type) {
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
     * {@inheritDoc}
     *
     * @param workflowExecutionHistory It holds the {@link WorkflowExecutionHistory} details
     * @param crmResponse              Represents the crm response
     * @return A {@link JsonObject} representing the converted {@link WorkflowExecutionHistory}
     */
    private JsonObject convert(final WorkflowExecutionHistory workflowExecutionHistory, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final Workflow workflow = workflowExecutionHistory.getWorkflow();
        final CrmUser crmUser = workflowExecutionHistory.getCrmUser();

        jsonObject.put(WORKFLOW_ID, workflow.id())
                .put(WorkflowAttributes.NAME, workflow.getName())
                .put(WorkflowAttributes.TRIGGER_SOURCE, workflow.getTriggerSource().getId())
                .put(WorkflowAttributes.ACTIONS, workflow.getTotalActions())
                .put(WorkflowAttributes.STATUS, crmResponse.resolveI18nString(workflowExecutionHistory.getExecutionStatus().getName()))
                .put(WorkflowAttributes.TIMESTAMP, dateTimeUtil.getTimeStamp(workflowExecutionHistory.getExecutedTime()))
                .put(WorkflowAttributes.ID, workflowExecutionHistory.id());

        if (Objects.nonNull(crmUser)) {
            jsonObject.put(WorkflowAttributes.OWNER, userConverter.convert(crmUser, true, true));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Sets the response of the converted {@link Collection} of {@link WorkflowExecutionHistory} applicable for the given filter
     * </p>
     *
     * @param hasMore      A flag which indicate whether there are additional records to be in the histories
     * @param totalRecords Represents the total number of histories which is matches for the given filter
     * @param histories    Represents the {@link Collection} of {@link WorkflowExecutionHistory}
     * @param crmResponse  Represents the {@link CrmResponse}
     */
    private void setResponse(final boolean hasMore, final long totalRecords, final Collection<WorkflowExecutionHistory> histories, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();

        crmResponse.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
        crmResponse.put(WorkflowAttributes.HAS_MORE, hasMore);

        histories.forEach(history -> {
            jsonArray.put(convert(history, crmResponse));
        });
        crmResponse.put(WorkflowAttributes.LIST, jsonArray);
    }
}
