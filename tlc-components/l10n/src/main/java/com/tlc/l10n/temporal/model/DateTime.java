package com.tlc.l10n.temporal.model;

import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * <p>
 * Model representing the Date & Time type field with it's time stamp & {@link TimezoneInfo}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class DateTime extends Temporal {
    private TimezoneInfo timezoneInfo;

    public DateTime(final Long timeStamp) {
        super(timeStamp);
        this.timezoneInfo = TimezoneInfo.UTC;
    }

    public DateTime(final Long timeStamp, final TimezoneInfo timezoneInfo) {
        super(timeStamp);
        this.timezoneInfo = timezoneInfo;
    }

    public DateTime(final String timeStamp, final DateTimeFormatter dateTimeFormatter) {
        super(timeStamp, dateTimeFormatter);
        this.timezoneInfo = TimezoneInfo.UTC;
    }

    public TimezoneInfo getTimezoneInfo() {
        return timezoneInfo;
    }

    public void setTimezoneInfo(final TimezoneInfo timezoneInfo) {
        this.timezoneInfo = timezoneInfo;
    }

    /**
     * <p>
     * Gets the UTC time stamp.
     * </p>
     *
     * @return UTC time stamp.
     */
    public Long getUtcTimeStamp() {
        final Long timeStamp = getTimeStamp();

        if (TimezoneInfo.UTC == timezoneInfo) {
            return timeStamp;
        } else {
            return TemporalServiceProvider.get().convertToUtc(timeStamp, timezoneInfo);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param object Represents the object to be compared.
     * @return if the given objects are equals.
     */
    @Override
    public boolean equals(final Object object) {

        if (this == object) {
            return true;
        }

        if (object instanceof final DateTime dateTime) {
            return Objects.equals(this.timezoneInfo, dateTime.timezoneInfo) &&
                    Objects.equals(this.getTimeStamp(), dateTime.getTimeStamp());
        }
        return false;
    }
}
