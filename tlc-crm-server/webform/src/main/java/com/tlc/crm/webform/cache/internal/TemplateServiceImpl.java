package com.tlc.crm.webform.cache.internal;

import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.cache.TemplateResolver;
import com.tlc.crm.webform.cache.TemplateService;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Implements the {@link TemplateService} and provides functionality for accessing {@link Template}.
 *
 * @author Kishore J
 * @version 1.0
 * @see TemplateService
 */
public class TemplateServiceImpl implements TemplateService {

    private final TemplateResolver templateResolver;

    private TemplateServiceImpl() {
        templateResolver = TemplateResolver.getInstance();
    }

    private static class Instance {
        private static final TemplateServiceImpl INSTANCE = new TemplateServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link TemplateServiceImpl}.
     * </p>
     *
     * @return The {@link TemplateServiceImpl}
     */
    public static TemplateServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return The {@link Template}.
     */
    @Override
    public Template get(final Long id) {
        return templateResolver.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link Template}.
     */
    @Override
    public Collection<Template> getAll() {
        return templateResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the template id.
     * @return If the template details exist it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Long id) {
        return Objects.nonNull(templateResolver.get(id));
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Which contains the collection of template ids.
     * @return If the template details exist it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Set<Long> ids) {
        return templateResolver.exists(ids);
    }
}
