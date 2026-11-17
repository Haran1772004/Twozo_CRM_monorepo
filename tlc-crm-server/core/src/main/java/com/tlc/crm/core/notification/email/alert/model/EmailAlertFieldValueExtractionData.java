package com.tlc.crm.core.notification.email.alert.model;

import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Holder that holds the email alert field values
 * </p>
 *
 * @param field                 Represents the  {@link Field}
 * @param emailAlertField       Represents the {@link EmailAlertField}
 * @param model                 Represents the {@link TlcModel}
 * @param fieldValuesByTemplate A {@link Map} containing extracted field values by each {@link EmailNotificationTemplate}
 * @param templates             Which contains the collection of {@link EmailNotificationTemplate} to filter
 * @author Kishore J
 * @version 1.0
 */
public record EmailAlertFieldValueExtractionData(Field field, EmailAlertField emailAlertField, TlcModel model,
                                                 Map<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplate,
                                                 Collection<EmailNotificationTemplate> templates) {

    public EmailAlertFieldValueExtractionData(final TlcModel model,
                                              final Map<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplate,
                                              final Collection<EmailNotificationTemplate> templates) {
        this(null, null, model, fieldValuesByTemplate, templates);
    }
}
