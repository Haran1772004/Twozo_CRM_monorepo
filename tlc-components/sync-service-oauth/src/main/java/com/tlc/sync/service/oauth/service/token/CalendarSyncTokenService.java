package com.tlc.sync.service.oauth.service.token;

import com.tlc.sync.service.oauth.internal.AccessTokenService;
import com.tlc.sync.service.oauth.internal.RefreshTokenService;
import com.tlc.sync.service.oauth.internal.calendar.token.CalendarSyncTokenServiceImpl;

/**
 * <p>
 * Provides the service for managing calendar access tokens.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public interface CalendarSyncTokenService extends AccessTokenService, RefreshTokenService {

    /**
     * <p>
     * Gets the {@link CalendarSyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link CalendarSyncTokenServiceImpl}
     */
    static CalendarSyncTokenService getInstance() {
        return CalendarSyncTokenServiceImpl.getInstance();
    }
}