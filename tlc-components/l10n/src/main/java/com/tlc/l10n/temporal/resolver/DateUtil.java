package com.tlc.l10n.temporal.resolver;

import com.tlc.l10n.temporal.model.Date;

import java.util.Objects;

/**
 * <p>
 * Acts as an agent to resolve {@link Date} values.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class DateUtil extends TemporalUtil<Date> {
    
    private DateUtil() {
        
    }

    private static class Instance {
        private static final DateUtil INSTANCE = new DateUtil();
    }

    /**
     * <p>
     * Gets the {@link DateUtil} instance.
     * </p>
     *
     * @return {@link DateUtil}.
     */
    public static DateUtil getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves time stamp.
     * </p>
     *
     * @param timeStamp Refers timestamp.
     * @return {@link Date}.
     */
    @Override
    public Date getTemporal(final Long timeStamp) {
        return Objects.isNull(timeStamp) ? null : new Date(timeStamp);
    }
}
