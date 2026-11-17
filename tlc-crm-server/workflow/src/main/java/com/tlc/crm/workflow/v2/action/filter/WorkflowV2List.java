package com.tlc.crm.workflow.v2.action.filter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.workflow.action.filter.CrmWorkflowListAction;
import com.tlc.crm.workflow.action.filter.WorkflowList;
import com.tlc.crm.workflow.api.manager.filter.WorkflowFilterManager;
import com.tlc.crm.workflow.api.model.filter.FilterGroup;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.api.model.filter.WorkflowFilter;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.WorkflowListDTO;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handles workflow V2 list requests.
 * Copied from {@link WorkflowList}
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_WORKFLOWS)
@WebAction(path = "/v2/workflow/list")
public final class WorkflowV2List extends CrmWorkflowListAction {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_FILTER_VALUE = -1;
    private static final String TOTAL_SELECTED_VALUES = "totalSelectedValues";

    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowFilterManager workflowFilterManager;
    private final DateTimeUtil dateTimeUtil;

    public WorkflowV2List() {
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.workflowFilterManager = WorkflowFilterManager.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Type getType() {
        return Type.WORKFLOW_AUTOMATION_V2;
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
                criteriaJson.put(WorkflowAttributesV2.FILTER_GROUP, criteriaGroup.getKey().getId());
                criteriaJson.put(WorkflowAttributesV2.CRITERIA, criteriaArray);
                filterCriteriaArray.put(criteriaJson);
            }
        } else {
            constructDefaultFilter(filterCriteriaArray);
        }
        response.put(WorkflowAttributesV2.FILTERS, filterCriteriaArray);
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
        final String criteriaKey = WorkflowAttributesV2.CRITERIA;
        final WorkflowFilter workflowFilter;
        final Type type = getType();

        if (jsonObject.containsKey(criteriaKey)) {
            workflowFilter = buildWorkflowFilter(orgId, userId, jsonObject, type);

            workflowFilterManager.loadWorkflowFilterIntoContainer(workflowFilter);
        } else {
            workflowFilter = workflowFilterManager.getWorkflowFilter(orgId, userId, type);
        }

        if (Objects.nonNull(workflowFilter) && jsonObject.containsKey(WorkflowAttributesV2.SEARCH_TEXT)) {
            workflowFilter.setSearchText(jsonObject.getString(WorkflowAttributesV2.SEARCH_TEXT));
        }

        final int start = Math.max(jsonObject.optInt(WorkflowAttributesV2.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(WorkflowAttributesV2.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = workflowV2Manager.getWorkflowV2Count(orgId, workflowFilter);
        final boolean hasMore = (start + limit) <= totalRecords;

        if (totalRecords > 0 && start <= totalRecords) {
            final JsonObject sortJson = jsonObject.optJsonObject(WorkflowAttributesV2.SORT);
            final String sortField;
            final String sortType;

            if (Objects.isNull(sortJson)) {
                sortField = WorkflowAttributesV2.MODIFIED_TIME;
                sortType = WorkflowAttributesV2.DESC;
            } else {
                sortField = sortJson.getString(WorkflowAttributesV2.FIELD);
                sortType = sortJson.getString(WorkflowAttributesV2.TYPE);
            }
            final Collection<WorkflowListDTO> workflows = workflowV2Manager.getWorkflowV2List(orgId, start, limit, sortField, sortType, workflowFilter);

            if (!workflows.isEmpty()) {
                for (final WorkflowListDTO workflow : workflows) {
                    jsonArray.put(convert(workflow));
                }
            }
        }
        crmResponse.put(WorkflowAttributesV2.TOTAL_RECORDS, totalRecords);
        crmResponse.put(WorkflowAttributesV2.HAS_MORE, hasMore);
        crmResponse.put(WorkflowAttributesV2.LIST, jsonArray);
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
    private WorkflowFilter buildWorkflowFilter(final long orgId, final long userId, final JsonObject jsonObject,
                                               final Type type) {
        final JsonArray criteriaArray = jsonObject.getJsonArray(WorkflowAttributesV2.CRITERIA);
        final Map<FilterGroup, Collection<Long>> criteriaList = new HashMap<>();
        final WorkflowFilter workflowFilter = new WorkflowFilter();
        final int criteriaSize = criteriaArray.size();

        workflowFilter.setUserId(userId);
        workflowFilter.setOrgId(orgId);
        workflowFilter.setType(type);

        for (int criteriaIndex = 0; criteriaIndex < criteriaSize; criteriaIndex++) {
            final JsonObject criteriaJson = criteriaArray.getJsonObject(criteriaIndex);
            final FilterGroup filterGroup = FilterGroup.get(criteriaJson.getShort(WorkflowAttributesV2.FILTER_GROUP));
            final JsonArray valueArray = criteriaJson.getJsonArray(WorkflowAttributesV2.VALUES);
            final Collection<Long> values = new ArrayList<>();

            for (int valueIndex = 0; valueIndex < valueArray.size(); valueIndex++) {
                final JsonObject valueJson = valueArray.getJsonObject(valueIndex);

                values.add(valueJson.getLong(WorkflowAttributesV2.ID));
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

        triggerJson.put(WorkflowAttributesV2.FILTER_GROUP, FilterGroup.TRIGGERS.getId());
        triggerJson.put(WorkflowAttributesV2.CRITERIA, defaultFilterValue);
        criteriaArray.put(triggerJson);

        actionJson.put(WorkflowAttributesV2.FILTER_GROUP, FilterGroup.ACTIONS.getId());
        actionJson.put(WorkflowAttributesV2.CRITERIA, defaultFilterValue);
        criteriaArray.put(actionJson);

        statusJson.put(WorkflowAttributesV2.FILTER_GROUP, FilterGroup.STATUS.getId());
        statusJson.put(WorkflowAttributesV2.CRITERIA, defaultFilterValue);
        criteriaArray.put(statusJson);

        createdByJson.put(WorkflowAttributesV2.FILTER_GROUP, FilterGroup.CREATED_BY.getId());
        createdByJson.put(WorkflowAttributesV2.CRITERIA, defaultFilterValue);
        criteriaArray.put(createdByJson);
    }

    /**
     * <p>
     * Converts the {@code WorkflowListDTO} into {@link JsonObject}
     * </p>
     *
     * @param workflow It holds the {@code WorkflowListDTO} details
     * @return A {@link JsonObject} representing the converted {@code WorkflowListDTO}
     */
    private JsonObject convert(final WorkflowListDTO workflow) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(WorkflowAttributesV2.ID, workflow.getId())
                .put(WorkflowAttributesV2.NAME, workflow.getName())
                .put(WorkflowAttributesV2.IS_ACTIVE, workflow.isActive())
                .put(WorkflowAttributesV2.TRIGGER_SOURCE, workflow.getTriggerSource().getId())
                .put(WorkflowAttributesV2.TRIGGER_EVENT, workflow.getTriggerEvent().getId())
                .put(WorkflowAttributesV2.ACTIONS, workflow.getTotalActions())
                .put(WorkflowAttributesV2.MODIFIED_TIME, dateTimeUtil.getTimeStamp(workflow.getModifiedTime()));

        return jsonObject;
    }
}
