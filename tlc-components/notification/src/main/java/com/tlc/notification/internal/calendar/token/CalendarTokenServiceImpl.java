package com.tlc.notification.internal.calendar.token;

import com.tlc.commons.util.StringUtil;
import com.tlc.notification.internal.calendar.resolver.CalendarTokenResolver;
import com.tlc.notification.service.calendar.CalendarTokenService;

import java.util.Optional;

/**
 * <p>
 * Service interface responsible for managing the caching of sync tokens for calendar data.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class CalendarTokenServiceImpl implements CalendarTokenService {

    private final CalendarTokenResolver calendarTokenResolver;

    private CalendarTokenServiceImpl() {
        this.calendarTokenResolver = CalendarTokenResolver.getInstance();
    }

    private static class Instance {
        private static final CalendarTokenServiceImpl INSTANCE = new CalendarTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link CalendarTokenServiceImpl} instance
     * </p>
     *
     * @return {@link CalendarTokenServiceImpl} instance
     */
    public static CalendarTokenServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllSyncTokens() {
        calendarTokenResolver.clearAllSyncTokens();
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @return Sync token stored for every sync
     */
    @Override
    public String getSyncToken(final String key) {
        return calendarTokenResolver.getSyncToken(key);
    }

    /**
     * {@inheritDoc}
     *
     * @param key   Represents the key
     * @param token Represents the sync token
     */
    @Override
    public void putSyncToken(final String key, final String token) {
        calendarTokenResolver.putSyncToken(key, token);
    }

    /**
     * <p>
     * Retrieves the delta sync token for a specific organization and email address.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress The email address associated with the calendar
     * @return {@link Optional} containing the delta token if found, otherwise empty
     */
    public Optional<String> getSyncToken(final long orgId, final String emailAddress) {
        return getKey(orgId, emailAddress).map(calendarTokenResolver::getSyncToken);
    }

    /**
     * <p>
     * Stores the delta sync token for the given organization and email address.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress The email address associated with the calendar
     * @param token        The delta sync token to be stored
     */
    public void putSyncToken(final long orgId, final String emailAddress, final String token) {
        getKey(orgId, emailAddress).ifPresent(key -> calendarTokenResolver.putSyncToken(key, token));
    }

    /**
     * <p>
     * Constructs a unique key using the organization ID and email address.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress The email address associated with the calendar
     * @return {@link Optional} containing the constructed key if both values are valid
     */
    private Optional<String> getKey(final long orgId, final String emailAddress) {
        return StringUtil.joinNonNullValues(StringUtil.Delimiter.UNDER_SCORE, orgId, emailAddress);
    }
}
