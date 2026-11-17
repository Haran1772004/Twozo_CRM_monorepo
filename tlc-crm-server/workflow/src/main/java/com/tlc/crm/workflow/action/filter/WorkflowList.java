package com.tlc.crm.workflow.action.filter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.filter.WorkflowFilter;
import com.tlc.crm.workflow.api.model.filter.FilterGroup;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Handles the HTTP requests that are relevant to workflow list action
 * </p>
 *
 * @author Kavinila E, Karthick V
 * @version 1.0
 */
@WebAction(path = "/workflow/list")
public final class WorkflowList extends CrmWorkflowListAction {

    private static final String TOTAL_SELECTED_VALUES = "totalSelectedValues";
    private static final String FILTERS = "filters";
    private static final String CRITERIA = "criteria";

    private final WorkflowManager workflowManager;
    private final WorkflowFilterManager workflowFilterManager;
    private final DateTimeUtil dateTimeUtil;

    public WorkflowList() {
        this.workflowManager = WorkflowManager.getInstance();
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Type} associated with the implementing class
     */
    @Override
    protected Type getType() {
        return Type.WORKFLOW_AUTOMATION;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param type     Represents the {@link Type}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    protected void loadFilters(final long orgId, final long userId, final Type type, final CrmResponse response) {
        final WorkflowFilter workflowFilter = workflowFilterManager.getWorkflowFilter(orgId, userId, type);
        final JsonArray filterCriteriaArray = Json.array();

        if (Objects.nonNull(workflowFilter)) {
            final Map<FilterGroup, Collection<Long>> criteria = workflowFilter.getCriteria();

            for (final Map.Entry<FilterGroup, Collection<Long>> criteriaGroup : criteria.entrySet()) {
                final Collection<Long> criteriaValue = criteriaGroup.getValue();
                final int criteriaSize = criteriaValue.size();
                final JsonObject criteriaJson = Json.object();
                final JsonArray criteriaArray = Json.array();

                for (final Long value : criteriaValue) {
                    criteriaArray.put(value);
                }

                if (1 < criteriaSize) {
                    criteriaJson.put(TOTAL_SELECTED_VALUES, criteriaSize);
                }
                criteriaJson.put(FILTER_GROUP, criteriaGroup.getKey().getId());
                criteriaJson.put(CRITERIA, criteriaArray);
                filterCriteriaArray.put(criteriaJson);
            }
        } else {
            constructDefaultFilter(filterCriteriaArray);
        }
        response.put(FILTERS, filterCriteriaArray);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param jsonObject  The {@link JsonObject} containing filter criteria, pagination details, and sorting information.
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void loadList(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final String criteriaKey = WorkflowAttributes.CRITERIA;
        final WorkflowFilter workflowFilter;
        final Type type = getType();

        if (jsonObject.containsKey(criteriaKey)) {
            workflowFilter = buildWorkflowFilter(orgId, userId, jsonObject, type);

            workflowFilterManager.loadWorkflowFilterIntoContainer(workflowFilter);
        } else {
            workflowFilter = workflowFilterManager.getWorkflowFilter(orgId, userId, type);
        }

        if (Objects.nonNull(workflowFilter) && jsonObject.containsKey(WorkflowAttributes.SEARCH_TEXT)) {
            workflowFilter.setSearchText(jsonObject.getString(WorkflowAttributes.SEARCH_TEXT));
        }

        final int start = Math.max(jsonObject.optInt(WorkflowAttributes.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(WorkflowAttributes.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = workflowManager.getTotalCount(orgId, workflowFilter);
        final boolean hasMore = (start + limit) <= totalRecords;

        if (totalRecords > 0 && start <= totalRecords) {
            final JsonObject sortJson = jsonObject.optJsonObject(WorkflowAttributes.SORT);
            final String sortField;
            final String sortType;

            if (Objects.isNull(sortJson)) {
                sortField = WorkflowAttributes.MODIFIED_TIME;
                sortType = WorkflowAttributes.DESC;
            } else {
                sortField = sortJson.getString(WorkflowAttributes.FIELD);
                sortType = sortJson.getString(WorkflowAttributes.TYPE);
            }
            final Collection<Workflow> workflows = workflowManager.getWorkflowList(orgId, start, limit, sortField, sortType, workflowFilter);

            if (!workflows.isEmpty()) {

                for (final Workflow workflow : workflows) {
                    jsonArray.put(convert(workflow));
                }
            }
        }
        crmResponse.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
        crmResponse.put(WorkflowAttributes.HAS_MORE, hasMore);
        crmResponse.put(WorkflowAttributes.LIST, jsonArray);
    }

    /**
     * <p>
     * Builds the {@link WorkflowFilter} based on details stored in the {@link JsonObject} {@link Type}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject The {@link JsonObject} containing filter criteria
     * @param type       Represents the {@link Type}
     * @return {@link WorkflowFilter}
     */
    private WorkflowFilter buildWorkflowFilter(final long orgId, final long userId, final JsonObject jsonObject, final Type type) {
        final JsonArray criteriaArray = jsonObject.getJsonArray(WorkflowAttributes.CRITERIA);
        final Map<FilterGroup, Collection<Long>> criteriaList = new HashMap<>();
        final WorkflowFilter workflowFilter = new WorkflowFilter();
        final int criteriaSize = criteriaArray.size();

        workflowFilter.setUserId(userId);
        workflowFilter.setOrgId(orgId);
        workflowFilter.setType(type);

        for (int criteriaIndex = 0; criteriaIndex < criteriaSize; criteriaIndex++) {
            final JsonObject criteriaJson = criteriaArray.getJsonObject(criteriaIndex);
            final FilterGroup filterGroup = FilterGroup.get(criteriaJson.getShort(FILTER_GROUP));
            final JsonArray valueArray = criteriaJson.getJsonArray(WorkflowAttributes.VALUES);
            final Collection<Long> values = new ArrayList<>();

            for (int valueIndex = 0; valueIndex < valueArray.size(); valueIndex++) {
                final JsonObject valueJson = valueArray.getJsonObject(valueIndex);

                values.add(valueJson.getLong(WorkflowAttributes.ID));
            }
            criteriaList.put(filterGroup, values);
        }
        workflowFilter.setCriteria(criteriaList);

        return workflowFilter;
    }

    /**
     * <p>
     * Constructs default filter criteria based on the specified filter type and appends them to the provided JsonArray
     * </p>
     *
     * @param criteriaArray The {@link JsonArray} to which the default filter criteria are appended
     */
    private void constructDefaultFilter(final JsonArray criteriaArray) {
        final JsonArray defaultFilterValue = Json.array().put(DEFAULT_FILTER_VALUE);
        final JsonObject triggerJson = Json.object();
        final JsonObject actionJson = Json.object();
        final JsonObject statusJson = Json.object();
        final JsonObject createdByJson = Json.object();

        triggerJson.put(FILTER_GROUP, FilterGroup.TRIGGERS.getId());
        triggerJson.put(CRITERIA, defaultFilterValue);
        criteriaArray.put(triggerJson);

        actionJson.put(FILTER_GROUP, FilterGroup.ACTIONS.getId());
        actionJson.put(CRITERIA, defaultFilterValue);
        criteriaArray.put(actionJson);

        statusJson.put(FILTER_GROUP, FilterGroup.STATUS.getId());
        statusJson.put(CRITERIA, defaultFilterValue);
        criteriaArray.put(statusJson);

        createdByJson.put(FILTER_GROUP, FilterGroup.CREATED_BY.getId());
        createdByJson.put(CRITERIA, defaultFilterValue);
        criteriaArray.put(createdByJson);
    }

    /**
     * <p>
     * Converts the {@link Workflow} into {@link JsonObject}
     * </p>
     *
     * @param workflow It holds the {@link Workflow} details
     * @return A {@link JsonObject} representing the converted {@link Workflow}
     */
    private JsonObject convert(final Workflow workflow) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(WorkflowAttributes.ID, workflow.id())
                .put(WorkflowAttributes.NAME, workflow.getName())
                .put(WorkflowAttributes.IS_ACTIVE, workflow.isActive())
                .put(WorkflowAttributes.TRIGGER_SOURCE, workflow.getTriggerSource().getId())
                .put(WorkflowAttributes.TRIGGER_EVENT, workflow.getTriggerEvent().getId())
                .put(WorkflowAttributes.ACTIONS, workflow.getTotalActions())
                .put(WorkflowAttributes.MODIFIED_TIME, dateTimeUtil.getTimeStamp(workflow.getModifiedTime()));

        return jsonObject;
    }
}
