package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.workflow.api.manager.WorkflowTemplateManager;
import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.WorkflowTemplateCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stores the templates into the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowTemplateCacheImpl implements WorkflowTemplateCache {

    private final Cache<Short, Map<Long, WorkflowTemplate>> templateCache;
    private final WorkflowTemplateManager workflowTemplateManager;
    private final ReentrantReadWriteLock lock;

    private WorkflowTemplateCacheImpl() {
        this.templateCache = CacheManager.getInstance().createCache();
        this.workflowTemplateManager = WorkflowTemplateManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final WorkflowTemplateCacheImpl INSTANCE = new WorkflowTemplateCacheImpl();
    }

    /**
     * Gets the {@link WorkflowTemplateCache} instance.
     *
     * @return {@link WorkflowTemplateCacheImpl}.
     */
    public static WorkflowTemplateCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<WorkflowTemplate> workflowTemplates = workflowTemplateManager.getAllTemplates();

        templateCache.putAll(groupTemplates(workflowTemplates));
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return The {@link WorkflowTemplate}.
     */
    @Override
    public WorkflowTemplate get(final Long id) {
        final List<WorkflowTemplate> workflowTemplates = new ArrayList<>();
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            templateCache.forEach((category, groupedTemplates) -> {

                if (groupedTemplates.containsKey(id)) {
                    workflowTemplates.add(groupedTemplates.get(id));
                }
            });

            return workflowTemplates.isEmpty() ? null : workflowTemplates.get(0);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link WorkflowTemplate}.
     */
    @Override
    public Collection<WorkflowTemplate> getAll() {
        final Collection<WorkflowTemplate> workflowTemplates = new ArrayList<>();
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            templateCache.forEach((category, groupedTemplates) -> workflowTemplates.addAll(groupedTemplates.values()));

            return workflowTemplates;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the category id of the template.
     * @return The collection of {@link WorkflowTemplate}.
     */
    @Override
    public Collection<WorkflowTemplate> getByCategoryId(final Short id) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            return templateCache.get(id).values();
        } finally {
            readLock.unlock();
        }

    }

    /**
     * {@inheritDoc}
     *
     * @param workflowTemplate Represents the {@link WorkflowTemplate}.
     */
    @Override
    public void setTemplate(final WorkflowTemplate workflowTemplate) {
        templateCache.computeIfAbsent(workflowTemplate.category().getId(), k -> new HashMap<>()).put(workflowTemplate.id(), workflowTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param workflowTemplates Represents the collection of {@link WorkflowTemplate}.
     */
    @Override
    public void setTemplates(final Collection<WorkflowTemplate> workflowTemplates) {
        templateCache.putAll(groupTemplates(workflowTemplates));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        templateCache.clear();
    }

    /**
     * Groups the {@link WorkflowTemplate} based on the category.
     *
     * @param workflowTemplates Refers the collection of {@link WorkflowTemplate}.
     * @return The grouped workflowTemplates.
     */
    private Map<Short, Map<Long, WorkflowTemplate>> groupTemplates(final Collection<WorkflowTemplate> workflowTemplates) {
        return workflowTemplates.stream().collect(Collectors.groupingBy(template -> template.category().getId(),
                Collectors.toMap(WorkflowTemplate::id, Function.identity())));
    }
}
