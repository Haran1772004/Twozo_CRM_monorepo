package com.tlc.crm.core.notification.email.alert.extractor;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.core.notification.email.alert.container.EmailAlertFieldValueExtractorRegistry;
import com.tlc.crm.core.notification.email.alert.model.EmailAlertFieldValueExtractionData;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * A {@link AbstractEmailAlertFieldValueExtractor} class implementing email alert field value extractors
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AbstractEmailAlertFieldValueExtractor implements EmailAlertFieldValueExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEmailAlertFieldValueExtractor.class);

    private final FieldPropertiesService fieldPropertiesService;
    private final PropertyDescriptorService propertyDescriptorService;
    private final EmailAlertFieldValueExtractorRegistry emailAlertFieldValueExtractorRegistry;

    protected AbstractEmailAlertFieldValueExtractor() {
        fieldPropertiesService = FieldPropertiesService.getInstance();
        propertyDescriptorService = PropertyDescriptorService.getInstance();
        emailAlertFieldValueExtractorRegistry = EmailAlertFieldValueExtractorRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAlertFieldValueExtractionData Represents the {@link EmailAlertFieldValueExtractionData}
     */
    public void extractTemplateFieldValues(final EmailAlertFieldValueExtractionData emailAlertFieldValueExtractionData) {
        final Map<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplate = emailAlertFieldValueExtractionData.fieldValuesByTemplate();
        final Collection<EmailNotificationTemplate> templates = emailAlertFieldValueExtractionData.templates();
        final TlcModel model = emailAlertFieldValueExtractionData.model();

        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(model.getClass(), EmailAlertField.class)) {
            final EmailAlertField emailAlertField = field.getAnnotation(EmailAlertField.class);
            final EmailAlertFieldType emailAlertFieldType = emailAlertField.fieldType();

            emailAlertFieldValueExtractorRegistry.get(emailAlertFieldType).ifPresent(emailAlertFieldValueExtractor -> {
                emailAlertFieldValueExtractor.populateFieldValues(new EmailAlertFieldValueExtractionData(field,
                        emailAlertField, model, fieldValuesByTemplate, templates));
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAlertFieldValueExtractionData Represents the {@link EmailAlertFieldValueExtractionData}
     */
    public void populateFieldValues(final EmailAlertFieldValueExtractionData emailAlertFieldValueExtractionData) {
        final EmailAlertField emailAlertField = emailAlertFieldValueExtractionData.emailAlertField();
        final Collection<EmailNotificationTemplate> templates = emailAlertFieldValueExtractionData.templates();
        final Field field = emailAlertFieldValueExtractionData.field();
        final TlcModel tlcModel = emailAlertFieldValueExtractionData.model();
        final Map<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplate
                = emailAlertFieldValueExtractionData.fieldValuesByTemplate();

        getValueFromField(field, tlcModel).ifPresent(value -> {
            final String fieldName = emailAlertField.name();

            if (emailAlertField.requirePreviousValue()) {
                final EmailNotificationTemplate[] emailNotificationTemplates = emailAlertField.requirePreviousValueFor().length == 0
                        ? emailAlertField.placeholderTemplates()
                        : emailAlertField.requirePreviousValueFor();

                for (final EmailNotificationTemplate template : emailNotificationTemplates) {

                    if (templates.contains(template)) {
                        fieldValuesByTemplate.computeIfAbsent(template, templateFieldValues -> new HashMap<>()).put(fieldName, value);
                    }
                }
            }
        });
    }

    /**
     * <p>
     * Fetches the value of the specified field from the provided {@link TlcModel}
     * </p>
     *
     * @param field    The field whose value needs to be retrieved
     * @param tlcModel Represents the {@link TlcModel}
     * @return An Optional containing the value of the field if found, or an empty Optional if not found or an error occurred
     */
    protected <T> Optional<T> getValueFromField(final Field field, final TlcModel tlcModel) {
        final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(field.getName(), tlcModel.getClass());

        try {
            return (Optional<T>) Optional.ofNullable(fetchValue(propertyDescriptor.getReadMethod().invoke(tlcModel)));
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            LOGGER.debug("Error while getting value from field {}", field.getName(), exception);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Fetches the value for the specific email alert type
     * </p>
     *
     * @param value Represents the field value to be processed
     * @return The processed or transformed field value
     */
    protected abstract Object fetchValue(final Object value);
}
