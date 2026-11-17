package com.tlc.crm.webform.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.webform.api.manager.TemplateManager;
import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.cache.TemplateCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Stores the template details into the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class TemplateCacheImpl implements TemplateCache {

    private final Cache<Long, Template> templateCache;
    private final TemplateManager templateManager;

    private TemplateCacheImpl() {
        this.templateCache = CacheManager.getInstance().createCache();
        this.templateManager = TemplateManager.getInstance();
    }

    private static class Instance {
        private static final TemplateCacheImpl INSTANCE = new TemplateCacheImpl();
    }

    /**
     * Gets the {@link TemplateCache} instance
     *
     * @return {@link TemplateCacheImpl}
     */
    public static TemplateCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<Template> templates = templateManager.getAllTemplates();

        if (Objects.nonNull(templates)) {
            templates.forEach(template -> templateCache.put(template.id(), template));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        templateCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return The {@link Template}.
     */
    @Override
    public Template get(final Long id) {
        return templateCache.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link Template}.
     */
    @Override
    public Collection<Template> getAll() {
        final Collection<Template> templates = new ArrayList<>();

        templateCache.forEach((id, template) -> templates.add(template));
        return templates;
    }

    /**
     * {@inheritDoc}
     *
     * @param template Represents the {@link Template}
     */
    @Override
    public void setTemplate(final Template template) {
        templateCache.put(template.id(), template);
    }
}
