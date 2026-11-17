package com.tlc.web.client.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.web.client.registrar.WebClientResourceRegistrar;
import com.tlc.web.client.registrar.model.WebClientRegistrarData;

import io.vertx.spi.cluster.hazelcast.impl.HazelcastRegistrationInfo;

import org.osgi.framework.Bundle;

/**
 * Registers the web event listeners from the started bundles
 *
 * @author Selvakumar
 * @version 1.0
 */
public class BundleListener extends com.tlc.commons.service.listener.BundleListener {

    private final WebClientResourceRegistrar webClientRegistrar;

    public BundleListener() {
        webClientRegistrar = WebClientResourceRegistrar.getInstance();
    }


    @Override
    public void bundleStarted(final Bundle bundle) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader newClassLoader = HazelcastRegistrationInfo.class.getClassLoader();
        final WebClientRegistrarData webClientRegistrarData = new WebClientRegistrarData();

        try {
            Thread.currentThread().setContextClassLoader(newClassLoader);

            webClientRegistrarData.setModuleName(bundle.getSymbolicName());
            webClientRegistrarData.setActionClasses(getClasses(bundle));
            webClientRegistrar.register(webClientRegistrarData);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Override
    public void bundleStopped(final Bundle bundle) {
        final WebClientRegistrarData webClientRegistrarData = new WebClientRegistrarData();

        webClientRegistrarData.setModuleName(bundle.getSymbolicName());
        webClientRegistrar.unregister(webClientRegistrarData);
    }
}
