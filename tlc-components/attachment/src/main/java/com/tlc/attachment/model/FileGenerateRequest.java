package com.tlc.attachment.model;

import com.tlc.attachment.generator.DataFileGenerator;

/**
 * Immutable container that bundles a {@link FileInfo} (output descriptor) with
 * a {@link DataHolder} (data payload) for a single write operation.
 *
 * <p>A {@link FileGenerateRequest} is the sole argument accepted by {@link DataFileGenerator#generate}.
 * Packaging both the "where / what format" concern ({@link FileInfo}) and the "what data" concern ({@link DataHolder})
 * in one record keeps writer signatures clean and makes it straightforward to extend either side independently.
 *
 * @param <DH>       The concrete {@link DataHolder} type carried by this request.
 * @param fileInfo   Describes the target file path and output format.
 * @param dataHolder The data payload to be written.
 *
 * @author Sathish
 * @version 1.0
 * @see FileInfo
 * @see DataHolder
 * @see DataFileGenerator
 */
public record FileGenerateRequest<DH extends DataHolder>(FileInfo fileInfo, DH dataHolder) {
}
