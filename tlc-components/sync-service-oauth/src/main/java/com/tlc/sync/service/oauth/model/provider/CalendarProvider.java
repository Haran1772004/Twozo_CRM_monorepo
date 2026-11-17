package com.tlc.sync.service.oauth.model.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.internal.calendar.CalendarSyncService;

import java.util.Objects;

/**
 * <p>
 * Enum that represents the calendar provider type
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public enum CalendarProvider implements Provider {

    GOOGLE("google", 1),
    OUTLOOK("outlook", 2),
    ;

    private final String name;
    private final int id;

    CalendarProvider(final String name, final int id) {
        this.name = Objects.requireNonNull(name);
        this.id = id;
    }

    /**
     * <p>
     * Gets the name of the provider.
     * </p>
     *
     * @return name of the provider.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the id of the provider.
     * </p>
     *
     * @return id of the provider.
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link SyncServiceCategory}
     */
    @Override
    public SyncServiceCategory serviceCategory() {
        return SyncServiceCategory.CALENDAR;
    }

    /**
     * <p>
     * Gets the {@link CalendarProvider} by type
     * </p>
     *
     * @param id Represents the calendar provider type id
     * @return {@link CalendarProvider}
     */
    public static CalendarProvider valueOf(final int id) {
        return switch (id) {
            case 1 -> GOOGLE;
            case 2 -> OUTLOOK;
            default -> throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        };
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link CalendarSyncService}
     */
    @Override
    public CalendarSyncService getSyncService() {
        return SyncServiceProvider.getInstance().getCalendarSyncService(this);
    }

    /**
     * <p>
     * Gets the matched {@link CalendarProvider}
     * </p>
     *
     * @param providerName represents the provider name
     * @return {@link CalendarProvider}
     */
    public static CalendarProvider get(final String providerName) {
        try {
            return CalendarProvider.valueOf(providerName.toUpperCase().trim());
        } catch (Exception e) {
            throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        }
    }
}
