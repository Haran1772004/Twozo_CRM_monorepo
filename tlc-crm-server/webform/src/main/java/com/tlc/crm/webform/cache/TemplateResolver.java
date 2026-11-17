package com.tlc.crm.webform.cache;

import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.cache.internal.TemplateResolverImpl;

import java.util.Collection;

/**
 * <p>
 * A resolver that resolves the {@link Template} related details.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface TemplateResolver {

    /**
     * <p>
     * Gets the instance of {@link TemplateResolver}.
     * </p>
     *
     * @return The {@link TemplateResolver}
     */
    static TemplateResolver getInstance() {
        return TemplateResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link Template} by its id. If not cached, retrieves data from the database,
     * caches it, and returns template from the cache.
     * </p>
     *
     * @param id Represents the template ID.
     * @return {@link Template} which contains the template info.
     */
    Template get(final Long id);

    /**
     * <p>
     * Gets the collection of templates. If not cached, retrieves data from the database,
     * caches it, and returns templates from the cache.
     * </p>
     *
     * @return {@link Collection} of {@link Template}.
     */
    Collection<Template> getAll();

    /**
     * <p>
     * Checks whether all the templates exist or not for the given template IDs. If not cached, retrieves data
     * from the database, caches it, and returns whether templates exist or not from the cache.
     * </p>
     *
     * @param ids Which contains the collection of template IDs.
     * @return If all the template exists it will return true, otherwise it will return false
     */
    boolean exists(final Collection<Long> ids);
}