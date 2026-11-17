package com.tlc.commons.internal.template.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.template.service.TemplateService;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.internal.template.model.VelocityTemplateWrapper;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * A class is responsible for processing Velocity templates and producing {@link VelocityTemplateWrapper}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class VelocityTemplateService implements TemplateService<String, VelocityTemplateWrapper> {

    private static final AtomicReference<VelocityEngine> VELOCITY_ENGINE_REFERENCE = new AtomicReference<>();

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

        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
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
        final Template template = VELOCITY_ENGINE_REFERENCE.get().getTemplate(url);

        return new VelocityTemplateWrapper(template);
    }

    /**
     * <p>
     * Registers the instance of the {@link VelocityEngine}
     * </p>
     *
     * @param velocityEngine Represents the VelocityEngine
     */
    private void register(final VelocityEngine velocityEngine) {
        if (!VELOCITY_ENGINE_REFERENCE.compareAndSet(null, velocityEngine)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
    }

    /**
     * <p>
     * Unregisters the instance of the {@link VelocityEngine}
     * </p>
     */
    private void unregister() {
        VELOCITY_ENGINE_REFERENCE.getAndSet(null);
    }
}
