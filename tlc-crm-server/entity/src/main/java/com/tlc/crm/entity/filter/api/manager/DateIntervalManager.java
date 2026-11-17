package com.tlc.crm.entity.filter.api.manager;

import com.tlc.crm.entity.filter.api.manager.impl.DateIntervalManagerImpl;
import com.tlc.crm.entity.filter.api.models.DateInterval;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link DateInterval} related fetch actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface DateIntervalManager {

    static DateIntervalManager getInstance() {
        return DateIntervalManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the date intervals available in DB.
     * </p>
     *
     * @return Collection of {@link DateInterval}.
     */
    Collection<DateInterval> getAllDateIntervals();

    /**
     * <p>
     * Gets the date interval given by id.
     * </p>
     *
     * @param dateIntervalId Represents the date interval id
     * @return The {@link DateInterval}
     */
    DateInterval getDateInterval(final Long dateIntervalId);
}