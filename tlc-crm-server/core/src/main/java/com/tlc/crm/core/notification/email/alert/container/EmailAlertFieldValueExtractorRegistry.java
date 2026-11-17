package com.tlc.crm.core.notification.email.alert.container;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.core.notification.email.alert.extractor.EmailAlertFieldValueExtractor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Registry the mapping of {@link EmailAlertFieldType} to corresponding {@link EmailAlertFieldValueExtractor}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailAlertFieldValueExtractorRegistry {

    private Map<EmailAlertFieldType, EmailAlertFieldValueExtractor> emailAlertFieldValueExtractors;

    private EmailAlertFieldValueExtractorRegistry() {
        emailAlertFieldValueExtractors = new ConcurrentHashMap<>();
    }

    private static class InstanceHolder {
        private static final EmailAlertFieldValueExtractorRegistry INSTANCE = new EmailAlertFieldValueExtractorRegistry();
    }

    /**
     * <p>
     * Gets the {@link EmailAlertFieldValueExtractor} instance
     * </p>
     *
     * @return The  {@link EmailAlertFieldValueExtractor} instance
     */
    public static EmailAlertFieldValueExtractorRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Adds a new mapping of {@link EmailAlertFieldType} to its corresponding
     * {@link EmailAlertFieldValueExtractor} in the registry.
     * </p>
     *
     * @param emailAlertFieldType              the type of previous alert to associate
     * @param emailAlertFieldValueExtractor the handler to associate with the alert type
     */
    public void add(final EmailAlertFieldType emailAlertFieldType, final EmailAlertFieldValueExtractor emailAlertFieldValueExtractor) {
        emailAlertFieldValueExtractors.put(emailAlertFieldType, emailAlertFieldValueExtractor);
    }

    /**
     * <p>
     * Fetches the {@link EmailAlertFieldValueExtractor} associated with the specified
     * {@link EmailAlertFieldType}.
     * </p>
     *
     * @param emailAlertFieldType the type of previous alert for which the handler is needed
     * @return An {@link Optional} containing the handler if found, or an empty {@link Optional}
     * if no handler is associated with the specified alert type
     */
    public Optional<EmailAlertFieldValueExtractor> get(final EmailAlertFieldType emailAlertFieldType) {

        if (emailAlertFieldType.isDefaultFieldValueType()) {
            return Optional.ofNullable(emailAlertFieldValueExtractors.get(EmailAlertFieldType.DEFAULT_FIELD_TYPE));
        }

        return Optional.ofNullable(emailAlertFieldValueExtractors.get(emailAlertFieldType));
    }
}
