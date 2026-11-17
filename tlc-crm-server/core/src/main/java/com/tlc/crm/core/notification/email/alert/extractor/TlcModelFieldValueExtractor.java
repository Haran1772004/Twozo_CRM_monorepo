package com.tlc.crm.core.notification.email.alert.extractor;

import com.tlc.crm.core.notification.email.alert.model.EmailAlertFieldValueExtractionData;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;

/**
 * <p>
 * Extracts and processes field values of type {@link TlcModel}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class TlcModelFieldValueExtractor extends AbstractEmailAlertFieldValueExtractor {

    private TlcModelFieldValueExtractor() {
    }

    private static class InstanceHolder {
        private static final TlcModelFieldValueExtractor INSTANCE = new TlcModelFieldValueExtractor();
    }

    /**
     * Gets the {@link TlcModelFieldValueExtractor} instance
     *
     * @return The  {@link TlcModelFieldValueExtractor}
     */
    public static TlcModelFieldValueExtractor getInstance() {
        return TlcModelFieldValueExtractor.InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param value Represents the field value to be processed
     * @return The processed or transformed field value
     */
    @Override
    public Object fetchValue(final Object value) {
        return value;
    }

    public void populateFieldValues(final EmailAlertFieldValueExtractionData emailAlertFieldValueExtractionData) {
        final Field field =emailAlertFieldValueExtractionData.field();

        getValueFromField(field, emailAlertFieldValueExtractionData.model())
                .ifPresent(value -> extractTemplateFieldValues(new EmailAlertFieldValueExtractionData(field,
                        emailAlertFieldValueExtractionData.emailAlertField(), (TlcModel) value,
                        emailAlertFieldValueExtractionData.fieldValuesByTemplate(), emailAlertFieldValueExtractionData.templates())));
    }
}
