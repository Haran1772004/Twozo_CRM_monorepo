package com.tlc.sync.service.oauth.internal.calendar.token;

import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.service.token.CalendarSyncTokenService;
import com.tlc.sync.service.oauth.internal.calendar.resolver.CalendarSyncTokenResolver;

/**
 * <p>
 * Provides the implementation for managing calendar access tokens.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class CalendarSyncTokenServiceImpl implements CalendarSyncTokenService {

    private final CalendarSyncTokenResolver calendarTokenResolver;

    private CalendarSyncTokenServiceImpl() {
        calendarTokenResolver = CalendarSyncTokenResolver.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncTokenServiceImpl INSTANCE = new CalendarSyncTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link CalendarSyncTokenServiceImpl} Instance
     * </p>
     *
     * @return {@link CalendarSyncTokenServiceImpl}
     */
    public static CalendarSyncTokenServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    @Override
    public String getAccessToken(final TokenReadRequest tokenReadRequest) {
        return calendarTokenResolver.getAccessToken(tokenReadRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    @Override
    public String getRefreshToken(final TokenReadRequest tokenReadRequest) {
        return calendarTokenResolver.getRefreshToken(tokenReadRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putRefreshToken(final TokenWriteRequest tokenWriteRequest) {
        calendarTokenResolver.putRefreshToken(tokenWriteRequest);
    }
}
