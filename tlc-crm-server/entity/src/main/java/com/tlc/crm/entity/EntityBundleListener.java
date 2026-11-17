package com.tlc.crm.entity;

import com.tlc.commons.service.listener.BundleListener;

import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.entity.registrar.EntityResourceRegistrar;

import org.osgi.framework.Bundle;

/**
 * @author Selvakumar
 * @version 1.0
 */
class EntityBundleListener extends BundleListener {

    private final EntityResourceRegistrar entityRegistrar;

    EntityBundleListener() {
        entityRegistrar = EntityResourceRegistrar.getInstance();
    }

    /**
     * <p>
     * Registers the dropdown actions which are present in the bundle
     * </p>
     *
     * @param bundle started bundle
     */
    @Override
    public void bundleStarted(final Bundle bundle) {
        final CrmServerRegistrarData registrarData = new CrmServerRegistrarData();

        registrarData.setModuleName(bundle.getSymbolicName());
        registrarData.setClasses(getClasses(bundle));
        entityRegistrar.register(registrarData);
    }

    /**
     * <p>
     * Clears the dropdown actions which have been registered by the bundle
     * </p>
     *
     * @param bundle stopped bundle
     */
    @Override
    public void bundleStopped(final Bundle bundle) {
        final CrmServerRegistrarData registrarData = new CrmServerRegistrarData();

        registrarData.setModuleName(bundle.getSymbolicName());
        entityRegistrar.unregister(registrarData);
    }
}
