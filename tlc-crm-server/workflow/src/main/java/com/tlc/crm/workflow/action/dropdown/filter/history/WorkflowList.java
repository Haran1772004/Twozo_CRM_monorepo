package com.tlc.crm.workflow.action.dropdown.filter.history;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import java.util.Collection;

/**
 * <p>
 * Manages the workflow list dropdown action
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@StaticDropdown(name = "workflowList")
public class WorkflowList extends AbstractDropdownAction {

    private static final String ALL_WORKFLOWS_I18N_KEY = "i18n.workflow.filter.all.workflows";
    private static final int DEFAULT_FILTER_VALUE = -1;
    private final WorkflowManager workflowManager;

    public WorkflowList() {
        this.workflowManager = WorkflowManager.getInstance();
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
            final Collection<Workflow> workflows = workflowManager.getWorkflows(orgId, dropdownLimit, dropdownCriteria);
            final JsonObject allWorkflowsJson = Json.object();

            allWorkflowsJson.put(WorkflowAttributes.NAME, dropdownRequest.i18nResolver().get(ALL_WORKFLOWS_I18N_KEY));
            allWorkflowsJson.put(WorkflowAttributes.ID, DEFAULT_FILTER_VALUE);

            optionContainer.addOption(allWorkflowsJson);

            for (final Workflow workflow : workflows) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(WorkflowAttributes.NAME, workflow.getName());
                jsonObject.put(WorkflowAttributes.ID, workflow.id());

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }
}
