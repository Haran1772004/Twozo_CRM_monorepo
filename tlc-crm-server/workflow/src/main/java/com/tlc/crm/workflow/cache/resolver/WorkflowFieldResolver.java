package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.resolver.internal.WorkflowFieldResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * The resolver responsible for retrieving and storing the details of {@link WorkflowField} from/to cache
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface WorkflowFieldResolver {

    /**
     * <p>
     * Gets the instance of {@link WorkflowFieldResolver}
     * </p>
     *
     * @return The {@link WorkflowFieldResolver}
     */
    static WorkflowFieldResolver getInstance() {
        return WorkflowFieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link WorkflowField} mapped for the given trigger action source id
     * </p>
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return {@link Collection} of {@link WorkflowField}which contains the workflow field info
     */
    Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId);

    /**
     * <p>
     * Gets the {@link WorkflowField} mapped for the given id
     * </p>
     *
     * @param id Represents the {@link WorkflowField} id
     * @return {@link WorkflowField}
     */
    WorkflowField get(final Long id);

    /**
     * <p>
     * Checks whether the given workflow fields ids are present are not
     * </p>
     *
     * @param ids Represents the {@link Collection} of workflow field id
     * @return true if all the ids are present or not
     */
    boolean exists(final Collection<Long> ids);

    /**
     * <p>
     * Gets the {@link Map} of {@link WorkflowField} for the given {@link Collection} of alias names
     * </p>
     *
     * @param aliasNames {@link Collection} of alias names
     * @return {@link Map} {@link WorkflowField} for the given {@link Collection} of alias names
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
