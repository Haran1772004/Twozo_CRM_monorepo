package com.tlc.crm.webform.cache;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.cache.internal.TemplateCacheImpl;

import java.util.Collection;

/**
 * Provides an abstract way to deal with {@link Template}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TemplateCache extends LocalCache {

    /**
     * Gets the instance of {@link TemplateCache}
     *
     * @return {@link TemplateCache}
     */
    static TemplateCache getInstance() {
        return TemplateCacheImpl.getInstance();
    }

    /**
     * Gets the {@link Template} by its id from cache.
     *
     * @param id Represents the template id.
     * @return {@link Template} which contains the template info.
     */
    Template get(final Long id);

    /**
     * Gets all the templates from the cache.
     *
     * @return {@link Collection} of {@link Template}.
     */
    Collection<Template> getAll();

    /**
     * <p>
     * Sets the template into the cache
     * </p>
     *
     * @param template Represents the {@link Template}
     */
    void setTemplate(final Template template);
}
