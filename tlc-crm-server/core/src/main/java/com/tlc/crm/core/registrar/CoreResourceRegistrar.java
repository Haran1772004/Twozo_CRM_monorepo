package com.tlc.crm.core.registrar;

import com.tlc.commons.service.registrar.ResourceRegistrar;

import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.core.action.service.CoreActionContainer;

/**
 * <p>
 * The registrar that registers the given actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class CoreResourceRegistrar implements ResourceRegistrar<CrmServerRegistrarData> {

    private final CoreActionContainer coreActionContainer;

    private CoreResourceRegistrar() {
        coreActionContainer = CoreActionContainer.getInstance();
    }

    private static final class Instance {
        private static final CoreResourceRegistrar CORE_REGISTRAR = new CoreResourceRegistrar();
    }

    public static CoreResourceRegistrar getInstance() {
        return Instance.CORE_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final CrmServerRegistrarData registrarData) {

    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final CrmServerRegistrarData registrarData) {
        coreActionContainer.remove(registrarData.getModuleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        coreActionContainer.clearAll();
    }
}
