package com.tlc.commons.internal.template.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.internal.template.service.VelocityRuntimeTemplateService;
import com.tlc.commons.template.model.TemplateBuildRequest;
import com.tlc.commons.template.model.TemplateModel;
import com.tlc.commons.template.model.TemplateWrapper;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>
 * A wrapper for Apache Velocity Template to facilitate template formatting with provided models
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class VelocityTemplateWrapper implements TemplateWrapper {

    public static final String TEMPLATE_KEY = "template";
    private static final VelocityRuntimeTemplateService VELOCITY_RUNTIME_TEMPLATE_SERVICE = VelocityRuntimeTemplateService.getInstance();

    private final Template template;

    public VelocityTemplateWrapper(final Template template) {
        this.template = template;
    }

    /**
     * <p>
     * Formats the template using the provided models
     * </p>
     *
     * @param templateBuildRequest Represents the {@link TemplateBuildRequest}
     * @return The template content
     */
    @Override
    public <T extends TemplateModel> String replaceValues(final TemplateBuildRequest<T> templateBuildRequest) {
        final StringWriter writer = new StringWriter();

        template.merge(buildVelocityContext(templateBuildRequest), writer);
        return writer.toString();
    }

    /**
     * Retrieves the HTML content of the current Velocity template as a string.
     *
     * <p>The template resource is loaded using the configured resource loader
     * and converted into a UTF-8 encoded string representation.
     *
     * @return HTML content of the template.
     */
    @Override
    public String getHtmlString() {
        final String templateName = template.getName();

        try (final InputStream inputStream = template.getResourceLoader().getResourceStream(templateName)) {

            if (Objects.isNull(inputStream)) {
                throw ErrorCode.get(ErrorCodes.READ_ACTION_FAILED, "Template not found: " + templateName);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw ErrorCode.get(ErrorCodes.READ_ACTION_FAILED, "Failed to read template content for: " + templateName, exception);
        }
    }

    /**
     * Creates a new template instance by replacing the existing template content
     * with the provided HTML string.
     *
     * <p>The updated template is temporarily registered with the Velocity runtime,
     * loaded as a new template wrapper, and removed from the runtime repository
     * after retrieval to avoid retaining unnecessary template resources.
     *
     * @param html New HTML content to be applied to the template.
     * @return Updated {@link TemplateWrapper} containing the provided HTML content.
     */
    @Override
    public TemplateWrapper updateHtml(final String html) {
        final String templateName = template.getName();

        try {
            VELOCITY_RUNTIME_TEMPLATE_SERVICE.addTemplate(templateName, html);

            return VELOCITY_RUNTIME_TEMPLATE_SERVICE.get(templateName);
        } finally {
            VELOCITY_RUNTIME_TEMPLATE_SERVICE.removeTemplate(templateName);
        }
    }

    /**
     * <p>
     * Builds the {@link VelocityContext}
     * </p>
     *
     * @param templateBuildRequest Represents the {@link TemplateBuildRequest}
     * @return The {@link VelocityContext}
     */
    protected <T extends TemplateModel> VelocityContext buildVelocityContext(final TemplateBuildRequest<T> templateBuildRequest) {
        final VelocityContext context = new VelocityContext();

        context.put(TEMPLATE_KEY, templateBuildRequest);
        return context;
    }
}
