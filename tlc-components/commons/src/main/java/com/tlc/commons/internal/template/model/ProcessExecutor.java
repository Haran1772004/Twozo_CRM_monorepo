package com.tlc.commons.internal.template.model;

import com.tlc.commons.template.processor.Processor;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Executes a sequence of pre-processing and post-processing operations
 * using registered {@link Processor} implementations.
 *
 * <p>This executor maintains separate processor chains for template
 * transformation and output transformation. Pre-processors are applied
 * before template execution, while post-processors are applied after the
 * template processing completes.
 *
 * <p>Processors are stored in a sorted order using {@link SortedSet}, allowing
 * execution order to be controlled by the natural ordering of the processor
 * implementations.
 *
 * @param <T> Type of the input template object handled by pre-processors.
 * @param <U> Type of the output object handled by post-processors.
 *
 * @author Sathish
 * @version 1.0
 */
public final class ProcessExecutor<T, U> {

    private final SortedSet<Processor<T>> preProcessors;
    private final SortedSet<Processor<U>> postProcessors;

    /**
     * Creates a new {@code ProcessExecutor} with empty pre-processing and
     * post-processing chains.
     */
    public ProcessExecutor() {
        preProcessors = new TreeSet<>();
        postProcessors = new TreeSet<>();
    }

    /**
     * Adds a processor to the pre-processing chain.
     *
     * <p>Pre-processors are executed in sorted order before the template
     * processing flow continues.
     *
     * @param processor Processor to be added to the pre-processing chain.
     */
    public void addPreProcessor(final Processor<T> processor) {
        preProcessors.add(processor);
    }

    /**
     * Adds a processor to the post-processing chain.
     *
     * <p>Post-processors are executed in sorted order after the template
     * processing flow completes.
     *
     * @param processor Processor to be added to the post-processing chain.
     */
    public void addPostProcessor(final Processor<U> processor) {
        postProcessors.add(processor);
    }

    /**
     * Executes all registered pre-processors against the given template object.
     *
     * <p>The output from each processor is passed as the input to the next
     * processor in the chain.
     *
     * @param templateWrapper Template object to be processed.
     * @return Processed template object after applying all pre-processors.
     */
    public T executePreProcess(final T templateWrapper) {
        T result = templateWrapper;

        for (final Processor<T> processor : preProcessors) {
            result = processor.process(result);
        }

        return result;
    }

    /**
     * Executes all registered post-processors against the given output object.
     *
     * <p>The output from each processor is passed as the input to the next
     * processor in the chain.
     *
     * @param htmlString Output object to be processed.
     * @return Processed output after applying all post-processors.
     */
    public U executePostProcess(final U htmlString) {
        U result = htmlString;

        for (final Processor<U> processor : postProcessors) {
            result = processor.process(result);
        }

        return result;
    }
}