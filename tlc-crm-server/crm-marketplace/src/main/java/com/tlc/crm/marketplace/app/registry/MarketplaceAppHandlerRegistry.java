package com.tlc.crm.marketplace.app.registry;

import com.tlc.crm.marketplace.app.handler.MarketplaceAppExecutionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * The store contains the marketplace app execution handlers, each paired with its corresponding type
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class MarketplaceAppHandlerRegistry {

    private final Map<String, MarketplaceAppExecutionHandler> marketplaceHandlerMap;

    private MarketplaceAppHandlerRegistry() {
        this.marketplaceHandlerMap = new HashMap<>();
    }

    private static class Instance {
        private static final MarketplaceAppHandlerRegistry MARKETPLACE_APP_HANDLER_REGISTRY = new MarketplaceAppHandlerRegistry();
    }

    /**
     * <p>
     * Gets the instance {@link MarketplaceAppHandlerRegistry}
     * </p>
     *
     * @return The instance {@link MarketplaceAppHandlerRegistry}
     */
    public static MarketplaceAppHandlerRegistry getInstance() {
        return Instance.MARKETPLACE_APP_HANDLER_REGISTRY;
    }

    /**
     * <p>
     * Loads the marketplace handler name and {@link MarketplaceAppExecutionHandler}
     * </p>
     *
     * @param marketplaceAppHandlerName      Represents the marketplace handler name
     * @param marketplaceAppExecutionHandler Represents the {@link MarketplaceAppExecutionHandler}
     */
    public void add(final String marketplaceAppHandlerName, final MarketplaceAppExecutionHandler marketplaceAppExecutionHandler) {
        marketplaceHandlerMap.put(marketplaceAppHandlerName, marketplaceAppExecutionHandler);
    }

    /**
     * <p>
     * Gets the {@link MarketplaceAppExecutionHandler} with the marketplace handler name
     * </p>
     *
     * @param marketplaceAppHandler Represents the marketplace handler name
     * @return The {@link MarketplaceAppExecutionHandler}
     */
    public MarketplaceAppExecutionHandler get(final String marketplaceAppHandler) {
        return marketplaceHandlerMap.get(marketplaceAppHandler);
    }

    /**
     * <p>
     * Clears the marketplace handler map
     * </p>
     */
    public void clear() {
        marketplaceHandlerMap.clear();
    }
}