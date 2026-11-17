package com.tlc.sync.service.oauth.internal.calendar.resolver;

import com.tlc.sync.service.oauth.internal.AccessTokenResolver;
import com.tlc.sync.service.oauth.internal.RefreshTokenResolver;

/**
 * <p>
 * Provides the services to resolve the tokens used for calendar synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface CalendarSyncTokenResolver extends AccessTokenResolver, RefreshTokenResolver {

    /**
     * <p>
     * Gets the instance for {@link CalendarSyncTokenResolver}.
     * </p>
     *
     * @return {@link CalendarSyncTokenResolver}
     */
    static CalendarSyncTokenResolver getInstance() {
        return CalendarSyncTokenResolverImpl.getInstance();
    }
}
