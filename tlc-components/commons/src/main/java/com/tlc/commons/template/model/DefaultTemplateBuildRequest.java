package com.tlc.commons.template.model;

import com.tlc.commons.internal.template.model.ProcessExecutor;
import com.tlc.commons.template.processor.Processor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Represents the default template data details
 * </p>
 *
 * @param <V> The type of the {@link TemplateModel}
 * @author Kishore J
 * @version 1.0
 */
public class DefaultTemplateBuildRequest<V extends TemplateModel> implements TemplateBuildRequest<V> {

    private final ProcessExecutor<TemplateWrapper, String> processExecutor;
    private final Map<String, V> templateModel;

    private Map<String, Collection<V>> templateModels;

    public DefaultTemplateBuildRequest(final Map<String, V> models) {
        this.templateModel = Collections.unmodifiableMap(models);
        this.processExecutor = new ProcessExecutor<>();
    }

    public void setTemplateModels(final Map<String, Collection<V>> templateModels) {
        this.templateModels = Collections.unmodifiableMap(templateModels);
    }

    /**
     * {@inheritDoc}
     *
     * @param name Represents the model name
     * @return The {@link TemplateModel}
     */
    @Override
    public V get(final String name) {
        return templateModel.get(name);
    }

    /**
     * {@inheritDoc}
     *
     * @param name Represents the model name
     * @return The collection of {@link TemplateModel}
     */
    @Override
    public Collection<V> getAll(final String name) {
        return Objects.isNull(templateModels) ? Collections.emptyList() : templateModels.getOrDefault(name, Collections.emptyList());
    }

    /**
     * Returns the process executor responsible for managing template pre-processors
     * and post-processors.
     *
     * @return Process executor instance used for template processing workflow.
     */
    @Override
    public ProcessExecutor<TemplateWrapper, String> getProcessExecutor() {
        return processExecutor;
    }

    /**
     * Adds a processor to the pre-processing chain.
     *
     * <p>Pre-processors are executed before the template processing operation to
     * transform or prepare the template data.
     *
     * @param processor Processor to be added to the pre-processing pipeline.
     */
    @Override
    public void addPreProcessor(final Processor<TemplateWrapper> processor) {
        processExecutor.addPreProcessor(processor);
    }

    /**
     * Adds a processor to the post-processing chain.
     *
     * <p>Post-processors are executed after template processing is completed to
     * transform or modify the generated output.
     *
     * @param processor Processor to be added to the post-processing pipeline.
     */
    @Override
    public void addPostProcessor(final Processor<String> processor) {
        processExecutor.addPostProcessor(processor);
    }
}
