package com.tlc.commons.listeners;

import com.tlc.commons.service.listener.BundleListener;
import com.tlc.commons.internal.template.store.TemplateWrapperStore;
import com.tlc.commons.template.model.TemplateRegistrarData;
import com.tlc.commons.internal.template.registrar.TemplateResourceRegistrar;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * A Listener that listens templates from resource files contained within the bundles
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class TemplateListener extends BundleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateListener.class);
    private static final String TEMPLATE_FILES_BASE_PATH = "/resources/templates";
    private static final String VM_FILE_EXTENSION = "*.vm";

    private final TemplateWrapperStore templateWrapperStore;
    private final TemplateResourceRegistrar templateResourceRegistrar;

    public TemplateListener() {
        templateWrapperStore = TemplateWrapperStore.getInstance();
        templateResourceRegistrar = TemplateResourceRegistrar.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bundle Started bundle
     */
    @Override
    public void bundleStarted(final Bundle bundle) {
        final TemplateRegistrarData templateRegistrarData = new TemplateRegistrarData();

        templateRegistrarData.setModuleName(bundle.getSymbolicName());
        templateRegistrarData.setTemplateUrls(fetchTemplateUrls(bundle));

        templateResourceRegistrar.register(templateRegistrarData);
    }

    /**
     * {@inheritDoc}
     *
     * @param bundle Represents stopped {@link Bundle}
     */
    public void bundleStopped(final Bundle bundle) {
        final String symbolicName = bundle.getSymbolicName();
        final TemplateRegistrarData templateRegistrarData = new TemplateRegistrarData();

        templateRegistrarData.setModuleName(symbolicName);

        templateWrapperStore.unregister(symbolicName);
        templateResourceRegistrar.unregister(templateRegistrarData);
        LOGGER.info("Templates have been successfully removed : {}", symbolicName);
    }

    /**
     * <p>
     * Fetches template URLs from the specified bundle.
     * </p>
     *
     * @param bundle Represents the {@link Bundle}
     * @return A {@link Collection} of template file URLs found in the bundle
     */
    private Collection<String> fetchTemplateUrls(final Bundle bundle) {
        final Collection<String> urls = new HashSet<>();
        final Enumeration<URL> entries = bundle.findEntries(TEMPLATE_FILES_BASE_PATH, VM_FILE_EXTENSION, false);

        if (Objects.nonNull(entries)) {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(bundle.adapt(BundleWiring.class).getClassLoader());

                while (entries.hasMoreElements()) {
                    urls.add(entries.nextElement().getPath());
                }
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
        return urls;
    }
}
