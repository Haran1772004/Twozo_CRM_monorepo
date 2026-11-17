package com.tlc.commons.service.listener;

import com.tlc.commons.internal.BundleContainerImpl;
import org.osgi.framework.Bundle;
import com.tlc.commons.service.TlcBundleActivator;
import org.osgi.framework.BundleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Listener to manage the {@link Bundle} event change activities in prerequisite order of bundles
 *
 * @author Sunder
 * @version 1.0
 */
public abstract class SortedBundleListener implements org.osgi.framework.BundleListener {

    private final BundleContainerImpl bundleContainer;
    private final Set<String> bundlesListenedByListener;
    private final Map<Bundle, Set<String>> waitingBundles;
    private static final Set<String> STARTED_BUNDLES = new HashSet<>();
    private final Lock LOCK;
    private static final String BUNDLE_PREFIX = "com.tlc";
    private static final Logger LOGGER = LoggerFactory.getLogger(SortedBundleListener.class);

    protected SortedBundleListener() {
        this.LOCK = new ReentrantLock();
        this.bundlesListenedByListener = new HashSet<>();
        this.waitingBundles = new ConcurrentHashMap<>();
        this.bundleContainer = BundleContainerImpl.getInstance();
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

        if (symbolicName.startsWith(BUNDLE_PREFIX)) {
            final int type = event.getType();

            LOGGER.info("Metafile listener action initiated for bundle : {}, Type : {}", symbolicName, type);

            switch (type) {
                case BundleEvent.STARTED -> {
                    LOCK.lock();

                    try {
                        STARTED_BUNDLES.add(bundle.getSymbolicName());

                        if (!bundlesListenedByListener.contains(symbolicName) && !waitingBundles.containsKey(bundle)) {
                            processBundleStart(bundle, getPrerequisiteBundleSet(bundle));
                        } else {
                            LOGGER.info("Bundle started called before bundle stop : {}, Type : {}", symbolicName, type);
                        }
                    } finally {
                        LOCK.unlock();
                    }
                }
                case BundleEvent.STOPPING -> {
                    LOCK.lock();

                    try {
                        bundleStopping(bundle);
                    } finally {
                        LOCK.unlock();
                    }
                }
                case BundleEvent.STOPPED -> {
                    LOCK.lock();

                    try {
                        bundleStopped(bundle);
                        bundlesListenedByListener.remove(symbolicName);
                    } finally {
                        LOCK.unlock();
                    }
                }
            }
        }
    }

    /**
     * Retrieves non started prerequisite bundles from the bundle
     *
     * @param bundle Started bundle
     * @return Set of prerequisite bundles
     */
    private Set<String> getPrerequisiteBundleSet(final Bundle bundle) {
        final String prerequisiteBundlesTag = "Prerequisite-Bundles";
        final String prerequisiteBundles = bundle.getHeaders().get(prerequisiteBundlesTag);
        final Set<String> prerequisiteBundleSet = new LinkedHashSet<>();
        final String crmBundlePrefix = BUNDLE_PREFIX + ".crm";

        if (prerequisiteBundles != null) {
            final String[] prerequisiteBundlesArray = prerequisiteBundles.split(";");

            LOGGER.debug("Prerequisite bundles to be started before {} are {}", bundle.getSymbolicName(),
                    prerequisiteBundlesArray);

            for (final String symbolicName : prerequisiteBundlesArray) {
                final String prerequisiteBundleName = symbolicName.trim();

                if (prerequisiteBundleName.startsWith(crmBundlePrefix) && !bundlesListenedByListener.contains(prerequisiteBundleName)) {
                    if (waitingBundles.keySet().stream().anyMatch(
                            waitingBundle -> prerequisiteBundleName.equals(waitingBundle.getSymbolicName())
                                    && waitingBundles.get(waitingBundle).contains(bundle.getSymbolicName()))) {
                        LOGGER.warn("Bundles {} and {} have a cyclic prerequisite for each other",
                                prerequisiteBundleName, bundle.getSymbolicName());
                        LOGGER.warn("It may lead the startup activities of bundles in an unexpected order");
                    } else if (!STARTED_BUNDLES.contains(prerequisiteBundleName)) {
                        prerequisiteBundleSet.add(prerequisiteBundleName);
                    }
                }
            }
        }
        return prerequisiteBundleSet;
    }

    /**
     * Processes the bundle startup activities
     *
     * @param bundle Started bundle
     * @param prerequisiteBundles Symbolic name set of prerequisite bundles
     */
    private void processBundleStart(final Bundle bundle, final Set<String> prerequisiteBundles) {
        final String symbolicName = bundle.getSymbolicName();

        if (prerequisiteBundles.isEmpty()) {
            bundleStarted(bundle);
            processBundleCallBack(bundle);
            bundlesListenedByListener.add(symbolicName);

            waitingBundles.remove(bundle);
            removeFromPrerequisites(symbolicName);

            LOGGER.info("Bundle startup activities done {}", symbolicName);
            startWaitingBundles();
        } else {
            LOGGER.info("Bundle waiting for prerequisite bundles startup activities to be done {}", symbolicName);
            waitingBundles.putIfAbsent(bundle, prerequisiteBundles);
        }
    }

    /**
     * Starts the waiting bundles whose prerequisites are resolved
     */
    private void startWaitingBundles() {
        for (final Map.Entry<Bundle, Set<String>> waitingEntry : waitingBundles.entrySet()) {
            final Bundle waitingBundle = waitingEntry.getKey();
            final Set<String> prerequisiteBundleSet = waitingEntry.getValue();

            if (!bundlesListenedByListener.contains(waitingBundle.getSymbolicName()) && prerequisiteBundleSet.isEmpty()) {
                processBundleStart(waitingBundle, prerequisiteBundleSet);
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
     * Removes the given symbolic name from the set of prerequisites of waiting bundles
     *
     * @param symbolicName Symbolic name of prerequisite bundle
     */
    private void removeFromPrerequisites(final String symbolicName) {
        for (final Set<String> prerequisites : waitingBundles.values()) {
            prerequisites.remove(symbolicName);
        }
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
     * Invoke the methods of prerequisite and allBundles loaded.
     *
     * @param bundle {@link Bundle}
     */
    private void processBundleCallBack(final Bundle bundle) {
        final Map<Bundle, TlcBundleActivator> bundleStoreMap = bundleContainer.getBundleStore();
        final TlcBundleActivator activator = bundleStoreMap.get(bundle);

        if (Objects.nonNull(activator)) {
            activator.preRequisiteBundlesLoaded(bundle.getBundleContext());
        }

        if (bundleStoreMap.containsKey(bundle)) {

            if (Objects.nonNull(activator)) {
                activator.allBundlesLoaded(bundle.getBundleContext());
            }
        }
    }
}
