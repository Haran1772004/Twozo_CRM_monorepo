package com.tlc.l10n.temporal.model;

import java.util.Objects;

/**
 * <p>
 * Model representing the Time type field with it's time stamp
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class Time extends Temporal {

    public Time(final Long timeStamp) {
        super(timeStamp);
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

        if (object instanceof final Time time) {
            return Objects.equals(this.getTimeStamp(), time.getTimeStamp());
        }
        return false;
    }
}