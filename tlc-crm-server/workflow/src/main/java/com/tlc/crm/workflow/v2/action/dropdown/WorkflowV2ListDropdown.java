package com.tlc.crm.workflow.v2.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;

import java.util.Collection;

/**
 * Manages the workflow list dropdown action for history filter.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
@StaticDropdown(name = "workflowv2List")
public class WorkflowV2ListDropdown extends AbstractDropdownAction {

    private static final String ALL_WORKFLOWS_I18N_KEY = "i18n.workflow.filter.all.workflows";
    private static final int DEFAULT_FILTER_VALUE = -1;
    private final WorkflowV2Manager workflowManager;

    public WorkflowV2ListDropdown() {
        this.workflowManager = WorkflowV2Manager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @return {@link OptionContainer} that holds the requested dropdown values
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = workflowManager.getTotalCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<WorkflowV2> workflows = workflowManager.getWorkflows(orgId, dropdownLimit, dropdownCriteria);
            final JsonObject allWorkflowsJson = Json.object();

            allWorkflowsJson.put(WorkflowAttributes.NAME, dropdownRequest.i18nResolver().get(ALL_WORKFLOWS_I18N_KEY));
            allWorkflowsJson.put(WorkflowAttributes.ID, DEFAULT_FILTER_VALUE);

            optionContainer.addOption(allWorkflowsJson);

            for (final WorkflowV2 workflow : workflows) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(WorkflowAttributes.NAME, workflow.getName());
                jsonObject.put(WorkflowAttributes.ID, workflow.id());

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }
}
