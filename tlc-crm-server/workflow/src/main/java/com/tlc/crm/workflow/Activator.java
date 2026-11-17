package com.tlc.crm.workflow;

import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.util.ConfHolder;

import com.tlc.crm.workflow.initializer.WorkflowServiceInitializer;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of workflow bundle.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private WorkflowServiceInitializer workFlowServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        workFlowServiceInitializer = WorkflowServiceInitializer.getInstance();

        workFlowServiceInitializer.preInitialize(ConfHolder.getInstance().getProperties("workflow-executor"));
        LOGGER.info("Workflow module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        workFlowServiceInitializer.stop();
        LOGGER.info("Workflow module stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Prerequisite startup activities done for {}", bundleContext.getBundle().getSymbolicName());
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Processes are done after all bundles are loaded for Bundle: {}", bundleContext.getBundle().getSymbolicName());
        workFlowServiceInitializer.postInitialize(null);
    }
}
