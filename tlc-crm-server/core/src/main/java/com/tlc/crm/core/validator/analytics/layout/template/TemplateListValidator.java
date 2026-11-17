package com.tlc.crm.core.validator.analytics.layout.template;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Validates LIST request for retrieving CRM template details.
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@NoArgsConstructor (access = AccessLevel.PRIVATE)
public class TemplateListValidator implements RequestValidator<JsonObject> {

    private static class Instance {
        private static final TemplateListValidator INSTANCE = new TemplateListValidator();
    }

    public static TemplateListValidator INSTANCE() {
        return TemplateListValidator.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest(final @NonNull Long orgId,
                                final @NonNull Long userId,
                                final @NonNull JsonObject jsonObject) throws Exception {
    }
}
