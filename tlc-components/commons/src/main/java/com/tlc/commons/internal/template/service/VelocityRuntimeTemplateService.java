package com.tlc.commons.internal.template.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.internal.template.model.VelocityTemplateWrapper;
import com.tlc.commons.template.service.TemplateService;
import com.tlc.commons.util.Configurations;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * A class is responsible for processing Velocity templates and producing {@link VelocityTemplateWrapper}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class VelocityRuntimeTemplateService implements TemplateService<String, VelocityTemplateWrapper> {

    private final AtomicReference<VelocityEngine> velocityEngineReference;

    private VelocityRuntimeTemplateService() {
        velocityEngineReference = new AtomicReference<>();
    }

    private static final class InstanceHolder {
        private static final VelocityRuntimeTemplateService VELOCITY_RUNTIME_TEMPLATE_SERVICE = new VelocityRuntimeTemplateService();
    }

    public static VelocityRuntimeTemplateService getInstance() {
        return InstanceHolder.VELOCITY_RUNTIME_TEMPLATE_SERVICE;
    }

    /**
     * <p>
     * Initializes the {@link VelocityEngine} with properties to load resources from the classpath and registers the engine
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    @Override
    public void start(final Configurations configurations) {
        final VelocityEngine velocityEngine = new VelocityEngine();

        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
        velocityEngine.setProperty("classpath.resource.loader.class", StringResourceLoader.class.getName());
        velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.Log4JLogChute");
        velocityEngine.setProperty("runtime.log.logsystem.log4j.logger", "my.custom.logger");
        velocityEngine.init();

        register(velocityEngine);
    }

    /**
     * <p>
     * Stops the {@link VelocityEngine} service
     * </p>
     */
    @Override
    public void stop() {
        unregister();
    }

    /**
     * <p>
     * Fetches a {@link VelocityTemplateWrapper} for the given template URL
     * </p>
     *
     * @param url the URL of the template to be retrieved
     * @return The {@link VelocityTemplateWrapper}
     */
    @Override
    public VelocityTemplateWrapper get(final String url) {
        final Template template = velocityEngineReference.get().getTemplate(url);

        return new VelocityTemplateWrapper(template);
    }

    /**
     * Adds or updates a Velocity template in the in-memory string resource repository.
     *
     * <p>The provided template content is registered against the given template name,
     * allowing the Velocity engine to load and process the template dynamically
     * without requiring a physical template file.
     *
     * @param templateName    Unique name used to identify the template.
     * @param templateContent Template HTML/content to be stored in the Velocity repository.
     * @throws RuntimeException if the Velocity template service has not been initialized.
     */
    public void addTemplate(final String templateName, final String templateContent) {
        final VelocityEngine velocityEngine = velocityEngineReference.get();

        if (Objects.isNull(velocityEngine)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
        StringResourceLoader.getRepository().putStringResource(templateName, templateContent);
    }

    /**
     * Removes a dynamically added Velocity template from the in-memory resource repository.
     *
     * <p>This method should be called after template processing is completed to avoid
     * retaining unnecessary template resources in memory.
     *
     * @param templateName Name of the template to remove from the repository.
     */
    public void removeTemplate(final String templateName) {
        StringResourceLoader.getRepository().removeStringResource(templateName);
    }

    /**
     * <p>
     * Registers the instance of the {@link VelocityEngine}
     * </p>
     *
     * @param velocityEngine Represents the VelocityEngine
     */
    private void register(final VelocityEngine velocityEngine) {
        if (!velocityEngineReference.compareAndSet(null, velocityEngine)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
    }

    /**
     * <p>
     * Unregisters the instance of the {@link VelocityEngine}
     * </p>
     */
    private void unregister() {
        velocityEngineReference.getAndSet(null);
    }
}
