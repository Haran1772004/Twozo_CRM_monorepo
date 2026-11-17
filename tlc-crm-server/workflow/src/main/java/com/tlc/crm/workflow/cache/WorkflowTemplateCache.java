package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.internal.WorkflowTemplateCacheImpl;

import java.util.Collection;

/**
 * Provides an abstract way to deal with {@link WorkflowTemplate}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface WorkflowTemplateCache extends LocalCache {

    /**
     * Gets the instance of {@link WorkflowTemplateCache}
     *
     * @return {@link WorkflowTemplateCache}
     */
    static WorkflowTemplateCache getInstance() {
        return WorkflowTemplateCacheImpl.getInstance();
    }

    /**
     * Gets the {@link WorkflowTemplate} by its id from cache.
     *
     * @param id Represents the template id.
     * @return {@link WorkflowTemplate} which contains the template info.
     */
    WorkflowTemplate get(final Long id);

    /**
     * Gets all the templates from the cache.
     *
     * @return {@link Collection} of {@link WorkflowTemplate}.
     */
    Collection<WorkflowTemplate> getAll();

    /**
     * Gets all the templates for the category id given.
     *
     * @param id Refers the category id of the template.
     * @return {@link Collection} of {@link WorkflowTemplate}.
     */
    Collection<WorkflowTemplate> getByCategoryId(final Short id);

    /**
     * Sets the workflowTemplate into the cache.
     *
     * @param workflowTemplate Represents the {@link WorkflowTemplate}.
     */
    void setTemplate(final WorkflowTemplate workflowTemplate);

    /**
     * Sets the workflowTemplates into the cache.
     *
     * @param workflowTemplates Represents the collection of {@link WorkflowTemplate}.
     */
    void setTemplates(final Collection<WorkflowTemplate> workflowTemplates);
}
