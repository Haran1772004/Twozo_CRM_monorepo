package com.tlc.attachment.model;

import com.tlc.attachment.generator.DataFileGenerator;

/**
 * Marker interface for all data payload types that can be passed to a
 * {@link DataFileGenerator}.
 *
 * <p>Every concrete data payload — whether it holds tabular records, raw HTML,
 * binary bytes, or any other content — must implement this interface so that the
 * generic writer infrastructure can handle it uniformly through
 * {@link FileGenerateRequest}.
 *
 * @author Sathish
 * @version 1.0
 * @see FileGenerateRequest
 * @see DataFileGenerator
 */
public interface DataHolder {

    /**
     * Checks whether this holder contains any data.
     *
     * @return {@code true} if the holder has no data to write; {@code false} otherwise.
     */
    boolean isEmpty();
}
