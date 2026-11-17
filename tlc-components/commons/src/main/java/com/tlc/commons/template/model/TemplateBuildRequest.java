package com.tlc.commons.template.model;

import com.tlc.commons.internal.template.model.ProcessExecutor;
import com.tlc.commons.template.processor.Processor;

import java.util.Collection;

/**
 * Represents a base template related data models
 *
 * @param <V> the type of {@link TemplateModel} held by this data
 * @author Kishore J
 * @version 1.0
 */
public interface TemplateBuildRequest<V extends TemplateModel> {

    /**
     * <p>
     * Fetches the {@link TemplateModel}
     * </p>
     *
     * @param name Represents the model name
     * @return The {@link TemplateModel}
     */
    V get(final String name);

    /**
     * <p>
     * Fetches the collection of {@link TemplateModel}
     * </p>
     *
     * @param name Represents the model name
     * @return The collection of {@link TemplateModel}
     */
    Collection<V> getAll(final String name);

    /**
     * Returns the process executor responsible for managing template pre-processing
     * and post-processing operations.
     *
     * <p>The executor maintains the registered processors and executes them in the
     * configured processing order.
     *
     * @return Process executor instance used for template processing.
     */
    ProcessExecutor<?, ?> getProcessExecutor();

    /**
     * Adds a processor to the pre-processing pipeline.
     *
     * <p>Pre-processors are executed before template processing begins and can be
     * used to modify or prepare the template before value replacement.
     *
     * @param processor Processor implementation to be executed during pre-processing.
     */
    void addPreProcessor(final Processor<TemplateWrapper> processor);

    /**
     * Adds a processor to the post-processing pipeline.
     *
     * <p>Post-processors are executed after template processing completes and can be
     * used to transform or modify the generated output.
     *
     * @param processor Processor implementation to be executed during post-processing.
     */
    void addPostProcessor(final Processor<String> processor);
}
