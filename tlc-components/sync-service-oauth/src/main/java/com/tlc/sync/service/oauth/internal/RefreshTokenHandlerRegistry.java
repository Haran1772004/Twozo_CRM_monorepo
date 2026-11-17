package com.tlc.sync.service.oauth.internal;

import com.tlc.sync.service.oauth.internal.handler.TokenRefreshHandler;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry which stores the refresh token handlers
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class RefreshTokenHandlerRegistry {

    private final Map<SyncServiceCategory, TokenRefreshHandler> refreshHandlerMap;

    private RefreshTokenHandlerRegistry() {
        refreshHandlerMap = new HashMap<>();
    }

    private static final class InstanceHolder {
        private static final RefreshTokenHandlerRegistry REFRESH_TOKEN_HANDLER_REGISTRY = new RefreshTokenHandlerRegistry();
    }

    /**
     * <p>
     * Gets the instance of {@link RefreshTokenHandlerRegistry}
     * </p>
     *
     * @return the instance of {@link RefreshTokenHandlerRegistry}
     */
    public static RefreshTokenHandlerRegistry getInstance() {
        return InstanceHolder.REFRESH_TOKEN_HANDLER_REGISTRY;
    }

    /**
     * <p>
     * Puts the {@link TokenRefreshHandler} into the registry
     * </p>
     *
     * @param syncServiceCategory Represents the {@link SyncServiceCategory}
     * @param tokenRefreshHandler      Represents the {@link TokenRefreshHandler}
     */
    public void put(final SyncServiceCategory syncServiceCategory, final TokenRefreshHandler tokenRefreshHandler) {
        refreshHandlerMap.put(syncServiceCategory, tokenRefreshHandler);
    }

    /**
     * <p>
     * Gets the {@link TokenRefreshHandler}
     * </p>
     *
     * @param syncServiceCategory Represents the {@link SyncServiceCategory}
     * @return the {@link TokenRefreshHandler}
     */
    public TokenRefreshHandler get(final SyncServiceCategory syncServiceCategory) {
        return refreshHandlerMap.get(syncServiceCategory);
    }
}