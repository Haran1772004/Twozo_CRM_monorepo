package com.tlc.commons.service.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Listener to manage the {@link Bundle} event change activities
 *
 * @author Abishek
 * @version 1.0
 */
public abstract class BundleListener implements org.osgi.framework.BundleListener {

    private final Set<Long> STARTED_BUNDLES;
    private final Lock LOCK;
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleListener.class);

    protected BundleListener() {
        this.LOCK = new ReentrantLock();
        this.STARTED_BUNDLES = new HashSet<>();
    }

    /**
     * Triggered when {@link Bundle} state changes
     *
     * @param event Bundle event
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        final Bundle bundle = event.getBundle();
        final String symbolicName = bundle.getSymbolicName();

        if (symbolicName.startsWith("com.tlc")) {
            final int type = event.getType();

            LOGGER.info("Metafile listener action initiated for bundle : {}, Type : {}", symbolicName, type);

            if (type == BundleEvent.STARTED) {
                LOCK.lock();

                try {
                    final Long bundleId = bundle.getBundleId();

                    if (!STARTED_BUNDLES.contains(bundleId)) {
                        bundleStarted(bundle);
                        STARTED_BUNDLES.add(bundleId);
                    } else {
                        LOGGER.info("Bundle started called before bundle stop : {}, Type : {}", symbolicName, type);
                    }
                } finally {
                    LOCK.unlock();
                }
            } else if (type == BundleEvent.STOPPED) {
                LOCK.lock();

                try {
                    bundleStopped(bundle);
                    STARTED_BUNDLES.remove(bundle.getBundleId());
                } finally {
                    LOCK.unlock();
                }
            } else if(type == BundleEvent.STOPPING) {
                LOCK.lock();

                try {
                    bundleStopping(bundle);
                } finally {
                    LOCK.unlock();
                }
            }
        }
    }

    /**
     * Performs the {@link Bundle} startup activities
     *
     * @param bundle Started bundle
     */
    public void bundleStarted(final Bundle bundle) {
    }

    /**
     * Performs the activities should be done after the {@link Bundle} stopped
     *
     * @param bundle Stopped bundle
     */
    public void bundleStopped(final Bundle bundle) {
    }

    /**
     * Performs the activities should be done while the {@link Bundle} is stopping
     *
     * @param bundle Stopping bundle
     */
    public void bundleStopping(final Bundle bundle) {
    }

    /**
     * <p>
     * Retrieves the classes from the given bundle.
     * </p>
     *
     * @param bundle The bundle from which to retrieve the classes.
     * @return A collection of classes found in the bundle.
     */
    protected Collection<Class<?>> getClasses(final Bundle bundle) {
        return getClasses("", bundle);
    }

    /**
     * <p>
     * Retrieves the classes from the given bundle.
     * </p>
     *
     * @param path   The path name in which to look.
     * @param bundle The bundle from which to retrieve the classes.
     * @return A collection of classes found in the bundle.
     */
    protected Collection<Class<?>> getClasses(final String path, final Bundle bundle) {
        final Enumeration<URL> enumeration = bundle.findEntries("/", "*.class", true);
        final Collection<Class<?>> classes = new HashSet<>();

        if (Objects.nonNull(enumeration)) {
            while (enumeration.hasMoreElements()) {
                final URL url = enumeration.nextElement();
                final String file = url.getFile();

                if (file.contains("com/tlc") && file.contains(path)) {
                    final int indexOfClass = file.indexOf(".class");
                    final String className = file.substring((file.startsWith("/") ? 1 : 0), indexOfClass).replace("/", ".");

                    try {
                        final Class<?> classObj = bundle.loadClass(className);

                        classes.add(classObj);
                    } catch (Exception exception) {
                        throw ErrorCode.get(ErrorCodes.UNABLE_TO_LOAD_CLASS, String.format("Failed to load the class(%s) with the specified bundle(%s)...!", className, bundle.getSymbolicName()));
                    }
                }
            }
        }
        return classes;
    }
}
