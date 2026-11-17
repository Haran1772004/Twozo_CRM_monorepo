package com.tlc.notification.service.calendar;

import com.tlc.notification.internal.calendar.token.CalendarTokenServiceImpl;
import com.tlc.notification.service.TokenService;

import java.util.Optional;

/**
 * <p>
 * Service interface responsible for managing the caching of sync tokens for calendar data.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface CalendarTokenService extends TokenService {

    /**
     * <p>
     * Gets the instance for {@link CalendarTokenService}.
     * </p>
     *
     * @return {@link CalendarTokenService}
     */
    static CalendarTokenService getInstance() {
        return CalendarTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Stores the delta sync token for a specific organization and email address.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress The email address associated with the calendar
     * @param token        The delta token to be stored
     */
    void putSyncToken(final long orgId, final String emailAddress, final String token);

    /**
     * <p>
     * Retrieves the stored delta sync token for the given organization and email address.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress The email address associated with the calendar
     * @return an {@link Optional} containing the delta token if present, otherwise empty
     */
    Optional<String> getSyncToken(final long orgId, final String emailAddress);
}