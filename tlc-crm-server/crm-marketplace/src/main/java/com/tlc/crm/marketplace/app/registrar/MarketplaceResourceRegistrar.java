package com.tlc.crm.marketplace.app.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.marketplace.app.handler.MarketplaceAppExecutionHandler;
import com.tlc.crm.marketplace.app.handler.MarketplaceAppHandler;
import com.tlc.crm.marketplace.app.internal.status.CrmMarketplaceErrorCodes;
import com.tlc.crm.marketplace.app.registry.MarketplaceAppHandlerRegistry;

import java.lang.reflect.Constructor;

/**
 * <p>
 * The registrar that registers the marketplace app related actions.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class MarketplaceResourceRegistrar implements ResourceRegistrar<CrmServerRegistrarData>  {

    private final MarketplaceAppHandlerRegistry marketplaceAppHandlerRegistry;

    private MarketplaceResourceRegistrar() {
        this.marketplaceAppHandlerRegistry = MarketplaceAppHandlerRegistry.getInstance();
    }

    private static final class Instance {
        private static final MarketplaceResourceRegistrar MARKETPLACE_APP_RESOURCE_REGISTRAR = new MarketplaceResourceRegistrar();
    }

    public static MarketplaceResourceRegistrar getInstance() {
        return Instance.MARKETPLACE_APP_RESOURCE_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final CrmServerRegistrarData registrarData) {

        for (final Class<?> classObj : registrarData.getClasses()) {
            final String className = classObj.getName();

            if (className.contains("marketplace.handler")) {

                try {
                    final MarketplaceAppHandler handler = classObj.getAnnotation(MarketplaceAppHandler.class);
                    final Constructor<MarketplaceAppExecutionHandler> constructor = (Constructor<MarketplaceAppExecutionHandler>) classObj.getConstructor();

                    marketplaceAppHandlerRegistry.add(handler.app(), constructor.newInstance());
                } catch (Exception exception) {
                    throw ErrorCode.get(CrmMarketplaceErrorCodes.FAILED_TO_REGISTER_MARKETPLACE_HANDLERS, exception);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final CrmServerRegistrarData registrarData) {
        marketplaceAppHandlerRegistry.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        marketplaceAppHandlerRegistry.clear();
    }
}
