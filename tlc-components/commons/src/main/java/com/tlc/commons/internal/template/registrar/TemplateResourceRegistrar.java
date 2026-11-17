package com.tlc.commons.internal.template.registrar;

import com.tlc.commons.internal.template.service.TemplateServiceProvider;
import com.tlc.commons.template.model.TemplateWrapper;
import com.tlc.commons.template.registrar.AbstractTemplateResourceRegistrar;
import com.tlc.commons.template.service.TemplateService;

/**
 * <p>
 * The registrar that registers the given template
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class TemplateResourceRegistrar extends AbstractTemplateResourceRegistrar {

    private TemplateResourceRegistrar() {
    }

    private static class InstanceHolder {
        private static final TemplateResourceRegistrar TEMPLATE_RESOURCE_REGISTRAR = new TemplateResourceRegistrar();
    }

    /**
     * <p>
     * Gets the {@link TemplateResourceRegistrar} instance
     * </p>
     *
     * @return The {@link TemplateResourceRegistrar}
     */
    public static TemplateResourceRegistrar getInstance() {
        return InstanceHolder.TEMPLATE_RESOURCE_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link TemplateService}
     */
    @Override
    public TemplateService<String, ? extends TemplateWrapper> getTemplateService() {
        return TemplateServiceProvider.getVelocityTemplateService();
    }
}
