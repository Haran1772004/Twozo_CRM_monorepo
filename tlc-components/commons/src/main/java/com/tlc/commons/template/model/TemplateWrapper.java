package com.tlc.commons.template.model;

/**
 * Defines the contract for template processing and manipulation.
 *
 * <p>A {@code TemplateWrapper} provides operations to access the raw template
 * content, replace dynamic values using a {@link TemplateBuildRequest}, and
 * create an updated template instance with modified HTML content.
 *
 * <p>Implementations can provide template engine-specific handling while
 * exposing a common interface for template operations.
 *
 * @author Sathish
 * @version 1.0
 */
public interface TemplateWrapper {

    /**
     * Replaces the placeholders in the template using the provided template data.
     *
     * <p>The implementation resolves the template variables using the supplied
     * {@link TemplateBuildRequest} and returns the processed template content.
     *
     * @param templateBuildRequest Holder containing the data required for
     *                           replacing template values.
     * @param <T> Type of the template model used for data replacement.
     * @return Processed template content after replacing all available values.
     */
    <T extends TemplateModel> String replaceValues(final TemplateBuildRequest<T> templateBuildRequest);


    /**
     * Retrieves the raw HTML content of the template.
     *
     * @return HTML representation of the template before value replacement.
     */
    String getHtmlString();


    /**
     * Creates an updated template wrapper using the provided HTML content.
     *
     * <p>The implementation may create a new template instance with the updated
     * content instead of modifying the existing template instance.
     *
     * @param html Updated HTML content to be applied to the template.
     * @return Template wrapper containing the updated HTML content.
     */
    TemplateWrapper updateHtml(final String html);
}