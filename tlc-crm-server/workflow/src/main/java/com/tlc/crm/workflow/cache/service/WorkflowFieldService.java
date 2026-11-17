package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.internal.WorkflowFieldServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides an abstract way to deal with {@link WorkflowField}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface WorkflowFieldService {

    /**
     * Gets the instance of {@link WorkflowFieldService}
     *
     * @return The {@link WorkflowFieldService}
     */
    static WorkflowFieldService getInstance() {
        return WorkflowFieldServiceImpl.getInstance();
    }

    /**
     * Gets the {@link Collection} of {@link WorkflowField} for the given trigger action source id
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return {@link Collection} of {@link WorkflowField}
     */
    Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId);

    /**
     * <p>
     * Checks whether the given workflow fields are present or not
     * </p>
     *
     * @param ids Refers the collection of workflow field ids
     * @return true, If the workflow fields ids are exist
     */
    boolean exists(final Collection<Long> ids);

    /**
     * <p>
     * Gets the {@link WorkflowField} for the given id
     * </p>
     *
     * @param id Represents the workflow field id
     * @return {@link WorkflowField}
     */
    WorkflowField get(final Long id);

    /**
     * <p>
     * Gets the {@link Map} workflow fields for the given {@link Collection} of alias names
     * </p>
     *
     * @param aliasNames Represents the {@link Collection} of alias names
     * @return {@link Map} of {@link WorkflowField}
     */
    Map<String, WorkflowField> getWorkflowFields(final Collection<String> aliasNames);

    /**
     * <p>
     * Gets the {@link WorkflowField} for the given alias name
     * </p>
     *
     * @param aliasName Represents the alias name of the workflow field
     * @return {@link WorkflowField}
     */
    WorkflowField getWorkflowField(final String aliasName);
}
