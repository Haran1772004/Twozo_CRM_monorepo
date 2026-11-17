package com.tlc.crm.webform.cache;

import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.cache.internal.TemplateServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Provides an abstract way to deal with {@link Template}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface TemplateService {

    /**
     * <p>
     * Gets the instance of {@link TemplateService}.
     * </p>
     *
     * @return The {@link TemplateService}
     */
    static TemplateService getInstance() {
        return TemplateServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link Template} by its id.
     * </p>
     *
     * @param id Represents the template id.
     * @return {@link Template} which contains the template info.
     */
    Template get(final Long id);

    /**
     * <p>
     * Gets all the templates.
     * </p>
     *
     * @return {@link Collection} of {@link Template}.
     */
    Collection<Template> getAll();

    /**
     * <p>
     * Checks whether the template is exists or not.
     * </p>
     *
     * @param id Represents the template id.
     * @return If the template details exist it will return true, otherwise it will return false
     */
    boolean exists(final Long id);

    /**
     * <p>
     * Checks whether all the given templates are exists or not.
     * </p>
     *
     * @param ids Which contains the collection of template ids.
     * @return If the template details exist it will return true, otherwise it will return false.
     */
    boolean exists(final Set<Long> ids);
}