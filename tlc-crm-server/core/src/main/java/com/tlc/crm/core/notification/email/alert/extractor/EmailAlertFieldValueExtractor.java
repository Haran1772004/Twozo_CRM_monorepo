package com.tlc.crm.core.notification.email.alert.extractor;

import com.tlc.crm.core.notification.email.alert.model.EmailAlertFieldValueExtractionData;

/**
 * <p>
 * Extractor for extracting field values used in email alerts
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailAlertFieldValueExtractor {

    /**
     * <p>
     * Extracts the field values for given {@link EmailAlertFieldValueExtractionData}
     * </p>
     *
     * @param emailAlertFieldValueExtractionData Represents the {@link EmailAlertFieldValueExtractionData}
     */
    void extractTemplateFieldValues(final EmailAlertFieldValueExtractionData emailAlertFieldValueExtractionData);

    /**
     * <p>
     * Populates the field values for given {@link EmailAlertFieldValueExtractionData}
     * </p>
     *
     * @param emailAlertFieldValueExtractionData Represents the {@link EmailAlertFieldValueExtractionData}
     */
    void populateFieldValues(final EmailAlertFieldValueExtractionData emailAlertFieldValueExtractionData);
}
