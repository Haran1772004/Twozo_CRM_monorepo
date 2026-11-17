package com.tlc.iam.server;

import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.util.ConfHolder;
import com.tlc.iam.server.initializer.IamServerInitializer;
import com.tlc.iam.server.internal.IamServerService;
import com.tlc.iam.server.internal.session.SessionCleaner;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of IAM server bundle
 *
 * @author Selvakumar
 * @version 1.0
 */
public class IamServerActivator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamServerActivator.class);
    private final IamServerInitializer iamServerInitializer = IamServerInitializer.getInstance();

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle that is being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        iamServerInitializer.preInitialize(ConfHolder.getInstance().getProperties("iam-server"));

        LOGGER.info("Iam server module activated");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle that is being stopped.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        SessionCleaner.getInstance().stop();
        IamServerService.getInstance().stop();
        LOGGER.info("Iam server module stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle that is being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
        iamServerInitializer.postInitialize(null);
    }

    /**
     * Once all bundles are loaded, init the iam server service
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
    }

}
