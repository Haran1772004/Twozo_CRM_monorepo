package com.tlc.notification.internal.calendar.resolver;

import com.tlc.notification.internal.resolver.TokenResolver;

/**
 * <p>
 * Provides the services to resolve the tokens used for calendar synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface CalendarTokenResolver extends TokenResolver {

    /**
     * <p>
     * Gets the instance for {@link CalendarTokenResolver}.
     * </p>
     *
     * @return {@link CalendarTokenResolver}
     */
    static CalendarTokenResolver getInstance() {
        return CalendarTokenResolverImpl.getInstance();
    }
}
