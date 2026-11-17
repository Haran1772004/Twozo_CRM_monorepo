package com.tlc.sync.service.oauth.service.provider;

import com.tlc.sync.service.oauth.internal.provider.calendar.CalendarProviderService;
import com.tlc.sync.service.oauth.internal.provider.contact.ContactProviderService;
import com.tlc.sync.service.oauth.internal.provider.email.EmailProviderService;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.provider.Provider;

/**
 * <p>
 * Provides the provider for given mail id by using MX record
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public interface ProviderService {

    /**
     * <p>
     * Gets the email provider service
     * </p>
     *
     * @return email provider service.
     */
    static ProviderService getEmailProviderService() {
        return EmailProviderService.getInstance();
    }

    /**
     * <p>
     * Gets the calendar provider service
     * </p>
     *
     * @return calendar provider service.
     */
    static CalendarProviderService getCalendarProviderService() {
        return CalendarProviderService.getInstance();
    }

    /**
     * <p>
     * Gets the contact provider service
     * </p>
     *
     * @return contact provider service.
     */
    static ContactProviderService getContactProviderService() {
        return ContactProviderService.getInstance();
    }

    /**
     * <p>
     * Finds the provider for the given email address.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the {@link Provider}
     */
    <T extends Provider> T findProvider(final EmailAddress emailAddress);
}
