package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.resolver.internal.WorkflowTemplateResolverImpl;

import java.util.Collection;

/**
 * A resolver responsible for retrieving and storing details of the {@link WorkflowTemplate} from/to the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface WorkflowTemplateResolver {

    /**
     * Gets the instance of {@link WorkflowTemplateResolver}.
     *
     * @return The {@link WorkflowTemplateResolver}
     */
    static WorkflowTemplateResolver getInstance() {
        return WorkflowTemplateResolverImpl.getInstance();
    }

    /**
     * Gets the {@link WorkflowTemplate} by its id. If not found in the cache, retrieves data from the DB
     * and stores in cache and returns the template.
     *
     * @param id Represents the template id.
     * @return {@link WorkflowTemplate} which contains the template info.
     */
    WorkflowTemplate get(final Long id);

    /**
     * Gets the collection of templates. If not found in the cache, retrieves data from the DB,
     * stores in cache and returns templates.
     *
     * @return {@link Collection} of {@link WorkflowTemplate}.
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
     * Checks whether all the templates are exist or not for the given template ids.
     *
     * @param ids Which contains the collection of template ids.
     * @return If all the template exists it will return true, otherwise it will return false.
     */
    boolean exists(final Collection<Long> ids);
}
