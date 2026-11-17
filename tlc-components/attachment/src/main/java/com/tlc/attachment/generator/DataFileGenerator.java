package com.tlc.attachment.generator;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.DataHolder;
import com.tlc.attachment.model.FileGenerateRequest;

/**
 * Defines the contract for file generation services in the attachment module.
 *
 * <p>Implementations of this interface are responsible for converting a
 * {@link DataHolder} payload provided through {@link FileGenerateRequest} into
 * a physical {@link File}.
 *
 * <p>The generic type {@code DH} ensures that each generator implementation
 * works with a specific {@link DataHolder} type, providing type safety between
 * the request payload and the generator implementation.
 *
 * @param <DH> The {@link DataHolder} type supported by this file generator.
 *
 * @author ThalaimalaiPandiyanT
 * @author Sathish
 * @version 1.0
 *
 * @see FileGenerateRequest
 * @see RecordFileGenerator
 * @see ContentFileGenerator
 */
public interface DataFileGenerator<DH extends DataHolder> {

    /**
     * Generates a file from the data provided in the request.
     *
     * @param fileGenerateRequest Request containing the file metadata and data payload required for generation.
     * @return Generated {@link File}, or {@code null} when no data is available to generate the file.
     */
    File generate(final FileGenerateRequest<DH> fileGenerateRequest);
}