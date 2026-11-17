package com.tlc.sync.service.oauth.internal.provider.calendar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.provider.AbstractProviderService;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.provider.CalendarProvider;
import com.tlc.sync.service.oauth.model.provider.Provider;

/**
 * <p>
 * Provides the calendar provider for given mail id by using MX record
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class CalendarProviderService extends AbstractProviderService {

    private static class Instance {
        private static final CalendarProviderService INSTANCE = new CalendarProviderService();
    }

    /**
     * <p>
     * Gets the {@link CalendarProviderService} Instance
     * </p>
     *
     * @return {@link CalendarProviderService}
     */
    public static CalendarProviderService getInstance() {
        return CalendarProviderService.Instance.INSTANCE;
    }

    private CalendarProviderService() {
    }

    /**
     * <p>
     * Gets provider for the given MX value
     * </p>
     *
     * @param providerMxValue Represents the mx value.
     * @return {@link CalendarProvider}
     */
    @Override
    protected Provider getProvider(final String providerMxValue) {

        if (providerMxValue.contains(SyncServiceFields.GMAIL)) {
            return CalendarProvider.GOOGLE;
        } else if (providerMxValue.contains(SyncServiceFields.OUTLOOK)) {
            return CalendarProvider.OUTLOOK;
        } else {
            throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        }
    }
}
