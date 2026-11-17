package com.tlc.crm.core.notification.email.alert.extractor;

import com.tlc.l10n.temporal.model.DateTime;

/**
 * <p>
 * Extracts and processes field values of type date time
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class DateTimeFieldValueExtractor extends AbstractEmailAlertFieldValueExtractor {

    private DateTimeFieldValueExtractor() {
    }

    private static class InstanceHolder {
        private static final DateTimeFieldValueExtractor INSTANCE = new DateTimeFieldValueExtractor();
    }

    /**
     * Gets the {@link DateTimeFieldValueExtractor} instance
     *
     * @return The  {@link DateTimeFieldValueExtractor}
     */
    public static DateTimeFieldValueExtractor getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param value Represents the field value to be processed
     * @return The processed or transformed field value
     */
    @Override
    public Object fetchValue(final Object value) {
        final DateTime dateTime = (DateTime) value;

        return dateTime.getTimeStamp();
    }
}
