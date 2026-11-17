package com.tlc.crm.workflow.service;

import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.service.internal.WorkflowCriteriaFieldServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Manages the {@link WorkflowCriteriaField} related services
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface WorkflowCriteriaFieldService {

    /**
     * Gets the instance of {@link WorkflowCriteriaFieldService}
     *
     * @return The {@link WorkflowCriteriaFieldService} instance
     */
    static WorkflowCriteriaFieldService getInstance() {
        return WorkflowCriteriaFieldServiceImpl.getInstance();
    }

    /**
     * </p>
     * Gets the {@link Collection} of {@link WorkflowCriteriaField} for the given trigger source id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param triggerSourceId Represents the trigger source id
     * @return {@link Collection} of {@link WorkflowCriteriaField}
     */
    Collection<WorkflowCriteriaField> getFields(final long orgId, final Short triggerSourceId);

    /**
     * </p>
     * Gets the {@link Collection} of {@link WorkflowCriteriaField} for the given action source id.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param actionSourceId Represents the action source id
     * @return {@link Collection} of {@link WorkflowCriteriaField}
     */
    Collection<WorkflowCriteriaField> getFields(final long orgId, final Long actionSourceId);
}
