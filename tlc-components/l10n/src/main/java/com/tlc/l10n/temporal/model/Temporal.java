package com.tlc.l10n.temporal.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * Model representing the temporal related details.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public abstract class Temporal implements Serializable {

    private Long timeStamp;

    public Temporal(final Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Temporal(final String timeStamp, final DateTimeFormatter dateTimeFormatter) {
        this.timeStamp = LocalDateTime.parse(timeStamp, dateTimeFormatter).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(final Long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
