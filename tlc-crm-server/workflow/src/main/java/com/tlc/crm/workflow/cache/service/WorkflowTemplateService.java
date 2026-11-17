package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.service.internal.WorkflowTemplateServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * Provides the abstract way to deal with the {@link WorkflowTemplate}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface WorkflowTemplateService {

    /**
     * Gets the instance of {@link WorkflowTemplateService}.
     *
     * @return The {@link WorkflowTemplateService}.
     */
    static WorkflowTemplateService getInstance() {
        return WorkflowTemplateServiceImpl.getInstance();
    }

    /**
     * Gets the {@link WorkflowTemplate} by its id.
     *
     * @param id Represents the template id.
     * @return {@link WorkflowTemplate} which contains the template info.
     */
    WorkflowTemplate get(final Long id);

    /**
     * Gets all the templates.
     *
     * @return The {@link Collection} of {@link WorkflowTemplate}.
     */
    Collection<WorkflowTemplate> getAll();

    /**
     * Gets all the templates which are come under the given category id.
     *
     * @param id Refers the template category id.
     * @return The collection of {@link WorkflowTemplate}.
     */
    Collection<WorkflowTemplate> getByCategoryId(final Short id);

    /**
     * Checks whether the template exist or not.
     *
     * @param id Represents the template id.
     * @return If the template exist it will return true, otherwise it will return false.
     */
    boolean exists(final Long id);

    /**
     * Checks whether all the given template ids are exists or not.
     *
     * @param ids Which contains the collection of template ids.
     * @return If the templates are exist it will return true, otherwise it will return false.
     */
    boolean exists(final Set<Long> ids);
}
