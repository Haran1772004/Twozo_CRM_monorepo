package com.tlc.l10n.temporal.model;

import java.util.Objects;

/**
 * <p>
 * Model representing the Date type field with it's time stamp
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class Date extends Temporal {

    public Date(final Long timeStamp) {
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

        if (object instanceof final Date date) {
            return Objects.equals(this.getTimeStamp(), date.getTimeStamp());
        }
        return false;
    }
}
