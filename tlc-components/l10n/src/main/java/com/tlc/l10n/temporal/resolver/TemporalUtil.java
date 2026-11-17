package com.tlc.l10n.temporal.resolver;

import com.tlc.l10n.temporal.model.Temporal;

import java.util.Objects;

/**
 * <p>
 * A tool  {@link Temporal} values.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class TemporalUtil<T extends Temporal> {

    /**
     * <p>
     * Resolves time stamp.
     * </p>
     *
     * @param timeStamp Refers timestamp.
     * @return {@link Temporal}.
     */
    public abstract T getTemporal(final Long timeStamp);

    /**
     * <p>
     * Resolves time stamp.
     * </p>
     *
     * @param temporal Refers {@link Temporal}.
     * @return {@link Temporal}.
     */
    public Long getTimeStamp(final T temporal) {
        return Objects.isNull(temporal) ? null : temporal.getTimeStamp();
    }
}
