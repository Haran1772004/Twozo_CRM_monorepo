package com.tlc.email.common.model.config;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents a time interval for email processing.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public record EmailInterval(long startTime, long endTime) implements Comparable<EmailInterval> {

    public EmailInterval {

        if (startTime >= endTime) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_TIME_RANGE);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param interval the object to be compared.
     * @return the compared value
     */
    @Override
    public int compareTo(final EmailInterval interval) {
        return Long.compare(this.startTime, interval.startTime);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param obj Represents the object
     * @return true, if the current object and the given object is same;otherwise false
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (Objects.isNull(obj) || getClass() != obj.getClass()) return false;

        final EmailInterval interval = (EmailInterval) obj;

        return Objects.equals(startTime, interval.startTime) && Objects.equals(endTime, interval.endTime);
    }
}

