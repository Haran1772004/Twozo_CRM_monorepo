package com.tlc.l10n.temporal.resolver;

import com.tlc.l10n.temporal.model.DateTime;

import java.util.Objects;

/**
 * <p>
 * Acts as an agent to resolve {@link DateTime} values.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class DateTimeUtil extends TemporalUtil<DateTime> {

    private DateTimeUtil() {

    }

    private static class Instance {
        private static final DateTimeUtil INSTANCE = new DateTimeUtil();
    }

    /**
     * <p>
     * Gets the {@link DateTimeUtil} instance.
     * </p>
     *
     * @return {@link DateTimeUtil}.
     */
    public static DateTimeUtil getInstance() {
        return DateTimeUtil.Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves time stamp.
     * </p>
     *
     * @param timeStamp Refers timestamp.
     * @return {@link DateTime}.
     */
    @Override
    public DateTime getTemporal(final Long timeStamp) {
        return Objects.isNull(timeStamp) ? null : new DateTime(timeStamp);
    }
}
