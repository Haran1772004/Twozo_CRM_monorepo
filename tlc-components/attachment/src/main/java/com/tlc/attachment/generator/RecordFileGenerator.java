package com.tlc.attachment.generator;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.attachment.generator.internal.CsvRecordFileGenerator;
import com.tlc.attachment.generator.internal.ExcelRecordFileGenerator;
import com.tlc.attachment.generator.internal.PdfRecordFileGenerator;

import com.tlc.commons.code.ErrorCode;

/**
 * Specialisation of {@link DataFileGenerator} for writing structured tabular records.
 *
 * <p>A {@link RecordFileGenerator} accepts a {@link RecordHolder} — which holds a header row and zero or more data rows — and
 * serialises it into the file format selected by the {@link Extension} passed to {@link #get}.
 *
 * @author ThalaimalaiPandiyanT
 * @author Sathish
 * @version 1.0
 * @see DataFileGenerator
 * @see RecordHolder
 */
public interface RecordFileGenerator extends DataFileGenerator<RecordHolder> {

    /**
     * Returns the singleton {@link RecordFileGenerator} for the given {@link Extension}.
     *
     * @param extension Target output file format.
     * @return The matching {@link RecordFileGenerator} singleton.
     *
     * @throws ErrorCode wrapping {@link StorageErrorCodes#UNSUPPORTED_FILE_EXTENSION} if {@code extension} is not a supported record format.
     */
    static RecordFileGenerator get(final Extension extension) {
        return switch (extension) {
            case XLS, XLSX -> ExcelRecordFileGenerator.getInstance();
            case CSV -> CsvRecordFileGenerator.getInstance();
            case PDF -> PdfRecordFileGenerator.getInstance();
            default -> throw ErrorCode.get(StorageErrorCodes.UNSUPPORTED_FILE_EXTENSION);
        };
    }
}
