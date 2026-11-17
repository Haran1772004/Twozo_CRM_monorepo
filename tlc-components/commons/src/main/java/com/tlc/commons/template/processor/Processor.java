package com.tlc.commons.template.processor;

/**
 * Defines a contract for processing data through a specific transformation step.
 *
 * <p>A {@code Processor} accepts an input object, applies custom processing logic,
 * and returns the transformed result. Processors are ordered based on the value
 * returned by {@link #getOrder()}, allowing multiple processors to be executed
 * in a predictable sequence.
 *
 * @param <T> Type of data processed by this processor.
 *
 * @author Sathish
 * @version 1.0
 */
public interface Processor<T> extends Comparable<Processor<T>> {

    /**
     * Processes the given data and returns the transformed result.
     *
     * @param data Input data to be processed.
     * @return Processed result after applying the processor logic.
     */
    T process(final T data);

    /**
     * Returns the execution order of this processor.
     *
     * <p>Processors with lower order values are executed before processors with
     * higher order values.
     *
     * @return Numeric order value used for processor sorting.
     */
    int getOrder();

    /**
     * Compares this processor with another processor based on execution order.
     *
     * <p>This default implementation sorts processors in ascending order using
     * the value returned by {@link #getOrder()}.
     *
     * @param processor Processor to compare against.
     * @return Negative value, zero, or positive value if this processor's order
     *         is less than, equal to, or greater than the given processor.
     */
    @Override
    default int compareTo(final Processor<T> processor) {
        return Integer.compare(this.getOrder(), processor.getOrder());
    }
}