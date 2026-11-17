package com.tlc.commons.internal.template.builder;

import com.tlc.commons.internal.template.model.ProcessExecutor;
import com.tlc.commons.template.model.TemplateWrapper;
import com.tlc.commons.internal.template.store.TemplateWrapperStore;
import com.tlc.commons.template.builder.TemplateBuilder;
import com.tlc.commons.template.model.TemplateModel;
import com.tlc.commons.template.model.DefaultTemplateBuildRequest;

import java.util.Optional;

/**
 * Implements {@link TemplateBuilder} interface for building templates {@link DefaultTemplateBuildRequest} containing {@link TemplateModel}
 *
 * @author Kishore J
 * @version 1.0
 */
public class DefaultTemplateBuilder implements TemplateBuilder<DefaultTemplateBuildRequest<TemplateModel>> {

    private DefaultTemplateBuilder() {
    }

    private static class InstanceHolder {
        private static final DefaultTemplateBuilder INSTANCE = new DefaultTemplateBuilder();
    }

    /**
     * <p>
     * Gets the {@link DefaultTemplateBuilder} instance
     * </p>
     *
     * @return The {@link DefaultTemplateBuilder}
     */
    public static DefaultTemplateBuilder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param templateName              Represents the template name
     * @param defaultTemplateDataHolder Represents the {@link DefaultTemplateBuildRequest}
     * @return The template data
     */
    @Override
    public Optional<String> build(final String templateName, final DefaultTemplateBuildRequest<TemplateModel> defaultTemplateDataHolder) {
        final Optional<TemplateWrapper> optionalTemplateWrapper = TemplateWrapperStore.getInstance().get(templateName);

        return optionalTemplateWrapper.map(templateWrapper -> {
            final ProcessExecutor<TemplateWrapper, String> processExecutor = defaultTemplateDataHolder.getProcessExecutor();
            final TemplateWrapper preProcessedTemplate = processExecutor.executePreProcess(templateWrapper);
            final String htmlString = preProcessedTemplate.replaceValues(defaultTemplateDataHolder);

            return processExecutor.executePostProcess(htmlString);
        });
    }
}
