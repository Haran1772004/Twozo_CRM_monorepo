package com.tlc.crm.common;

import com.tlc.crm.common.registrar.CommonResourceRegistrar;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import org.osgi.framework.Bundle;

/**
 * Listens for changes in each bundle and initialize the common services of client bundle.
 * While a client bundle stopped, all the associated services registered by the client bundle will be deregistered
 *
 * @author Selvakumar
 * @version 1.0
 */
class BundleListener extends com.tlc.commons.service.listener.BundleListener {

    private final CommonResourceRegistrar commonRegistrar;

    BundleListener() {
        commonRegistrar = CommonResourceRegistrar.getInstance();
    }

    /**
     * <p>
     * Registers the required actions which are present in the bundle
     * </p>
     *
     * @param bundle started bundle
     */
    @Override
    public void bundleStarted(final Bundle bundle) {
        final CrmServerRegistrarData registrarData = new CrmServerRegistrarData();

        registrarData.setModuleName(bundle.getSymbolicName());
        registrarData.setClasses(getClasses(bundle));
        commonRegistrar.register(registrarData);
    }

    /**
     * <p>
     * Clears the actions which have been registered by the bundle
     * </p>
     *
     * @param bundle stopped bundle
     */
    @Override
    public void bundleStopped(final Bundle bundle) {
        final CrmServerRegistrarData registrarData = new CrmServerRegistrarData();

        registrarData.setModuleName(bundle.getSymbolicName());
        commonRegistrar.unregister(registrarData);
    }
}
