package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.WorkflowTemplateManager;
import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.WorkflowTemplateCache;
import com.tlc.crm.workflow.cache.resolver.WorkflowTemplateResolver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A resolver responsible for retrieving and storing details of the {@link WorkflowTemplate} from/to the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowTemplateResolverImpl implements WorkflowTemplateResolver {

    private final WorkflowTemplateCache workflowTemplateCache;
    private final WorkflowTemplateManager workflowTemplateManager;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private WorkflowTemplateResolverImpl() {
        this.workflowTemplateCache = WorkflowTemplateCache.getInstance();
        this.workflowTemplateManager = WorkflowTemplateManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final WorkflowTemplateResolverImpl INSTANCE = new WorkflowTemplateResolverImpl();
    }

    /**
     * Gets the instance of {@link WorkflowTemplateResolverImpl}.
     *
     * @return The {@link WorkflowTemplateResolverImpl}.
     */
    public static WorkflowTemplateResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return The {@link WorkflowTemplate}.
     */
    @Override
    public WorkflowTemplate get(final Long id) {
        final WorkflowTemplate existingWorkflowTemplate = workflowTemplateCache.get(id);

        if (Objects.isNull(existingWorkflowTemplate)) {
            final WorkflowTemplate workflowTemplate = workflowTemplateManager.getTemplate(id);

            if (Objects.nonNull(workflowTemplate)) {
                workflowTemplateCache.setTemplate(workflowTemplate);
            }
            return workflowTemplate;
        }

        return existingWorkflowTemplate;
    }

    /**
     * Gets all the templates which are come under the given category.
     *
     * @param id Refers the template category id.
     * @return The collection of {@link WorkflowTemplate}.
     */
    @Override
    public Collection<WorkflowTemplate> getByCategoryId(final Short id) {
        final Collection<WorkflowTemplate> existingWorkflowTemplates = workflowTemplateCache.getByCategoryId(id);

        if (Objects.isNull(existingWorkflowTemplates)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Collection<WorkflowTemplate> workflowTemplates = workflowTemplateManager.getTemplateByCategory(id);

                if (!workflowTemplates.isEmpty()) {

                    for (final WorkflowTemplate workflowTemplate : workflowTemplates) {
                        workflowTemplateCache.setTemplate(workflowTemplate);
                    }
                }
                return workflowTemplates;
            } finally {
                writeLock.unlock();
            }
        }
        return existingWorkflowTemplates;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link WorkflowTemplate}.
     */
    @Override
    public Collection<WorkflowTemplate> getAll() {
        if (!cacheLoaded.get()) {
            reloadTemplate();
        }

        return workflowTemplateCache.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Which contains the collection of template ids.
     * @return If all the template exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        final Collection<Long> templateIds = new HashSet<>();

        for (final Long id : ids) {

            if (Objects.nonNull(get(id))) {
                templateIds.add(id);
            }
        }

        return templateIds.containsAll(ids);
    }

    /**
     * Reloads the templates into the cache.
     */
    private void reloadTemplate() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<WorkflowTemplate> workflowTemplates = workflowTemplateManager.getAllTemplates();

                if (!workflowTemplates.isEmpty()) {
                    workflowTemplateCache.setTemplates(workflowTemplates);
                    cacheLoaded.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

}
