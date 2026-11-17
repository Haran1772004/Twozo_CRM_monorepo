package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.resolver.WorkflowTemplateResolver;
import com.tlc.crm.workflow.cache.service.WorkflowTemplateService;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Provides the services to handle the templates.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowTemplateServiceImpl implements WorkflowTemplateService {

    private final WorkflowTemplateResolver workflowTemplateResolver;

    private WorkflowTemplateServiceImpl() {
        this.workflowTemplateResolver = WorkflowTemplateResolver.getInstance();
    }

    private static class Instance {
        private static final WorkflowTemplateServiceImpl INSTANCE = new WorkflowTemplateServiceImpl();
    }

    /**
     * Gets the instance of {@link WorkflowTemplateServiceImpl}.
     *
     * @return The {@link WorkflowTemplateServiceImpl}.
     */
    public static WorkflowTemplateServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return The template.
     */
    @Override
    public WorkflowTemplate get(final Long id) {
        return workflowTemplateResolver.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link WorkflowTemplate}.
     */
    @Override
    public Collection<WorkflowTemplate> getAll() {
        return workflowTemplateResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the template category id.
     * @return The collection of {@link WorkflowTemplate}.
     */
    @Override
    public Collection<WorkflowTemplate> getByCategoryId(final Short id) {
        return workflowTemplateResolver.getByCategoryId(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return If the template exist it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Long id) {
        return Objects.nonNull(workflowTemplateResolver.get(id));
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Which contains the collection of template ids.
     * @return If the templates are exist it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Set<Long> ids) {
        return workflowTemplateResolver.exists(ids);
    }
}
