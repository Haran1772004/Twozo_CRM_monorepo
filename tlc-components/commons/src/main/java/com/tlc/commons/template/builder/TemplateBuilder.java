package com.tlc.commons.template.builder;

import com.tlc.commons.internal.template.builder.DefaultTemplateBuilder;
import com.tlc.commons.template.model.TemplateBuildRequest;
import com.tlc.commons.template.model.TemplateModel;

import java.util.Optional;

/**
 * A builder for building templates based on provided data models
 *
 * @param <T> the type of {@link TemplateBuildRequest} used to build the template
 * @author Kishore J
 * @version 1.0
 */
public interface TemplateBuilder<T extends TemplateBuildRequest<? extends TemplateModel>> {

    /**
     * <p>
     * Gets the {@link TemplateBuilder} instance
     * </p>
     *
     * @return The {@link TemplateBuilder}
     */
    static <T extends TemplateBuildRequest<? extends TemplateModel>> TemplateBuilder<T> getInstance() {
        return (TemplateBuilder<T>) DefaultTemplateBuilder.getInstance();
    }

    /**
     * <p>
     * Builds the templates for template name, {@link TemplateBuildRequest}
     * </p>
     *
     * @param templateName Represents the template name
     * @param templateData Represents the {@link TemplateBuildRequest}
     * @return The template data
     */
    Optional<String> build(final String templateName, final T templateData);
}
