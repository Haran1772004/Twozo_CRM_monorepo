package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.internal.WorkflowFieldCacheImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides an abstract way to stores the {@link WorkflowField} into cache
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface WorkflowFieldCache extends LocalCache {

    /**
     * <p>
     * Retrieves the instance of {@link WorkflowFieldCache}
     * </p>
     *
     * @return The {@link WorkflowFieldCache} instance
     */
    static WorkflowFieldCache getInstance() {
        return WorkflowFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link WorkflowFieldCache} by trigger action source id, If not found in the cache, fetch it from the DB
     * and stores it in cache and returns the {@link Collection} of {@link WorkflowField}
     * </p>
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return {@link WorkflowField} An entity which holds the attributes of {@link WorkflowField}
     */
    Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId);

    /**
     * <p>
     * Sets the {@link Collection} of {@link WorkflowField} into the workflow field cache
     * </P>
     *
     * @param workflowFields Represents the {@link Collection} of {@link WorkflowField}
     */
    void setWorkflowFields(final Collection<WorkflowField> workflowFields);

    /**
     * <p>
     * Sets the {@link WorkflowField} into the workflow field cache
     * </p>
     *
     * @param workflowField Represents the {@link WorkflowField}
     */
    void setWorkflowField(final WorkflowField workflowField);

    /**
     * <p>
     * Retrieves the {@link WorkflowField} for the given id
     * </p>
     *
     * @param id Represents the id of {@link WorkflowField}
     * @return {@link WorkflowField}
     */
    WorkflowField getWorkflowField(final Long id);

    /**
     * <p>
     * Gets the {@link Map}of {@link WorkflowField} for the given {@link Collection} of alias names
     * </p>
     *
     * @param aliasNames Represents the {@link Collection}of alias names
     * @return {@link Map} of workflow fields
     */
    Map<String, WorkflowField> getWorkflowFields(final Collection<String> aliasNames);

    /**
     * <p>
     * Gets the {@link WorkflowField} for the given alias name
     * </p>
     *
     * @param aliasName Represents the alias name
     * @return {@link WorkflowField}
     */
    WorkflowField getWorkflowField(final String aliasName);
}
