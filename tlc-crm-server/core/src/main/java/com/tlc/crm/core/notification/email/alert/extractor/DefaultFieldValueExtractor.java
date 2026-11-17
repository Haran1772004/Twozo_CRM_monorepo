package com.tlc.crm.core.notification.email.alert.extractor;

/**
 * <p>
 * Extracts and processes field values of type
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class DefaultFieldValueExtractor extends AbstractEmailAlertFieldValueExtractor {

    private DefaultFieldValueExtractor() {
    }

    private static class InstanceHolder {
        private static final DefaultFieldValueExtractor INSTANCE = new DefaultFieldValueExtractor();
    }

    /**
     * Gets the {@link DefaultFieldValueExtractor} instance
     *
     * @return The  {@link DefaultFieldValueExtractor}
     */
    public static DefaultFieldValueExtractor getInstance() {
        return DefaultFieldValueExtractor.InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Fetches the value for the specific email alert type
     * </p>
     *
     * @param value Represents the field value to be processed
     * @return The processed or transformed field value
     */
    @Override
    protected Object fetchValue(final Object value) {
        return value;
    }
}
