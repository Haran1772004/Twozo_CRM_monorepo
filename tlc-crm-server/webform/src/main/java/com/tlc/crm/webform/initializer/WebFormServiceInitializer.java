package com.tlc.crm.webform.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.webform.cache.TemplateCache;
import com.tlc.crm.webform.url.impl.WebFormLinkGenerator;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class WebFormServiceInitializer implements ServiceInitializer {

    private final WebFormLinkGenerator webFormLinkGenerator;
    private final TemplateCache templateCache;

    private WebFormServiceInitializer() {
        webFormLinkGenerator = WebFormLinkGenerator.getInstance();
        templateCache = TemplateCache.getInstance();
    }

    private static final class Instance {
        private static final WebFormServiceInitializer WEB_FORM_SERVICE_INITIALIZER = new WebFormServiceInitializer();
    }

    public static WebFormServiceInitializer getInstance() {
        return Instance.WEB_FORM_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        webFormLinkGenerator.setBaseUrl(configurations);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        templateCache.clear();
    }
}
