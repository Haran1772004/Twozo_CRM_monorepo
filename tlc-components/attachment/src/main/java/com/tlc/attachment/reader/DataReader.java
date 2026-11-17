package com.tlc.attachment.reader;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.reader.internal.CSVReader;
import com.tlc.attachment.reader.internal.ExcelReader;
import com.tlc.commons.code.ErrorCode;

import java.util.Collection;

/**
 * Provides services to read the Excel file rows and stores into {@link RecordHolder}
 *
 * @author pandiaraj
 * @version 1.0
 */
public interface DataReader {

    int MAX_ROW_LENGTH = 50000;

    /**
     * Retrieves the appropriate reader based on the provided {@link Extension}
     *
     * @param extension specifies the File extension to obtain the suitable reader
     * @return {@link DataReader}
     */
    static DataReader get(final Extension extension) {
        return switch (extension) {
            case CSV -> CSVReader.getInstance();
            case XLS, XLSX -> ExcelReader.getInstance();
            default -> throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_FILE_FORMAT);
        };
    }

    /**
     * Reads the rows from the Excel file and loads the records into the {@link RecordHolder}
     *
     * @param file Represents the Excel file contains the data to read
     * @return {@link RecordHolder} that contains the collection parsed Excel file rows
     */
    RecordHolder read(final File file);

    /**
     * Reads the rows from the Excel file and loads the records into the {@link RecordHolder}
     *
     * @param file Represents the Excel file contains the data to read
     */
    void read(final File file, final RecordHolder recordHolder);

    /**
     * Reads the rows from the Excel file and loads the records into the {@link RecordHolder}
     *
     * @param file  Represents the Excel file contains the data to read
     * @param start Indicates the index (1-based) from which the Excel file should be read, excluding the header row
     * @param limit Refers to where the reading of the file should be stopped
     */
    void read(final File file, final int start, final int limit, final RecordHolder recordHolder);

    /**
     * Reads the rows from the Excel file and loads the records into the provided {@link RecordHolder}
     *
     * @param file         Represents the Excel file contains the data to read
     * @param indices      The indices to include in the output RecordHolder object
     * @param recordHolder The {@link RecordHolder} object to populate with the read data
     */
    void read(final File file, final Collection<Integer> indices, final RecordHolder recordHolder);
}
