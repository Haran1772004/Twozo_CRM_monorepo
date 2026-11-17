package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.resolver.WorkflowFieldResolver;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the service to handle the {@link WorkflowField}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowFieldServiceImpl implements WorkflowFieldService {

    private final WorkflowFieldResolver workflowFieldResolver;

    private WorkflowFieldServiceImpl() {
        this.workflowFieldResolver = WorkflowFieldResolver.getInstance();
    }

    private static class Instance {
        private static final WorkflowFieldServiceImpl INSTANCE = new WorkflowFieldServiceImpl();
    }

    /**
     * Gets the {@link WorkflowFieldServiceImpl} instance
     *
     * @return {@link WorkflowFieldServiceImpl}
     */
    public static WorkflowFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return {@link Collection} of {@link WorkflowField} mapped for the given trigger action source id
     */
    @Override
    public Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId) {
        return workflowFieldResolver.getWorkflowFieldsBySource(triggerActionSourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Refers the collection of workflow field ids
     * @return true if the all ids are present
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        return workflowFieldResolver.exists(ids);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the workflow field id
     * @return the {@link WorkflowField} mapped for the given id
     */
    @Override
    public WorkflowField get(final Long id) {
        return workflowFieldResolver.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasNames Represents the {@link Collection} of alias names
     * @return {@link Map} of {@link WorkflowField}
     */
    @Override
    public Map<String, WorkflowField> getWorkflowFields(final Collection<String> aliasNames) {
        return workflowFieldResolver.getWorkflowFields(aliasNames);
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName Represents the alias name of the workflow field
     * @return {@link WorkflowField}
     */
    @Override
    public WorkflowField getWorkflowField(final String aliasName) {
        return workflowFieldResolver.getWorkflowField(aliasName);
    }
}
