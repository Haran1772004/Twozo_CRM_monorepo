package com.tlc.attachment.model;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.generator.DataFileGenerator;

/**
 * Immutable value object that describes the output file to be produced by a
 * {@link DataFileGenerator}.
 *
 * <p>{@link FileInfo} decouples the two pieces of output-file metadata — the
 * target path on disk and the required format — from the data payload
 * ({@link DataHolder}).  Both are bundled together in a {@link FileGenerateRequest}
 * before being handed to a writer.
 *
 * <p>Example usage:
 * <pre>{@code
 * FileInfo fileInfo = new FileInfo("/tmp/export_2026.pdf", Extension.PDF);
 * FileGenerateRequest<RecordHolder> request = new FileGenerateRequest<>(fileInfo, recordHolder);
 * RecordFileGenerator.get(Extension.PDF).write(request);
 * }</pre>
 *
 * @param fileName  Absolute path, including the file name and extension, where
 *                  the writer will create the output file.
 * @param extension The target {@link Extension} that determines both the file
 *                  format and the MIME type of the generated output.
 *
 * @author Sathish
 * @version 1.0
 * @see FileGenerateRequest
 * @see Extension
 */
public record FileInfo(String fileName, Extension extension) {
}
