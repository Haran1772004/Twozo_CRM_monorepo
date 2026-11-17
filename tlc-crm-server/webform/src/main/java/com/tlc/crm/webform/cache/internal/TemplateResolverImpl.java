package com.tlc.crm.webform.cache.internal;

import com.tlc.crm.webform.api.manager.TemplateManager;
import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.cache.TemplateCache;
import com.tlc.crm.webform.cache.TemplateResolver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the {@link Template} related details
 *
 * @author Kishore J
 * @version 1.0
 * @see TemplateResolver
 */
public class TemplateResolverImpl implements TemplateResolver {

    private final TemplateCache templateCache;
    private final TemplateManager templateManager;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private TemplateResolverImpl() {
        templateCache = TemplateCache.getInstance();
        templateManager = TemplateManager.getInstance();
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final TemplateResolverImpl INSTANCE = new TemplateResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link TemplateResolverImpl}.
     * </p>
     *
     * @return The {@link TemplateResolverImpl}
     */
    public static TemplateResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link Template}.
     */
    public Collection<Template> getAll() {
        if (!cacheLoaded.get()) {
            reloadTemplate();
        }

        return templateCache.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template ID.
     * @return The {@link Template}.
     */
    public Template get(final Long id) {
        final Template existingTemplate = templateCache.get(id);

        if (Objects.isNull(existingTemplate)) {
            final Template template = templateManager.getTemplate(id);

            if (Objects.nonNull(template)) {
                templateCache.setTemplate(template);
            }
            return template;
        }

        return existingTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Which contains the collection of template IDs.
     * @return If all the template exists it will return true, otherwise it will return false.
     */
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
     * <p>
     * Reloads the templates into cache.
     * </p>
     */
    private void reloadTemplate() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<Template> templates = templateManager.getAllTemplates();

                if (Objects.nonNull(templates)) {

                    for (final Template template : templates) {
                        templateCache.setTemplate(template);
                    }
                    cacheLoaded.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}
