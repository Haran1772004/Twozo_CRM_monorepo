package com.tlc.notification.internal.template.mail.registrar;

import com.tlc.commons.template.model.TemplateWrapper;
import com.tlc.commons.template.registrar.AbstractTemplateResourceRegistrar;
import com.tlc.commons.internal.template.registrar.TemplateResourceRegistrar;
import com.tlc.commons.template.service.TemplateService;
import com.tlc.notification.internal.template.mail.service.VelocityEmailTemplateServiceProvider;
import com.tlc.notification.internal.template.mail.service.VelocityEmailTemplateService;

/**
 * <p>
 * The registrar that registers the given email template
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailTemplateResourceRegistrar extends AbstractTemplateResourceRegistrar {

    private EmailTemplateResourceRegistrar() {
    }

    private static class InstanceHolder {
        private static final EmailTemplateResourceRegistrar TEMPLATE_RESOURCE_REGISTRAR = new EmailTemplateResourceRegistrar();
    }

    /**
     * <p>
     * Gets the {@link TemplateResourceRegistrar} instance
     * </p>
     *
     * @return The {@link TemplateResourceRegistrar}
     */
    public static EmailTemplateResourceRegistrar getInstance() {
        return InstanceHolder.TEMPLATE_RESOURCE_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link VelocityEmailTemplateService}
     */
    @Override
    protected TemplateService<String, ? extends TemplateWrapper> getTemplateService() {
        return VelocityEmailTemplateServiceProvider.getVelocityMailTemplateService();
    }
}
